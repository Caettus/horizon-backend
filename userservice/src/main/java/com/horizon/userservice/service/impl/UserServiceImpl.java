package com.horizon.userservice.service.impl;

import com.horizon.userservice.repository.UserRepository;
import com.horizon.userservice.dto.UserCreateDTO;
import com.horizon.userservice.dto.UserResponseDTO;
import com.horizon.userservice.dto.UserUpdateDTO;
import com.horizon.userservice.event.UserForgottenEvent;
import com.horizon.userservice.event.UserRegisteredEvent;
import com.horizon.userservice.event.UserProfileUpdatedEvent;
import com.horizon.userservice.eventbus.EventCreatedListener;
import com.horizon.userservice.model.User;
import com.horizon.userservice.service.UserService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final EventCreatedListener eventCreatedListener;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RabbitTemplate rabbitTemplate, EventCreatedListener eventCreatedListener) {
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.eventCreatedListener = eventCreatedListener;
    }

    @Override
    public UserResponseDTO getUserById(int id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        return mapToResponseDTO(user);
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO createUser(UserCreateDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setAge(dto.getAge());
        user.setKeycloakId(dto.getKeycloakId());
        user.setCreatedAt(LocalDateTime.now());
        return mapToResponseDTO(userRepository.save(user));
    }

    @Override
    public UserResponseDTO updateUser(int id, UserUpdateDTO dto) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );

        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAge() != null) user.setAge(dto.getAge());

        return mapToResponseDTO(userRepository.save(user));
    }

    @Override
    public void deleteUser(int id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }

    @Override
    public void deleteUserByKeycloakId(String keycloakId) {
        Optional<User> userOptional = userRepository.findByKeycloakId(keycloakId);
        if (userOptional.isPresent()) {
            userRepository.delete(userOptional.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + keycloakId);
        }
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

        Optional<User> existingUserOpt = userRepository.findByKeycloakId(keycloakId);
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
                userToSave = userRepository.save(userToSave);
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
            userToSave = userRepository.save(userToSave);
            // Publish UserRegisteredEvent
            UserRegisteredEvent event = new UserRegisteredEvent(userToSave.getKeycloakId(), userToSave.getUsername(), userToSave.getEmail());
            rabbitTemplate.convertAndSend("horizon.users.exchange", "user.registered", event);
            System.out.println("Published UserRegisteredEvent for keycloakId: " + userToSave.getKeycloakId()); // Logging
        }
        return userToSave;
    }

    @Override
    public User getUserByKeycloakId(String keycloakId) {
        // TODO: Implement actual logic
        return userRepository.findByKeycloakId(keycloakId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + keycloakId));
    }

    @Override
    public List<User> getUsersByKeycloakIds(List<String> keycloakIds) {
        if (keycloakIds == null || keycloakIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllByKeycloakIdIn(keycloakIds); // Use the new DAL method
    }

    @Override
    public User updateUserByKeycloakId(String keycloakId, UserUpdateDTO userDetails) {
        User user = userRepository.findByKeycloakId(keycloakId).orElseThrow(() ->
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

        // TODO: Add other updatable fields from UserUpdateDTO in a similar manner
        // Example for a hypothetical 'displayName' field:
        // if (userDetails.getDisplayName() != null && !userDetails.getDisplayName().equals(user.getDisplayName())) {
        //    String oldDisplayName = user.getDisplayName();
        //    user.setDisplayName(userDetails.getDisplayName());
        //    updatedFields.put("displayName", Map.of("oldValue", oldDisplayName, "newValue", user.getDisplayName()));
        // }

        if (!updatedFields.isEmpty()) {
            User updatedUser = userRepository.save(user);
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