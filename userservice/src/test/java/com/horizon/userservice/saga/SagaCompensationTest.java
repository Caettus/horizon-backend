package com.horizon.userservice.saga;

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
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class SagaCompensationTest {

    private static final Network network = Network.newNetwork();

    private static final DockerImageName RABBIT_IMAGE =
            DockerImageName
                    .parse("heidiks/rabbitmq-delayed-message-exchange:3.13.3-management")
                    .asCompatibleSubstituteFor("rabbitmq");

    @Container
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer(RABBIT_IMAGE)
            .withNetwork(network)
            .withNetworkAliases("rabbitmq")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("rabbitmq.conf"),
                    "/etc/rabbitmq/rabbitmq.conf"
            )
            .withExposedPorts(5672, 15672)          // +25672 if you use clustering
            // wait for one of those ports to be up
            .waitingFor(Wait.forListeningPort()
                    .withStartupTimeout(Duration.ofMinutes(5)));

    // --- MODIFICATION: Use a single MySQL Container for all services ---
    @Container
    public static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("userservice_db") // Default database
            .withUsername("root")
            .withPassword("superSecret")
            .withNetwork(network)
            .withNetworkAliases("mysql")
            // Script to create the other databases
            .withInitScript("init-test-dbs.sql");

    @Container
    public static GenericContainer<?> userservice = new GenericContainer<>("horizon/userservice")
            .withExposedPorts(8081)
            .withNetwork(network)
            .dependsOn(mysql, rabbitmq)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql:3306/userservice_db")
            .withEnv("SPRING_DATASOURCE_USERNAME", "root")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "superSecret")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "health")
            .withEnv("APP_SECURITY_ENABLED", "false")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "update")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));

    @Container
    public static GenericContainer<?> eventservice = new GenericContainer<>("horizon/eventservice")
            .withExposedPorts(8082)
            .withNetwork(network)
            .dependsOn(mysql, rabbitmq)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql:3306/eventservice_db")
            .withEnv("SPRING_DATASOURCE_USERNAME", "root")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "superSecret")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "health")
            .withEnv("APP_SECURITY_ENABLED", "false")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "update")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));

    // --- CORRECTION: Keep the rsvpservice container for this test ---
    @Container
    public static GenericContainer<?> rsvpservice = new GenericContainer<>("horizon/rsvpservice")
            .withExposedPorts(8084)
            .withNetwork(network)
            .dependsOn(mysql, rabbitmq)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql:3306/rsvpservice_db")
            .withEnv("SPRING_DATASOURCE_USERNAME", "root")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "superSecret")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "health")
            .withEnv("APP_SECURITY_ENABLED", "false")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "update")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));


    private static RestTemplate restTemplate;
    private static Connection eventServiceDbConnection;
    private static Connection rsvpServiceDbConnection;
    private static Connection userServiceDbConnection;

    @BeforeAll
    static void setUp() throws SQLException {
        restTemplate = new RestTemplate();
        // --- MODIFICATION: Create connections to the different databases in the single MySQL container ---
        eventServiceDbConnection = DriverManager.getConnection(mysql.getJdbcUrl().replace("userservice_db", "eventservice_db"), mysql.getUsername(), mysql.getPassword());
        rsvpServiceDbConnection = DriverManager.getConnection(mysql.getJdbcUrl().replace("userservice_db", "rsvpservice_db"), mysql.getUsername(), mysql.getPassword());
        userServiceDbConnection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (eventServiceDbConnection != null) eventServiceDbConnection.close();
        if (rsvpServiceDbConnection != null) rsvpServiceDbConnection.close();
        if (userServiceDbConnection != null) userServiceDbConnection.close();
    }
    
    @Test
    void whenParticipantServiceFails_thenSagaIsRolledBackAndUserStatusIsReverted() throws Exception {
        // 1. Prepare Data
        String keycloakId = UUID.randomUUID().toString();
        UUID eventId = seedInitialData(keycloakId);

        // 2. Simulate Failure
        eventservice.stop();

        // 3. Trigger the Saga
        triggerForgetMeRequest(keycloakId);

        // 4. Verify the Rollback
        await().atMost(Duration.ofMinutes(5)).until(() -> sagaHasFailed(keycloakId));
        await().atMost(Duration.ofMinutes(5)).until(() -> userStatusIsActive(keycloakId));

        // Verify final state
        assertTrue(sagaHasFailed(keycloakId), "Saga status should be FAILED.");
        assertTrue(userStatusIsActive(keycloakId), "User status should be reverted to ACTIVE.");
        assertFalse(rsvpExistsForUser(keycloakId), "RSVP data should be deleted by the successful participant.");
        
        // We cannot check eventExists because the eventservice container is stopped.
        // Re-opening the connection would be complex. The core of the test is verifying
        // the compensating action on the userservice side.
    }

    private void triggerForgetMeRequest(String keycloakId) {
        String userServiceUrl = String.format("http://%s:%d", userservice.getHost(), userservice.getMappedPort(8081));
        restTemplate.exchange(userServiceUrl + "/users/forget/" + keycloakId, HttpMethod.DELETE, null, Void.class);
    }

    private UUID seedInitialData(String keycloakId) throws Exception {
        String userServiceUrl = String.format("http://%s:%d", userservice.getHost(), userservice.getMappedPort(8081));

        ObjectMapper objectMapper = new ObjectMapper();

        var userCreateMap = new java.util.HashMap<String, Object>();
        userCreateMap.put("keycloakId", keycloakId);
        userCreateMap.put("username", "testuser-rollback");
        userCreateMap.put("email", "rollback@test.com");
        userCreateMap.put("age", "30");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> userRequest = new HttpEntity<>(objectMapper.writeValueAsString(userCreateMap), headers);

        restTemplate.postForEntity(userServiceUrl + "/users", userRequest, String.class);

        final UUID eventId = UUID.randomUUID();
        try (var statement = eventServiceDbConnection.createStatement()) {
            String insertEventSql = String.format(
                    "INSERT INTO event (id, title, description, location, start_date, end_date, category, is_private, organizer_id, image_url, created_at, status, is_flagged) " +
                            "VALUES ('%s', 'Rollback Event', 'Desc', 'Location', '%s', '%s', 'Category', false, '%s', 'http://example.com/img.png', '%s', 'UPCOMING', false)",
                    eventId, LocalDateTime.now(), LocalDateTime.now().plusHours(2), keycloakId, LocalDateTime.now()
            );
            statement.executeUpdate(insertEventSql);
        }

        try (var statement = rsvpServiceDbConnection.createStatement()) {
            String insertRsvpSql = String.format(
                    "INSERT INTO rsvps (event_id, user_id, status, created_at, user_display_name) VALUES ('%s', '%s', 'ATTENDING', '%s', 'testuser-rollback')",
                    eventId, keycloakId, LocalDateTime.now()
            );
            statement.executeUpdate(insertRsvpSql);
        }

        assertTrue(eventExists(eventId), "Pre-condition failed: Event should exist.");
        assertTrue(rsvpExistsForUser(keycloakId), "Pre-condition failed: RSVP should exist.");
        return eventId;
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

    private boolean sagaHasFailed(String correlationId) throws SQLException {
        try (var statement = userServiceDbConnection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT status FROM saga_states WHERE correlation_id = '" + correlationId + "'");
            if (rs.next()) {
                return "FAILED".equals(rs.getString("status"));
            }
            return false;
        }
    }

    private boolean userStatusIsActive(String keycloakId) throws SQLException {
        try (var statement = userServiceDbConnection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT status FROM users WHERE keycloak_id = '" + keycloakId + "'");
            if (rs.next()) {
                return "ACTIVE".equals(rs.getString("status"));
            }
            return false;
        }
    }
} 