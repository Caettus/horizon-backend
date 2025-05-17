package com.horizon.userservice.Interface;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import com.horizon.userservice.DTO.UserUpdateDTO;
import com.horizon.userservice.event.UserRegisteredEvent;
import com.horizon.userservice.event.UserProfileUpdatedEvent;
import com.horizon.userservice.model.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
public class UserServiceImpl implements UserService {

    private final UserDAL userDAL;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public UserServiceImpl(UserDAL userDAL, RabbitTemplate rabbitTemplate) {
        this.userDAL = userDAL;
        this.rabbitTemplate = rabbitTemplate;
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
    public UserResponseDTO createUser(UserCreateDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setAge(dto.getAge());
        user.setKeycloakId(dto.getKeycloakId());
        user.setCreatedAt(LocalDateTime.now());
        return mapToResponseDTO(userDAL.save(user));
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
        // TODO: Implement actual logic
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

        // TODO: Add other updatable fields from UserUpdateDTO in a similar manner
        // Example for a hypothetical 'displayName' field:
        // if (userDetails.getDisplayName() != null && !userDetails.getDisplayName().equals(user.getDisplayName())) {
        //    String oldDisplayName = user.getDisplayName();
        //    user.setDisplayName(userDetails.getDisplayName());
        //    updatedFields.put("displayName", Map.of("oldValue", oldDisplayName, "newValue", user.getDisplayName()));
        // }

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
        return dto;
    }
}

