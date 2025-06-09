package com.horizon.userservice.integration;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import com.horizon.userservice.Interface.UserService;
import com.horizon.userservice.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.EmptyResultDataAccessException;
import com.horizon.userservice.DTO.UserUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.awaitility.Awaitility;
import java.util.concurrent.TimeUnit;
import com.horizon.userservice.event.UserRegisteredEvent;
import com.horizon.userservice.event.UserProfileUpdatedEvent;
import java.util.Map;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.keycloak.admin.client.Keycloak;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.EmptyResultDataAccessException;
import com.horizon.userservice.DTO.UserUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.awaitility.Awaitility;
import java.util.concurrent.TimeUnit;
import com.horizon.userservice.event.UserRegisteredEvent;
import com.horizon.userservice.event.UserProfileUpdatedEvent;
import java.util.Map;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE) // No need for a web server for service tests
@ActiveProfiles("test")
@Transactional // Rollback transactions after each test
class UserServiceImplIntegrationTest {

    @MockBean
    private Keycloak keycloak;

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.28");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

    @Autowired
    private UserService userService;

    @Autowired
    private UserDAL userDAL;

    @SpyBean // Use SpyBean if you need to verify interactions but still want real method execution
    private RabbitTemplate rabbitTemplate;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.url", mySQLContainer::getJdbcUrl); // If using Flyway
        registry.add("spring.flyway.user", mySQLContainer::getUsername);
        registry.add("spring.flyway.password", mySQLContainer::getPassword);

        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
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

    private RabbitAdmin rabbitAdmin;

    @BeforeEach
    void setUp() {
        // Clean up database before each test to ensure a clean state
        userDAL.deleteAll();

        // Setup RabbitAdmin for queue management
        rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        // Declare queues to ensure they exist for message count checks (optional, if not auto-declared)
        // It's good practice to use the same queue names as in your main application configuration if possible
        // For this example, assuming direct exchange and routing key as queue name for simplicity of checking.
        // In a real scenario, you might listen to specific queues bound to the exchange.
        // We will check the exchange and routing key directly with SpyBean on rabbitTemplate for sent messages,
        // but for received messages or dead-letter queues, direct queue interaction is useful.
        // For now, we focus on verifying that convertAndSend was called with the correct parameters.
    }

