package com.horizon.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class SagaSynchronizationTest {

    private static final Network network = Network.newNetwork();

    @Container
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management")
            .withNetwork(network)
            .withNetworkAliases("rabbitmq");

    @Container
    public static MySQLContainer<?> mysqlUser = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("userservice_db")
            .withUsername("root")
            .withPassword("superSecret")
            .withNetwork(network)
            .withNetworkAliases("mysql-user");

    @Container
    public static MySQLContainer<?> mysqlEvent = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("eventservice_db")
            .withUsername("root")
            .withPassword("superSecret")
            .withNetwork(network)
            .withNetworkAliases("mysql-event");
            
    @Container
    public static MySQLContainer<?> mysqlRsvp = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("rsvpservice_db")
            .withUsername("root")
            .withPassword("superSecret")
            .withNetwork(network)
            .withNetworkAliases("mysql-rsvp");

    @Container
    public static GenericContainer<?> userservice = new GenericContainer<>("horizon/userservice")
            .withExposedPorts(8081)
            .withNetwork(network)
            .dependsOn(mysqlUser, rabbitmq)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-user:3306/userservice_db")
            .withEnv("SPRING_DATASOURCE_USERNAME", "root")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "superSecret")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "health")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));

    @Container
    public static GenericContainer<?> eventservice = new GenericContainer<>("horizon/eventservice")
            .withExposedPorts(8082)
            .withNetwork(network)
            .dependsOn(mysqlEvent, rabbitmq)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-event:3306/eventservice_db")
            .withEnv("SPRING_DATASOURCE_USERNAME", "root")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "superSecret")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "health")
            .withEnv("APP_SECURITY_ENABLED", "false")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));

    @Container
    public static GenericContainer<?> rsvpservice = new GenericContainer<>("horizon/rsvpservice")
            .withExposedPorts(8084)
            .withNetwork(network)
            .dependsOn(mysqlRsvp, rabbitmq)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-rsvp:3306/rsvpservice_db")
            .withEnv("SPRING_DATASOURCE_USERNAME", "root")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "superSecret")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "health")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));


    private static RestTemplate restTemplate;
    private static Connection eventServiceDbConnection;
    private static Connection rsvpServiceDbConnection;
    private static Connection userServiceDbConnection;

    @BeforeAll
    static void setUp() throws SQLException {
        restTemplate = new RestTemplate();
        eventServiceDbConnection = DriverManager.getConnection(mysqlEvent.getJdbcUrl(), mysqlEvent.getUsername(), mysqlEvent.getPassword());
        rsvpServiceDbConnection = DriverManager.getConnection(mysqlRsvp.getJdbcUrl(), mysqlRsvp.getUsername(), mysqlRsvp.getPassword());
        userServiceDbConnection = DriverManager.getConnection(mysqlUser.getJdbcUrl(), mysqlUser.getUsername(), mysqlUser.getPassword());
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (eventServiceDbConnection != null) eventServiceDbConnection.close();
        if (rsvpServiceDbConnection != null) rsvpServiceDbConnection.close();
        if (userServiceDbConnection != null) userServiceDbConnection.close();
    }

    @Test
    void whenForgetMeRequestIsMade_thenUserDataIsDeletedAcrossServices() throws Exception {
        String keycloakId = "user-to-be-forgotten";
        UUID eventId = seedInitialData(keycloakId);
        triggerForgetMeRequest(keycloakId);
        verifyUserIsCompletelyDeleted(keycloakId, eventId);
    }

    private void triggerForgetMeRequest(String keycloakId) {
        String userServiceUrl = String.format("http://%s:%d", userservice.getHost(), userservice.getMappedPort(8081));
        restTemplate.exchange(userServiceUrl + "/users/forget/" + keycloakId, HttpMethod.DELETE, null, Void.class);
    }

    private UUID seedInitialData(String keycloakId) throws Exception {
        // Step 1: Create a user in userservice
        String userServiceUrl = String.format("http://%s:%d", userservice.getHost(), userservice.getMappedPort(8081));

        ObjectMapper objectMapper = new ObjectMapper();

        var userCreateMap = new java.util.HashMap<String, Object>();
        userCreateMap.put("keycloakId", keycloakId);
        userCreateMap.put("username", "testuser");
        userCreateMap.put("email", "test@test.com");
        userCreateMap.put("age", "25");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> userRequest = new HttpEntity<>(objectMapper.writeValueAsString(userCreateMap), headers);

        restTemplate.postForEntity(userServiceUrl + "/users", userRequest, String.class);

        // Step 2: Create associated data directly in the databases
        final UUID eventId = UUID.randomUUID();
        try (var statement = eventServiceDbConnection.createStatement()) {
            String insertEventSql = String.format(
                    "INSERT INTO event (id, title, description, location, start_date, end_date, category, is_private, organizer_id, image_url, created_at, status) " +
                            "VALUES ('%s', 'Test Event', 'Test Desc', 'Test Location', '%s', '%s', 'Test Category', false, '%s', 'http://example.com/img.png', '%s', 'UPCOMING')",
                    eventId, LocalDateTime.now(), LocalDateTime.now().plusHours(2), keycloakId, LocalDateTime.now()
            );
            statement.executeUpdate(insertEventSql);
        }

        try (var statement = rsvpServiceDbConnection.createStatement()) {
            String insertRsvpSql = String.format(
                    "INSERT INTO rsvps (event_id, user_id, status, created_at, user_display_name) VALUES ('%s', '%s', 'ATTENDING', '%s', 'testuser')",
                    eventId, keycloakId, LocalDateTime.now()
            );
            statement.executeUpdate(insertRsvpSql);
        }

        assertTrue(eventExists(eventId), "Pre-condition failed: Event should exist before deletion.");
        assertTrue(rsvpExistsForUser(keycloakId), "Pre-condition failed: RSVP should exist before deletion.");
        return eventId;
    }

    private void verifyUserIsCompletelyDeleted(String keycloakId, UUID eventId) throws SQLException {
        await().atMost(Duration.ofSeconds(60)).until(() -> !eventExists(eventId));
        await().atMost(Duration.ofSeconds(60)).until(() -> !rsvpExistsForUser(keycloakId));
        await().atMost(Duration.ofSeconds(60)).until(() -> userIsDeactivated(keycloakId));

        assertFalse(eventExists(eventId), "Event should be deleted after saga completion.");
        assertFalse(rsvpExistsForUser(keycloakId), "RSVP should be deleted after saga completion.");
        assertTrue(userIsDeactivated(keycloakId), "User status should be DEACTIVATED after saga completion.");
    }

    private boolean eventExists(UUID eventId) throws SQLException {
        try (var statement = eventServiceDbConnection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM event WHERE id = '" + eventId.toString() + "'");
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private boolean rsvpExistsForUser(String userId) throws SQLException {
        try (var statement = rsvpServiceDbConnection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM rsvps WHERE user_id = '" + userId + "'");
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private boolean userIsDeactivated(String keycloakId) throws SQLException {
        try (var statement = userServiceDbConnection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT status FROM users WHERE keycloak_id = '" + keycloakId + "'");
            if (rs.next()) {
                return "DEACTIVATED".equals(rs.getString("status"));
            }
            return false;
        }
    }
} 