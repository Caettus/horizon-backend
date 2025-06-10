package com.horizon.userservice.integration;

import com.horizon.userservice.model.User;
import com.horizon.userservice.model.UserDeletionSagaState;
import com.horizon.userservice.model.UserDeletionSagaStatus;
import com.horizon.userservice.repository.UserDeletionSagaRepository;
import com.horizon.userservice.repository.UserRepository;
import com.horizon.userservice.resource.UsersResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@Transactional
public class UserDeletionSagaIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeletionSagaRepository sagaRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @MockBean
    private UsersResource usersResource;

    @Autowired
    private com.horizon.userservice.saga.UserDeletionSaga userDeletionSaga;

    private User testUser;
    private String sagaId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        sagaRepository.deleteAll();

        // Purge all queues
        rabbitAdmin.purgeQueue(TestRabbitMQConfig.REQUESTED_QUEUE, false);
        rabbitAdmin.purgeQueue(TestRabbitMQConfig.EVENTS_CONFIRMED_QUEUE, false);
        rabbitAdmin.purgeQueue(TestRabbitMQConfig.RSVPS_CONFIRMED_QUEUE, false);
        rabbitAdmin.purgeQueue(TestRabbitMQConfig.CONFIRMED_QUEUE, false);

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setKeycloakId("test-keycloak-id");
        testUser = userRepository.save(testUser);

        sagaId = UUID.randomUUID().toString();
    }

    @Test
    void testUserDeletionSaga() throws InterruptedException {
        // Start the saga
        userDeletionSaga.startSaga(testUser.getKeycloakId());

        // Simulate events service confirmation
        rabbitTemplate.convertAndSend(
                TestRabbitMQConfig.EXCHANGE_NAME,
                "user.deletion.events.confirmed",
                testUser.getKeycloakId()
        );

        // Simulate rsvps service confirmation
        rabbitTemplate.convertAndSend(
                TestRabbitMQConfig.EXCHANGE_NAME,
                "user.deletion.rsvps.confirmed",
                testUser.getKeycloakId()
        );

        // Wait for saga to process
        Thread.sleep(1000);

        // Verify saga state
        UserDeletionSagaState saga = sagaRepository.findBySagaId(sagaId).orElse(null);
        assertNotNull(saga);
        assertEquals(UserDeletionSagaStatus.COMPLETED, saga.getStatus());

        // Verify user deletion
        verify(usersResource, times(1)).delete(testUser.getKeycloakId());
    }
}