    @Test
    void testCreateAndGetUser() {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("testuser");
        createDTO.setEmail("testuser@example.com");
        createDTO.setAge("25");
        createDTO.setKeycloakId("keycloak-id-123");

        UserResponseDTO createdUser = userService.createUser(createDTO);
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("testuser", createdUser.getUsername());
        assertEquals("testuser@example.com", createdUser.getEmail());
        assertEquals("25", createdUser.getAge());
        assertEquals("keycloak-id-123", createdUser.getKeycloakId());

        UserResponseDTO fetchedUser = userService.getUserById(createdUser.getId());
        assertNotNull(fetchedUser);
        assertEquals(createdUser.getId(), fetchedUser.getId());
        assertEquals("testuser", fetchedUser.getUsername());
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        UserCreateDTO createDTO1 = new UserCreateDTO();
        createDTO1.setUsername("user1");
        createDTO1.setEmail("user1@example.com");
        createDTO1.setAge("30");
        createDTO1.setKeycloakId("keycloak1");
        userService.createUser(createDTO1);

        UserCreateDTO createDTO2 = new UserCreateDTO();
        createDTO2.setUsername("user2");
        createDTO2.setEmail("user2@example.com");
        createDTO2.setAge("35");
        createDTO2.setKeycloakId("keycloak2");
        userService.createUser(createDTO2);

        // Act
        List<UserResponseDTO> users = userService.getAllUsers();

        // Assert
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user1")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user2")));
    }

    @Test
    void testGetAllUsers_whenNoUsers_shouldReturnEmptyList() {
        // Act
        List<UserResponseDTO> users = userService.getAllUsers();

        // Assert
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void testUpdateUser() {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("updateTestUser");
        createDTO.setEmail("update@example.com");
        createDTO.setAge("40");
        createDTO.setKeycloakId("keycloakUpdate");
        UserResponseDTO createdUser = userService.createUser(createDTO);

        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("updatedEmail@example.com");
        updateDTO.setAge("42");

        // Act
        UserResponseDTO updatedUser = userService.updateUser(createdUser.getId(), updateDTO);

        // Assert
        assertNotNull(updatedUser);
        assertEquals(createdUser.getId(), updatedUser.getId());
        assertEquals("updatedEmail@example.com", updatedUser.getEmail());
        assertEquals("42", updatedUser.getAge());
        assertEquals("updateTestUser", updatedUser.getUsername()); // Username should not change

        // Verify in DB
        UserResponseDTO userFromDb = userService.getUserById(createdUser.getId());
        assertEquals("updatedEmail@example.com", userFromDb.getEmail());
        assertEquals("42", userFromDb.getAge());
    }

    @Test
    void testUpdateUser_whenUserNotFound_shouldThrowException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setEmail("any@example.com");
        assertThrows(ResponseStatusException.class, () -> {
            userService.updateUser(9999, updateDTO); // Non-existent ID
        });
    }

    @Test
    void testDeleteUser() {
        // Arrange
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("deleteTestUser");
        createDTO.setEmail("delete@example.com");
        createDTO.setAge("50");
        createDTO.setKeycloakId("keycloakDelete");
        UserResponseDTO createdUser = userService.createUser(createDTO);
        Integer userId = createdUser.getId();

        // Act
        userService.deleteUser(userId);

        // Assert
        // Try to fetch the deleted user, should throw an exception
        assertThrows(ResponseStatusException.class, () -> {
            userService.getUserById(userId);
        });

        // Also verify directly with DAL if user does not exist
        assertFalse(userDAL.existsById(userId));
    }

    @Test
    void testDeleteUser_whenUserNotFound_shouldThrowException() {
        assertThrows(ResponseStatusException.class, () -> {
            userService.deleteUser(9999); // Non-existent ID
        });
    }

    @Test
    void testSynchronizeUser_whenNewUser_createsUserAndPublishesEvent() {
        // Arrange
        String keycloakId = "sync-keycloak-new";
        String username = "syncNewUser";
        String email = "syncnew@example.com";

        // Act
        User synchronizedUser = userService.synchronizeUser(keycloakId, username, email);

        // Assert: User in DB
        assertNotNull(synchronizedUser);
        assertNotNull(synchronizedUser.getId());
        assertEquals(keycloakId, synchronizedUser.getKeycloakId());
        assertEquals(username, synchronizedUser.getUsername());
        assertEquals(email, synchronizedUser.getEmail());

        User userFromDb = userDAL.findByKeycloakId(keycloakId).orElse(null);
        assertNotNull(userFromDb);
        assertEquals(username, userFromDb.getUsername());

        // Assert: Event published
        // Verify that convertAndSend was called on the spy with the correct arguments
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(rabbitTemplate).convertAndSend(
                eq("horizon.users.exchange"),
                eq("user.registered"),
                isA(UserRegisteredEvent.class)
            );
        });
    }

    @Test
    void testSynchronizeUser_whenExistingUserWithChanges_updatesUserAndPublishesEvent() {
        // Arrange: Create initial user
        String keycloakId = "sync-keycloak-existing";
        String initialUsername = "initialSyncUser";
        String initialEmail = "initialsync@example.com";
        User initialUser = new User();
        initialUser.setKeycloakId(keycloakId);
        initialUser.setUsername(initialUsername);
        initialUser.setEmail(initialEmail);
        initialUser.setCreatedAt(LocalDateTime.now());
        userDAL.save(initialUser);

        String updatedUsername = "updatedSyncUser";
        String updatedEmail = "updatedsync@example.com";

        // Act
        User synchronizedUser = userService.synchronizeUser(keycloakId, updatedUsername, updatedEmail);

        // Assert: User in DB
        assertNotNull(synchronizedUser);
        assertEquals(initialUser.getId(), synchronizedUser.getId());
        assertEquals(updatedUsername, synchronizedUser.getUsername());
        assertEquals(updatedEmail, synchronizedUser.getEmail());

        User userFromDb = userDAL.findByKeycloakId(keycloakId).orElse(null);
        assertNotNull(userFromDb);
        assertEquals(updatedUsername, userFromDb.getUsername());
        assertEquals(updatedEmail, userFromDb.getEmail());

        // Assert: Event published
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(rabbitTemplate).convertAndSend(
                eq("horizon.users.exchange"),
                eq("user.profile.updated"),
                isA(UserProfileUpdatedEvent.class)
            );
        });
    }

    @Test
    void testSynchronizeUser_whenExistingUserNoChanges_returnsUserAndNoEvent() {
        // Arrange: Create initial user
        String keycloakId = "sync-keycloak-nochange";
        String username = "noChangeSyncUser";
        String email = "nochangesync@example.com";
        User initialUser = new User();
        initialUser.setKeycloakId(keycloakId);
        initialUser.setUsername(username);
        initialUser.setEmail(email);
        initialUser.setCreatedAt(LocalDateTime.now());
        userDAL.save(initialUser);

        // Act
        User synchronizedUser = userService.synchronizeUser(keycloakId, username, email);

        // Assert: User in DB (unchanged)
        assertNotNull(synchronizedUser);
        assertEquals(username, synchronizedUser.getUsername());
        assertEquals(email, synchronizedUser.getEmail());

        // Assert: No event published (give a small delay to ensure no message is sent)
        try {
            TimeUnit.SECONDS.sleep(1); // Small delay to catch any unintended async sends
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        verify(rabbitTemplate, never()).convertAndSend(
            eq("horizon.users.exchange"), 
            eq("user.profile.updated"), 
            any(UserProfileUpdatedEvent.class)
        );
        verify(rabbitTemplate, never()).convertAndSend(
            eq("horizon.users.exchange"), 
            eq("user.registered"), 
            any(UserRegisteredEvent.class)
        );
    }

    @Test
    void testUserLifecycle_ByKeycloakId_Flow() {
        // === 1. Synchronize a new user (simulates Keycloak sending user info) ===
        String keycloakId = "flow-keycloak-id";
        String initialUsername = "flowUser";
        String initialEmail = "flow@example.com";
        String initialAge = "28"; // Age is not part of synchronizeUser, but we need it for UserUpdateDTO later

        User synchronizedUser = userService.synchronizeUser(keycloakId, initialUsername, initialEmail);
        assertNotNull(synchronizedUser, "User should be created by synchronization");
        assertNotNull(synchronizedUser.getId(), "Synchronized user should have an ID");
        assertEquals(keycloakId, synchronizedUser.getKeycloakId());
        assertEquals(initialUsername, synchronizedUser.getUsername());
        assertEquals(initialEmail, synchronizedUser.getEmail());

        // Verify event for registration
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(rabbitTemplate).convertAndSend(
                eq("horizon.users.exchange"),
                eq("user.registered"),
                isA(UserRegisteredEvent.class)
            );
        });

        // === 2. Get the user by Keycloak ID ===
        User fetchedUserByKeycloakId = userService.getUserByKeycloakId(keycloakId);
        assertNotNull(fetchedUserByKeycloakId, "User should be retrievable by Keycloak ID");
        assertEquals(synchronizedUser.getId(), fetchedUserByKeycloakId.getId());
        assertEquals(initialUsername, fetchedUserByKeycloakId.getUsername());

        // === 3. Update user details using Keycloak ID ===
        UserUpdateDTO updateDetails = new UserUpdateDTO();
        String updatedEmail = "flow.updated@example.com";
        String updatedAge = "30";
        updateDetails.setEmail(updatedEmail);
        updateDetails.setAge(updatedAge);

        User updatedUser = userService.updateUserByKeycloakId(keycloakId, updateDetails);
        assertNotNull(updatedUser, "User should be updated by Keycloak ID");
        assertEquals(updatedEmail, updatedUser.getEmail());
        assertEquals(updatedAge, updatedUser.getAge());
        assertEquals(initialUsername, updatedUser.getUsername()); // Username is not updated by this method

        // Verify event for profile update
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(rabbitTemplate).convertAndSend(
                eq("horizon.users.exchange"),
                eq("user.profile.updated"),
                isA(UserProfileUpdatedEvent.class)
            );
        });

        // Verify in DB
        UserResponseDTO userFromDbAfterUpdate = userService.getUserById(synchronizedUser.getId());
        assertEquals(updatedEmail, userFromDbAfterUpdate.getEmail());
        assertEquals(updatedAge, userFromDbAfterUpdate.getAge());

        // === 4. Synchronize the same user again, but with different email (username shouldn't change via sync) ===
        String emailAfterSecondSync = "flow.second.sync@example.com";
        // Reset the mock interaction on rabbitTemplate for the next verification, if this is the last interaction we care about for this spy
        // Or use a more specific verify(..., times(2)) if previous calls are also important.
        // For this flow, let's assume we only care about the *next* event for profile update.
        org.mockito.Mockito.reset(rabbitTemplate); // Reset interactions for the spy for cleaner verification of the next event

        User userAfterSecondSync = userService.synchronizeUser(keycloakId, initialUsername, emailAfterSecondSync);
        assertNotNull(userAfterSecondSync);
        assertEquals(emailAfterSecondSync, userAfterSecondSync.getEmail());
        assertEquals(initialUsername, userAfterSecondSync.getUsername()); // Username should remain initialUsername
        assertEquals(updatedAge, userAfterSecondSync.getAge()); // Age from previous updateUserByKeycloakId should persist

        // Verify event for the second profile update during sync
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(rabbitTemplate).convertAndSend(
                eq("horizon.users.exchange"),
                eq("user.profile.updated"),
                isA(UserProfileUpdatedEvent.class) // This should be the second UserProfileUpdatedEvent
            );
        });

        // === 5. Get multiple users by Keycloak IDs (including a non-existent one) ===
        String anotherKeycloakId = "flow-another-keycloak";
        userService.synchronizeUser(anotherKeycloakId, "anotherFlowUser", "anotherflow@example.com");
        // Wait for the second registration event
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
             verify(rabbitTemplate).convertAndSend(
                eq("horizon.users.exchange"),
                eq("user.registered"),
                org.mockito.ArgumentMatchers.argThat( (UserRegisteredEvent event) -> event.getKeycloakId().equals(anotherKeycloakId))
            );
        });

        List<String> idsToFetch = List.of(keycloakId, anotherKeycloakId, "non-existent-keycloak-id");
        List<User> fetchedUsers = userService.getUsersByKeycloakIds(idsToFetch);
        assertNotNull(fetchedUsers);
        assertEquals(2, fetchedUsers.size(), "Should fetch two existing users");
        assertTrue(fetchedUsers.stream().anyMatch(u -> u.getKeycloakId().equals(keycloakId)));
        assertTrue(fetchedUsers.stream().anyMatch(u -> u.getKeycloakId().equals(anotherKeycloakId)));

        // === 6. Try to get a user by a non-existent Keycloak ID ===
        assertThrows(ResponseStatusException.class, () -> {
            userService.getUserByKeycloakId("truly-non-existent-id");
        });

        // === 7. Try to update a user by a non-existent Keycloak ID ===
        assertThrows(ResponseStatusException.class, () -> {
            userService.updateUserByKeycloakId("truly-non-existent-id-for-update", updateDetails);
        });
    }

    // TODO: Add more comprehensive integration tests for other UserServiceImpl methods
    // - getUserByKeycloakId
    // - getUsersByKeycloakIds
    // - updateUserByKeycloakId (verify event publishing)
    // Consider testing event publishing with RabbitTemplate verifications (e.g. using Awaitility and RabbitAdmin or a test listener).
} 