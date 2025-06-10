package com.horizon.userservice.integration;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.DAL.UserDeletionSagaStateRepository;
import com.horizon.userservice.event.UserDeletionRequestedEvent;
import com.horizon.userservice.model.User;
import com.horizon.userservice.model.UserDeletionSagaState;
import com.horizon.userservice.saga.SagaStatus;
import com.horizon.userservice.saga.UserDeletionSaga;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource; // Import this
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// Add this annotation to enable RabbitMQ listeners for this test
@TestPropertySource(properties = {"spring.rabbitmq.listener.simple.auto-startup=true"})
public class UserDeletionSagaIntegrationTest {

    // Note: I've updated the image name. Using "mysql:8" lets Docker choose the best
    // architecture for your machine (e.g., ARM64 on Apple Silicon) to avoid performance warnings.
    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

    @Autowired
    private UserDeletionSaga userDeletionSaga;

    @Autowired
    private UserDAL userDAL;

    @Autowired
    private UserDeletionSagaStateRepository sagaStateRepository;

    @MockitoSpyBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private Keycloak keycloak;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mySQLContainer::getDriverClassName);

        // This is the new line. It tells Hibernate to generate MySQL-compatible SQL.
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");

        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);

        registry.add("saga.deletion.timeout-check-rate-ms", () -> "999999999");
    }

    @BeforeAll
    static void beforeAll() {
        mySQLContainer.start();
        rabbitmq.start();
    }

    @AfterAll
    static void afterAll() {
        mySQLContainer.stop();
        rabbitmq.stop();
    }

    @BeforeEach
    void setUp() {
        // This setup will now work because the tables will be created correctly.
        sagaStateRepository.deleteAll();
        userDAL.deleteAll();

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        // Mock the delete call to do nothing to avoid NullPointerException if the call returns void
        doNothing().when(usersResource).delete(anyString());
    }

    @Test
    void testUserDeletionSaga_SuccessfulFlow() {
        String keycloakId = "user-to-be-deleted-saga-test";

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setUsername("testSagaUser");
        user.setEmail("saga@example.com");
        user.setCreatedAt(LocalDateTime.now());
        userDAL.save(user);

        UserDeletionRequestedEvent startEvent = new UserDeletionRequestedEvent(keycloakId);
        userDeletionSaga.startSaga(startEvent);

        verify(rabbitTemplate).convertAndSend(
                eq("user.deletion.exchange"),
                eq("user.deletion.requested"),
                eq(startEvent)
        );

        Optional<UserDeletionSagaState> initialSagaState = sagaStateRepository.findBySagaId(keycloakId);
        assertTrue(initialSagaState.isPresent(), "Saga state should be created");
        assertEquals(SagaStatus.AWAITING_CONFIRMATION, initialSagaState.get().getStatus());

        System.out.println("Test: Simulating confirmation from eventservice...");
        // Send confirmations which will now be picked up by the active listeners
        rabbitTemplate.convertAndSend("user.deletion.exchange", "user.deletion.events.confirmed", keycloakId);

        System.out.println("Test: Simulating confirmation from rsvpservice...");
        rabbitTemplate.convertAndSend("user.deletion.exchange", "user.deletion.rsvps.confirmed", keycloakId);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Now this verification should pass
            verify(keycloak.realm("horizon-realm").users()).delete(keycloakId);
            assertFalse(userDAL.findByKeycloakId(keycloakId).isPresent(), "User should be deleted from the database.");
            Optional<UserDeletionSagaState> finalSagaState = sagaStateRepository.findBySagaId(keycloakId);
            assertTrue(finalSagaState.isPresent(), "Final saga state should still exist.");
            assertEquals(SagaStatus.COMPLETED, finalSagaState.get().getStatus(), "Saga status should be COMPLETED.");
            verify(rabbitTemplate).convertAndSend(
                    eq("user.deletion.exchange"),
                    eq("user.deletion.confirmed"),
                    any(com.horizon.userservice.event.UserDeletionConfirmedEvent.class)
            );
        });

        System.out.println("Test: Saga completed and all assertions passed.");
    }
}