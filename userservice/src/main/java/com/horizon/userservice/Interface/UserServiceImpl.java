package com.horizon.userservice.Interface;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import com.horizon.userservice.DTO.UserUpdateDTO;
import com.horizon.userservice.event.UserProfileUpdatedEvent;
import com.horizon.userservice.event.UserRegisteredEvent;
import com.horizon.userservice.eventbus.EventCreatedListener;
import com.horizon.userservice.model.User;
import com.horizon.userservice.saga.UserDeletionMessage;
import com.horizon.userservice.saga.SagaRabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@Service
public class UserServiceImpl implements UserService {

    private final UserDAL userDAL;
    private final RabbitTemplate rabbitTemplate;
    private final EventCreatedListener eventCreatedListener;

    @Autowired
    public UserServiceImpl(UserDAL userDAL, RabbitTemplate rabbitTemplate, EventCreatedListener eventCreatedListener) {
        this.userDAL = userDAL;
        this.rabbitTemplate = rabbitTemplate;
        this.eventCreatedListener = eventCreatedListener;
    }

    @Override
    public UserResponseDTO getUserById(int id) {
        User user = userDAL.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        return mapToResponseDTO(user);
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userDAL.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {
        User user = new User();
        user.setFirstname(userCreateDTO.getFirstName());
        user.setLastname(userCreateDTO.getLastName());
        user.setEmail(userCreateDTO.getEmail());

        User savedUser = userDAL.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(this, savedUser.getId(), savedUser.getFirstname(), savedUser.getLastname(), savedUser.getEmail()));

        return mapToUserResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public void initiateUserDeletionSaga(String userId) {
        // 1. Check if user exists
        userDAL.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Create a unique ID for this saga instance
        String sagaId = UUID.randomUUID().toString();

        // 3. Start the saga in the coordinator
        sagaCoordinator.startSaga(sagaId, userId);

        // 4. Publish the user deletion event
        UserDeletionMessage message = new UserDeletionMessage(sagaId, userId);
        rabbitTemplate.convertAndSend(SagaRabbitMQConfig.SAGA_EXCHANGE_NAME, "", message);
        logger.info("Published user deletion event for saga {} and user {}", sagaId, userId);
    }

    @Override
    @Transactional
    public void finalizeUserDeletion(String userId) {
        logger.info("Finalizing deletion for user {}", userId);
        // Here you would also delete the user from Keycloak
        userDAL.deleteById(userId);
        logger.info("User {} deleted from database.", userId);
    }


    @Override
    public UserResponseDTO updateUser(int id, UserUpdateDTO dto) {
        User user = userDAL.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );

        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAge() != null) user.setAge(dto.getAge());

