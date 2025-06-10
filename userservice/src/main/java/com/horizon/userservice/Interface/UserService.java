package com.horizon.userservice.Interface;

import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import com.horizon.userservice.DTO.UserSyncRequestDTO;
import com.horizon.userservice.DTO.UserUpdateDTO;
import com.horizon.userservice.model.User;

import java.util.List;

public interface UserService {
    UserResponseDTO getUserById(String id);

    UserResponseDTO getUserById(int id);

    List<UserResponseDTO> getAllUsers();
    UserResponseDTO createUser(UserCreateDTO dto);
    UserResponseDTO updateUser(String id, UserUpdateDTO dto);
    void deleteUser(String id);

    void initiateUserDeletionSaga(String userId);

    void finalizeUserDeletion(String userId);

    void syncUser(UserSyncRequestDTO syncRequest);

    User getUserByKeycloakId(String keycloakId);
    List<User> getUsersByKeycloakIds(List<String> keycloakIds);
    User updateUserByKeycloakId(String keycloakId, UserUpdateDTO userDetails);

    UserResponseDTO updateUser(int id, UserUpdateDTO dto);

    void deleteUser(int id);

    /**
     * Synchronizes user information from an external source (e.g., Keycloak event or JWT).
     * Creates a new user if one doesn't exist for the given keycloakId,
     * or updates the existing user's username or email if they differ.
     *
     * @param keycloakId the Keycloak ID of the user
     * @param username the username
     * @param email the email
     * @return the synchronized User entity.
     */
    User synchronizeUser(String keycloakId, String username, String email);
}
