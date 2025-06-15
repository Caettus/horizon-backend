package com.horizon.userservice.service;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import com.horizon.userservice.DTO.UserUpdateDTO;
import com.horizon.userservice.Interface.UserServiceImpl;
import com.horizon.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.horizon.common.events.UserRegisteredEvent;
import com.horizon.common.events.UserProfileUpdatedEvent;
import com.horizon.userservice.dto.UserResponseDTO;
import com.horizon.userservice.dto.UserUpdateDTO;
import com.horizon.userservice.eventbus.EventCreatedListener;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDAL userDAL;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserCreateDTO userCreateDTO;
    private UserUpdateDTO userUpdateDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setAge("30");
        user.setKeycloakId("keycloak-test-id");
        user.setCreatedAt(LocalDateTime.now());

        userCreateDTO = new UserCreateDTO();
        userCreateDTO.setUsername("newuser");
        userCreateDTO.setEmail("new@example.com");
        userCreateDTO.setAge("25");
        userCreateDTO.setKeycloakId("keycloak-new-id");

        userUpdateDTO = new UserUpdateDTO();
        userUpdateDTO.setEmail("updated@example.com");
        userUpdateDTO.setAge("35");
    }

    @Test
    void getUserById_whenUserExists_shouldReturnUserResponseDTO() {
        when(userDAL.findById(1)).thenReturn(Optional.of(user));

        UserResponseDTO result = userService.getUserById(1);

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getUsername(), result.getUsername());
        verify(userDAL).findById(1);
    }

    @Test
    void getUserById_whenUserNotExists_shouldThrowResponseStatusException() {
        when(userDAL.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            userService.getUserById(1);
        });
        verify(userDAL).findById(1);
    }

    @Test
    void createUser_shouldSaveAndReturnUser() {
        // Arrange
        User userToSave = new User();
        userToSave.setUsername(userCreateDTO.getUsername());
        userToSave.setEmail(userCreateDTO.getEmail());
        userToSave.setAge(userCreateDTO.getAge());
        userToSave.setKeycloakId(userCreateDTO.getKeycloakId());
        // createdAt is set within the method, so we capture the argument to check it

        User savedUser = new User(); // This would be the object returned by userDAL.save()
        savedUser.setId(2); // Simulate DB generating an ID
        savedUser.setUsername(userCreateDTO.getUsername());
        savedUser.setEmail(userCreateDTO.getEmail());
        savedUser.setAge(userCreateDTO.getAge());
        savedUser.setKeycloakId(userCreateDTO.getKeycloakId());
        savedUser.setCreatedAt(LocalDateTime.now()); // Approximate time

        when(userDAL.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponseDTO result = userService.createUser(userCreateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.getId());
        assertEquals(userCreateDTO.getUsername(), result.getUsername());
        assertEquals(userCreateDTO.getEmail(), result.getEmail());
        assertEquals(userCreateDTO.getAge(), result.getAge());
        assertEquals(userCreateDTO.getKeycloakId(), result.getKeycloakId());
        assertNotNull(result.getCreatedAt());

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDAL).save(userArgumentCaptor.capture());
        User capturedUser = userArgumentCaptor.getValue();
        assertEquals(userCreateDTO.getUsername(), capturedUser.getUsername());
        assertNotNull(capturedUser.getCreatedAt()); // Check that createdAt was set
    }

    @Test
    void getAllUsers_shouldReturnListOfUserResponseDTO() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(2);
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setAge("40");
        anotherUser.setKeycloakId("keycloak-another-id");
        anotherUser.setCreatedAt(LocalDateTime.now());

        List<User> users = List.of(user, anotherUser);
        when(userDAL.findAll()).thenReturn(users);

        // Act
        List<UserResponseDTO> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(user.getUsername(), result.get(0).getUsername());
        assertEquals(anotherUser.getUsername(), result.get(1).getUsername());
        verify(userDAL).findAll();
    }

    @Test
    void getAllUsers_whenNoUsers_shouldReturnEmptyList() {
        // Arrange
        when(userDAL.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<UserResponseDTO> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userDAL).findAll();
    }

    @Test
    void updateUser_whenUserExists_shouldUpdateAndReturnUser() {
        // Arrange
        when(userDAL.findById(1)).thenReturn(Optional.of(user));
        when(userDAL.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the same user that was passed to save

        // Act
        UserResponseDTO result = userService.updateUser(1, userUpdateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(userUpdateDTO.getEmail(), result.getEmail());
        assertEquals(userUpdateDTO.getAge(), result.getAge());
        // Username and keycloakId should not be updated by this method
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getKeycloakId(), result.getKeycloakId());

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDAL).save(userArgumentCaptor.capture());
        User capturedUser = userArgumentCaptor.getValue();
        assertEquals(userUpdateDTO.getEmail(), capturedUser.getEmail());
        assertEquals(userUpdateDTO.getAge(), capturedUser.getAge());
    }

    @Test
    void updateUser_whenOnlyEmailIsProvided_shouldUpdateEmailOnly() {
        // Arrange
        UserUpdateDTO partialUpdateDTO = new UserUpdateDTO();
        partialUpdateDTO.setEmail("onlyemail@example.com");

        when(userDAL.findById(1)).thenReturn(Optional.of(user));
        when(userDAL.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponseDTO result = userService.updateUser(1, partialUpdateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(partialUpdateDTO.getEmail(), result.getEmail());
        assertEquals(user.getAge(), result.getAge()); // Age should remain unchanged
        verify(userDAL).save(any(User.class));
    }

    @Test
    void updateUser_whenOnlyAgeIsProvided_shouldUpdateAgeOnly() {
        // Arrange
        UserUpdateDTO partialUpdateDTO = new UserUpdateDTO();
        partialUpdateDTO.setAge("50");

        when(userDAL.findById(1)).thenReturn(Optional.of(user));
        when(userDAL.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponseDTO result = userService.updateUser(1, partialUpdateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail()); // Email should remain unchanged
        assertEquals(partialUpdateDTO.getAge(), result.getAge());
        verify(userDAL).save(any(User.class));
    }

    @Test
    void updateUser_whenUserNotExists_shouldThrowResponseStatusException() {
        // Arrange
        when(userDAL.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            userService.updateUser(1, userUpdateDTO);
        });
        verify(userDAL).findById(1);
        verify(userDAL, never()).save(any(User.class));
    }

    @Test
    void deleteUser_whenUserExists_shouldCallDeleteById() {
        // Arrange
        when(userDAL.existsById(1)).thenReturn(true);
        doNothing().when(userDAL).deleteById(1);

        // Act
        userService.deleteUser(1);

        // Assert
        verify(userDAL).existsById(1);
        verify(userDAL).deleteById(1);
    }

    @Test
    void deleteUser_whenUserNotExists_shouldThrowResponseStatusException() {
        // Arrange
        when(userDAL.existsById(1)).thenReturn(false);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            userService.deleteUser(1);
        });
        verify(userDAL).existsById(1);
        verify(userDAL, never()).deleteById(1);
    }

    @Test
    void synchronizeUser_whenNewUser_shouldCreateUserAndPublishEvent() {
        // Arrange
        String newKeycloakId = "new-keycloak-id";
        String newUsername = "syncNewUser";
        String newEmail = "syncnew@example.com";
        when(userDAL.findByKeycloakId(newKeycloakId)).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setId(3);
        savedUser.setKeycloakId(newKeycloakId);
        savedUser.setUsername(newUsername);
        savedUser.setEmail(newEmail);
        savedUser.setCreatedAt(LocalDateTime.now());
        when(userDAL.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.synchronizeUser(newKeycloakId, newUsername, newEmail);

        // Assert
        assertNotNull(result);
        assertEquals(newKeycloakId, result.getKeycloakId());
        assertEquals(newUsername, result.getUsername());
        assertEquals(newEmail, result.getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDAL).save(userCaptor.capture());
        assertEquals(newKeycloakId, userCaptor.getValue().getKeycloakId());
        assertEquals(newUsername, userCaptor.getValue().getUsername());
        assertEquals(newEmail, userCaptor.getValue().getEmail());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("horizon.users.exchange"), eq("user.registered"), eventCaptor.capture());
        assertTrue(eventCaptor.getValue() instanceof UserRegisteredEvent);
        UserRegisteredEvent registeredEvent = (UserRegisteredEvent) eventCaptor.getValue();
        assertEquals(newKeycloakId, registeredEvent.getKeycloakId());
        assertEquals(newUsername, registeredEvent.getUsername());
        assertEquals(newEmail, registeredEvent.getEmail());
    }

    @Test
    void synchronizeUser_whenExistingUserWithUpdates_shouldUpdateUserAndPublishEvent() {
        // Arrange
        String existingKeycloakId = user.getKeycloakId();
        String updatedUsername = "syncUpdatedUser";
        String updatedEmail = "syncupdated@example.com";

        User existingUser = new User(); // Create a mutable copy for the test
        existingUser.setId(user.getId());
        existingUser.setKeycloakId(user.getKeycloakId());
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setAge(user.getAge());
        existingUser.setCreatedAt(user.getCreatedAt());

        when(userDAL.findByKeycloakId(existingKeycloakId)).thenReturn(Optional.of(existingUser));

        User savedUser = new User();
        savedUser.setId(existingUser.getId());
        savedUser.setKeycloakId(existingKeycloakId);
        savedUser.setUsername(updatedUsername);
        savedUser.setEmail(updatedEmail);
        savedUser.setAge(existingUser.getAge());
        savedUser.setCreatedAt(existingUser.getCreatedAt());
        when(userDAL.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.synchronizeUser(existingKeycloakId, updatedUsername, updatedEmail);

        // Assert
        assertNotNull(result);
        assertEquals(updatedUsername, result.getUsername());
        assertEquals(updatedEmail, result.getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDAL).save(userCaptor.capture());
        assertEquals(updatedUsername, userCaptor.getValue().getUsername());
        assertEquals(updatedEmail, userCaptor.getValue().getEmail());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("horizon.users.exchange"), eq("user.profile.updated"), eventCaptor.capture());
        assertTrue(eventCaptor.getValue() instanceof UserProfileUpdatedEvent);
        UserProfileUpdatedEvent updatedEvent = (UserProfileUpdatedEvent) eventCaptor.getValue();
        assertEquals(existingKeycloakId, updatedEvent.getKeycloakId());
        assertNotNull(updatedEvent.getUpdatedFields().get("username"));
        assertNotNull(updatedEvent.getUpdatedFields().get("email"));
    }

    @Test
    void synchronizeUser_whenExistingUserNoUpdates_shouldReturnUserAndNotPublishEvent() {
        // Arrange
        String existingKeycloakId = user.getKeycloakId();
        String sameUsername = user.getUsername();
        String sameEmail = user.getEmail();
        when(userDAL.findByKeycloakId(existingKeycloakId)).thenReturn(Optional.of(user));

        // Act
        User result = userService.synchronizeUser(existingKeycloakId, sameUsername, sameEmail);

        // Assert
        assertNotNull(result);
        assertEquals(sameUsername, result.getUsername());
        assertEquals(sameEmail, result.getEmail());
        verify(userDAL, never()).save(any(User.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void synchronizeUser_whenKeycloakIdIsNull_shouldReturnNullAndNotInteractWithDALOrRabbit() {
        // Act
        User result = userService.synchronizeUser(null, "test", "test@example.com");

        // Assert
        assertNull(result);
        verifyNoInteractions(userDAL);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void synchronizeUser_whenKeycloakIdIsEmpty_shouldReturnNullAndNotInteractWithDALOrRabbit() {
        // Act
        User result = userService.synchronizeUser(" ", "test", "test@example.com");

        // Assert
        assertNull(result);
        verifyNoInteractions(userDAL);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void getUserByKeycloakId_whenUserExists_shouldReturnUser() {
        // Arrange
        String keycloakId = user.getKeycloakId();
        when(userDAL.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));

        // Act
        User result = userService.getUserByKeycloakId(keycloakId);

        // Assert
        assertNotNull(result);
        assertEquals(keycloakId, result.getKeycloakId());
        assertEquals(user.getUsername(), result.getUsername());
        verify(userDAL).findByKeycloakId(keycloakId);
    }

    @Test
    void getUserByKeycloakId_whenUserNotExists_shouldThrowResponseStatusException() {
        // Arrange
        String keycloakId = "non-existent-keycloak-id";
        when(userDAL.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.getUserByKeycloakId(keycloakId);
        });
        assertEquals("User not found with keycloakId: " + keycloakId, exception.getReason());
        verify(userDAL).findByKeycloakId(keycloakId);
    }

    @Test
    void getUsersByKeycloakIds_whenIdsProvided_shouldReturnMatchingUsers() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(2);
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setKeycloakId("keycloak-another-id");

        List<String> keycloakIds = List.of(user.getKeycloakId(), anotherUser.getKeycloakId());
        List<User> expectedUsers = List.of(user, anotherUser);
        when(userDAL.findAllByKeycloakIdIn(keycloakIds)).thenReturn(expectedUsers);

        // Act
        List<User> result = userService.getUsersByKeycloakIds(keycloakIds);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(user));
        assertTrue(result.contains(anotherUser));
        verify(userDAL).findAllByKeycloakIdIn(keycloakIds);
    }

    @Test
    void getUsersByKeycloakIds_whenSomeIdsDontMatch_shouldReturnOnlyMatchingUsers() {
        // Arrange
        List<String> keycloakIds = List.of(user.getKeycloakId(), "non-existent-id");
        List<User> expectedUsers = List.of(user); // Only the existing user
        when(userDAL.findAllByKeycloakIdIn(keycloakIds)).thenReturn(expectedUsers);

        // Act
        List<User> result = userService.getUsersByKeycloakIds(keycloakIds);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(user));
        verify(userDAL).findAllByKeycloakIdIn(keycloakIds);
    }

    @Test
    void getUsersByKeycloakIds_whenNoIdsMatch_shouldReturnEmptyList() {
        // Arrange
        List<String> keycloakIds = List.of("non-existent-id-1", "non-existent-id-2");
        when(userDAL.findAllByKeycloakIdIn(keycloakIds)).thenReturn(new ArrayList<>());

        // Act
        List<User> result = userService.getUsersByKeycloakIds(keycloakIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userDAL).findAllByKeycloakIdIn(keycloakIds);
    }

    @Test
    void getUsersByKeycloakIds_whenEmptyListProvided_shouldReturnEmptyListAndNotCallDAL() {
        // Arrange
        List<String> keycloakIds = new ArrayList<>();

        // Act
        List<User> result = userService.getUsersByKeycloakIds(keycloakIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userDAL, never()).findAllByKeycloakIdIn(anyList());
    }

    @Test
    void getUsersByKeycloakIds_whenNullProvided_shouldReturnEmptyListAndNotCallDAL() {
        // Act
        List<User> result = userService.getUsersByKeycloakIds(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userDAL, never()).findAllByKeycloakIdIn(anyList());
    }

    @Test
    void updateUserByKeycloakId_whenUserExistsAndChangesMade_shouldUpdateUserAndPublishEvent() {
        // Arrange
        String keycloakId = user.getKeycloakId();
        UserUpdateDTO updateDetails = new UserUpdateDTO();
        updateDetails.setEmail("updatedByKeycloak@example.com");
        updateDetails.setAge("45");

        User existingUser = new User(); // Mutable copy for the test
        existingUser.setId(user.getId());
        existingUser.setKeycloakId(user.getKeycloakId());
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail()); // Old email
        existingUser.setAge(user.getAge());     // Old age
        existingUser.setCreatedAt(user.getCreatedAt());

        when(userDAL.findByKeycloakId(keycloakId)).thenReturn(Optional.of(existingUser));

        User savedUser = new User(); // Simulate the state after saving
        savedUser.setId(existingUser.getId());
        savedUser.setKeycloakId(keycloakId);
        savedUser.setUsername(existingUser.getUsername());
        savedUser.setEmail(updateDetails.getEmail());
        savedUser.setAge(updateDetails.getAge());
        savedUser.setCreatedAt(existingUser.getCreatedAt());
        when(userDAL.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.updateUserByKeycloakId(keycloakId, updateDetails);

        // Assert
        assertNotNull(result);
        assertEquals(updateDetails.getEmail(), result.getEmail());
        assertEquals(updateDetails.getAge(), result.getAge());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDAL).save(userCaptor.capture());
        assertEquals(updateDetails.getEmail(), userCaptor.getValue().getEmail());
        assertEquals(updateDetails.getAge(), userCaptor.getValue().getAge());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("horizon.users.exchange"), eq("user.profile.updated"), eventCaptor.capture());
        assertTrue(eventCaptor.getValue() instanceof UserProfileUpdatedEvent);
        UserProfileUpdatedEvent updatedEvent = (UserProfileUpdatedEvent) eventCaptor.getValue();
        assertEquals(keycloakId, updatedEvent.getKeycloakId());
        assertTrue(updatedEvent.getUpdatedFields().containsKey("email"));
        assertTrue(updatedEvent.getUpdatedFields().containsKey("age"));
    }

    @Test
    void updateUserByKeycloakId_whenUserExistsAndOnlyEmailChanged_shouldUpdateUserAndPublishEvent() {
        // Arrange
        String keycloakId = user.getKeycloakId();
        UserUpdateDTO updateDetails = new UserUpdateDTO();
        updateDetails.setEmail("updatedEmailOnlyByKeycloak@example.com");
        // Age is null in updateDetails, so it shouldn't be updated

        User existingUser = new User();
        existingUser.setId(user.getId());
        existingUser.setKeycloakId(user.getKeycloakId());
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail()); 
        existingUser.setAge(user.getAge());     
        existingUser.setCreatedAt(user.getCreatedAt());
        when(userDAL.findByKeycloakId(keycloakId)).thenReturn(Optional.of(existingUser));

        User savedUser = new User();
        savedUser.setId(existingUser.getId());
        savedUser.setKeycloakId(keycloakId);
        savedUser.setUsername(existingUser.getUsername());
        savedUser.setEmail(updateDetails.getEmail()); // New Email
        savedUser.setAge(existingUser.getAge());    // Old Age
        savedUser.setCreatedAt(existingUser.getCreatedAt());
        when(userDAL.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.updateUserByKeycloakId(keycloakId, updateDetails);

        // Assert
        assertNotNull(result);
        assertEquals(updateDetails.getEmail(), result.getEmail());
        assertEquals(existingUser.getAge(), result.getAge()); // Age should be the old age

        verify(userDAL).save(any(User.class));
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("horizon.users.exchange"), eq("user.profile.updated"), eventCaptor.capture());
        assertTrue(eventCaptor.getValue() instanceof UserProfileUpdatedEvent);
        UserProfileUpdatedEvent updatedEvent = (UserProfileUpdatedEvent) eventCaptor.getValue();
        assertEquals(keycloakId, updatedEvent.getKeycloakId());
        assertTrue(updatedEvent.getUpdatedFields().containsKey("email"));
        assertFalse(updatedEvent.getUpdatedFields().containsKey("age"));
    }

    @Test
    void updateUserByKeycloakId_whenUserExistsAndNoChangesMade_shouldReturnUserAndNotPublishEvent() {
        // Arrange
        String keycloakId = user.getKeycloakId();
        UserUpdateDTO noChangeDetails = new UserUpdateDTO(); // Empty DTO, or DTO with same values
        noChangeDetails.setEmail(user.getEmail());
        noChangeDetails.setAge(user.getAge());

        when(userDAL.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        // Note: userDAL.save should NOT be called in this case by the actual implementation

        // Act
        User result = userService.updateUserByKeycloakId(keycloakId, noChangeDetails);

        // Assert
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getAge(), result.getAge());
        verify(userDAL, never()).save(any(User.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void updateUserByKeycloakId_whenUserNotExists_shouldThrowResponseStatusException() {
        // Arrange
        String nonExistentKeycloakId = "non-existent-for-update";
        UserUpdateDTO updateDetails = new UserUpdateDTO();
        updateDetails.setEmail("any@example.com");
        when(userDAL.findByKeycloakId(nonExistentKeycloakId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.updateUserByKeycloakId(nonExistentKeycloakId, updateDetails);
        });
        assertEquals("User not found with keycloakId: " + nonExistentKeycloakId, exception.getReason());
        verify(userDAL, never()).save(any(User.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // TODO: Add unit tests for other methods:
    // - Test RabbitMQ interactions for event publishing methods (synchronizeUser, updateUserByKeycloakId) -> Covered for synchronizeUser & updateUserByKeycloakId
} 