        return mapToResponseDTO(userDAL.save(user));
    }

    @Override
    public void deleteUser(int id) {
        if (!userDAL.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userDAL.deleteById(id);
    }

    @Override
    public User synchronizeUser(String keycloakId, String username, String email) {
        if (keycloakId == null || keycloakId.trim().isEmpty()) {
            System.err.println("keycloakId is null or empty in synchronizeUser. Skipping.");
            return null; 
        }
        if (username == null || username.trim().isEmpty()) {
            System.err.println("username is null or empty in synchronizeUser for keycloakId: " + keycloakId + ". Skipping.");
            return null;
        }
        if (email == null || email.trim().isEmpty()) {
            System.err.println("email is null or empty in synchronizeUser for keycloakId: " + keycloakId + ". Skipping.");
            return null;
        }

        Optional<User> existingUserOpt = userDAL.findByKeycloakId(keycloakId);
        User userToSave;
        Map<String, Object> updatedFieldsForEvent = new HashMap<>();

        if (existingUserOpt.isPresent()) {
            userToSave = existingUserOpt.get();
            String oldUsername = userToSave.getUsername();
            String oldEmail = userToSave.getEmail();
            boolean updated = false;

            if (username != null && !username.equals(userToSave.getUsername())) {
                userToSave.setUsername(username);
                updatedFieldsForEvent.put("username", Map.of("oldValue", oldUsername, "newValue", username));
                updated = true;
            }
            if (email != null && !email.equals(userToSave.getEmail())) {
                userToSave.setEmail(email);
                updatedFieldsForEvent.put("email", Map.of("oldValue", oldEmail, "newValue", email));
                updated = true;
            }

            if (updated) {
                userToSave = userDAL.save(userToSave);
                if (!updatedFieldsForEvent.isEmpty()) {
                    UserProfileUpdatedEvent event = new UserProfileUpdatedEvent(userToSave.getKeycloakId(), updatedFieldsForEvent);
                    rabbitTemplate.convertAndSend("horizon.users.exchange", "user.profile.updated", event);
                    System.out.println("Published UserProfileUpdatedEvent from synchronizeUser for keycloakId: " + userToSave.getKeycloakId() + " with fields: " + updatedFieldsForEvent.keySet());
                }
            }
        } else {
            userToSave = new User();
            userToSave.setKeycloakId(keycloakId);
            userToSave.setUsername(username);
            userToSave.setEmail(email);
            // @PrePersist handles createdAt
            userToSave = userDAL.save(userToSave);
            // Publish UserRegisteredEvent
            UserRegisteredEvent event = new UserRegisteredEvent(userToSave.getKeycloakId(), userToSave.getUsername(), userToSave.getEmail());
            rabbitTemplate.convertAndSend("horizon.users.exchange", "user.registered", event);
            System.out.println("Published UserRegisteredEvent for keycloakId: " + userToSave.getKeycloakId()); // Logging
        }
        return userToSave;
    }

    @Override
    public User getUserByKeycloakId(String keycloakId) {
        return userDAL.findByKeycloakId(keycloakId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + keycloakId));
    }

    @Override
    public List<User> getUsersByKeycloakIds(List<String> keycloakIds) {
        if (keycloakIds == null || keycloakIds.isEmpty()) {
            return List.of();
        }
        return userDAL.findAllByKeycloakIdIn(keycloakIds); // Use the new DAL method
    }

    @Override
    public User updateUserByKeycloakId(String keycloakId, UserUpdateDTO userDetails) {
        User user = userDAL.findByKeycloakId(keycloakId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + keycloakId)
        );

        Map<String, Object> updatedFields = new HashMap<>();

        // Email
        if (userDetails.getEmail() != null && !userDetails.getEmail().equals(user.getEmail())) {
            String oldEmail = user.getEmail();
            user.setEmail(userDetails.getEmail());
            updatedFields.put("email", Map.of("oldValue", String.valueOf(oldEmail), "newValue", user.getEmail()));
        }

        // Age
        if (userDetails.getAge() != null && !userDetails.getAge().equals(user.getAge())) {
            String oldAge = user.getAge();
            user.setAge(userDetails.getAge());
            updatedFields.put("age", Map.of("oldValue", String.valueOf(oldAge), "newValue", user.getAge()));
        }

        if (!updatedFields.isEmpty()) {
            User updatedUser = userDAL.save(user);
            UserProfileUpdatedEvent event = new UserProfileUpdatedEvent(updatedUser.getKeycloakId(), updatedFields);
            rabbitTemplate.convertAndSend("horizon.users.exchange", "user.profile.updated", event);
            System.out.println("Published UserProfileUpdatedEvent for keycloakId: " + updatedUser.getKeycloakId() + " with fields: " + updatedFields.keySet()); // Logging
            return updatedUser;
        } else {
            // No actual changes, just return the user as is, no event published
            return user;
        }
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAge(user.getAge());
        dto.setKeycloakId(user.getKeycloakId());
        dto.setCreatedAt(user.getCreatedAt());

        // Populate eventsCreated field
        if (user.getKeycloakId() != null) {
            System.out.println("[UserServiceImpl] Mapping DTO for user: " + user.getUsername() + ", Keycloak ID: " + user.getKeycloakId());
            try {
                UUID userUuid = UUID.fromString(user.getKeycloakId());
                System.out.println("[UserServiceImpl] Converted Keycloak ID to UUID: " + userUuid);
                int count = eventCreatedListener.getEventCount(userUuid);
                System.out.println("[UserServiceImpl] Fetched event count for UUID " + userUuid + ": " + count);
                dto.setEventsCreated(count);
            } catch (IllegalArgumentException e) {
                System.err.println("[UserServiceImpl] Invalid KeycloakId format for user " + user.getId() + " (" + user.getUsername() + "): " + user.getKeycloakId() + ". Error: " + e.getMessage());
                dto.setEventsCreated(0);
            }
        } else {
            System.out.println("[UserServiceImpl] User " + user.getUsername() + " has null Keycloak ID.");
            dto.setEventsCreated(0);
        }
        return dto;
    }
}

