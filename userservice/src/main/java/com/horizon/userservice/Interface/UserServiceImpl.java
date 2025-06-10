package com.horizon.userservice.Interface;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import com.horizon.userservice.DTO.UserUpdateDTO;
import com.horizon.userservice.DTO.UserSyncRequestDTO;
import com.horizon.userservice.event.UserProfileUpdatedEvent;
import com.horizon.userservice.event.UserRegisteredEvent;
import com.horizon.userservice.eventbus.EventCreatedListener;
import com.horizon.userservice.model.User;
import com.horizon.userservice.saga.SagaCoordinator;
import com.horizon.userservice.saga.UserDeletionMessage;
import com.horizon.userservice.saga.SagaRabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final SagaCoordinator sagaCoordinator;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    public UserServiceImpl(UserDAL userDAL, RabbitTemplate rabbitTemplate, EventCreatedListener eventCreatedListener, SagaCoordinator sagaCoordinator) {
        this.userDAL = userDAL;
        this.rabbitTemplate = rabbitTemplate;
        this.eventCreatedListener = eventCreatedListener;
        this.sagaCoordinator = sagaCoordinator;
    }

    @Override
    public UserResponseDTO getUserById(String id) {
        User user = userDAL.findByKeycloakId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + id)
        );
        return mapToResponseDTO(user);
    }

    @Override
    public UserResponseDTO getUserById(int id) {
        User user = userDAL.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id)
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
        user.setUsername(userCreateDTO.getUsername());
        user.setEmail(userCreateDTO.getEmail());
        user.setAge(userCreateDTO.getAge());
        user.setKeycloakId(userCreateDTO.getKeycloakId());

        User savedUser = userDAL.save(user);

        UserRegisteredEvent event = new UserRegisteredEvent(savedUser.getKeycloakId(), savedUser.getUsername(), savedUser.getEmail());
        rabbitTemplate.convertAndSend("horizon.users.exchange", "user.registered", event);

        return mapToResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public void initiateUserDeletionSaga(String userId) {
        userDAL.findByKeycloakId(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + userId));
        String sagaId = UUID.randomUUID().toString();
        sagaCoordinator.startSaga(sagaId, userId);
        UserDeletionMessage message = new UserDeletionMessage(sagaId, userId);
        rabbitTemplate.convertAndSend(SagaRabbitMQConfig.SAGA_EXCHANGE_NAME, "", message);
        logger.info("Published user deletion event for saga {} and user {}", sagaId, userId);
    }

    @Override
    @Transactional
    public void finalizeUserDeletion(String userId) {
        logger.info("Finalizing deletion for user {}", userId);
        User user = userDAL.findByKeycloakId(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + userId));
        userDAL.deleteById(user.getId());
        logger.info("User {} deleted from database.", userId);
    }

    @Override
    public void syncUser(UserSyncRequestDTO syncRequest) {
        synchronizeUser(syncRequest.getKeycloakId(), syncRequest.getUsername(), syncRequest.getEmail());
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
    public void deleteUser(String id) {
        User user = userDAL.findByKeycloakId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + id));
        initiateUserDeletionSaga(user.getKeycloakId());
    }

    @Override
    public User synchronizeUser(String keycloakId, String username, String email) {
        if (keycloakId == null || keycloakId.trim().isEmpty()) {
            logger.error("keycloakId is null or empty in synchronizeUser. Skipping.");
            return null;
        }
        if (username == null || username.trim().isEmpty()) {
            logger.error("username is null or empty in synchronizeUser for keycloakId: " + keycloakId + ". Skipping.");
            return null;
        }
        if (email == null || email.trim().isEmpty()) {
            logger.error("email is null or empty in synchronizeUser for keycloakId: " + keycloakId + ". Skipping.");
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
                    logger.info("Published UserProfileUpdatedEvent from synchronizeUser for keycloakId: " + userToSave.getKeycloakId() + " with fields: " + updatedFieldsForEvent.keySet());
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
            logger.info("Published UserRegisteredEvent for keycloakId: " + userToSave.getKeycloakId());
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
        return userDAL.findAllByKeycloakIdIn(keycloakIds);
    }

    @Override
    public UserResponseDTO updateUser(String id, UserUpdateDTO dto) {
        User user = userDAL.findByKeycloakId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + id));
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAge() != null) user.setAge(dto.getAge());
        return mapToResponseDTO(userDAL.save(user));
    }


    @Override
    public User updateUserByKeycloakId(String keycloakId, UserUpdateDTO userDetails) {
        User user = userDAL.findByKeycloakId(keycloakId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with keycloakId: " + keycloakId)
        );

        Map<String, Object> updatedFields = new HashMap<>();

        if (userDetails.getEmail() != null && !userDetails.getEmail().equals(user.getEmail())) {
            String oldEmail = user.getEmail();
            user.setEmail(userDetails.getEmail());
            updatedFields.put("email", Map.of("oldValue", String.valueOf(oldEmail), "newValue", user.getEmail()));
        }

        if (userDetails.getAge() != null && !userDetails.getAge().equals(user.getAge())) {
            String oldAge = user.getAge();
            user.setAge(userDetails.getAge());
            updatedFields.put("age", Map.of("oldValue", String.valueOf(oldAge), "newValue", user.getAge()));
        }

        if (!updatedFields.isEmpty()) {
            User updatedUser = userDAL.save(user);
            UserProfileUpdatedEvent event = new UserProfileUpdatedEvent(updatedUser.getKeycloakId(), updatedFields);
            rabbitTemplate.convertAndSend("horizon.users.exchange", "user.profile.updated", event);
            logger.info("Published UserProfileUpdatedEvent for keycloakId: " + updatedUser.getKeycloakId() + " with fields: " + updatedFields.keySet());
            return updatedUser;
        } else {
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
        dto.setEventsCreated(user.getEventsCreatedCount());
        return dto;
    }
}