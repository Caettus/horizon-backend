package com.horizon.userservice.controller;

import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import com.horizon.userservice.DTO.UserUpdateDTO;
import com.horizon.userservice.DTO.UserSyncRequestDTO;
import com.horizon.userservice.Interface.UserService;
import com.horizon.userservice.model.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@Validated
public class UsersController {

    private final UserService userService;

    @Autowired
    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserDetails(@PathVariable int id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        UserResponseDTO created = userService.createUser(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable int id, @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable int id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/keycloak/{keycloakId}")
    public ResponseEntity<UserResponseDTO> getUserByKeycloakId(@PathVariable String keycloakId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        return ResponseEntity.ok(mapToResponseDTO(user));
    }

    @GetMapping("/batch")
    public ResponseEntity<List<UserResponseDTO>> getUsersByKeycloakIds(@RequestParam List<String> ids) {
        List<User> users = userService.getUsersByKeycloakIds(ids);
        List<UserResponseDTO> dtos = users.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getCurrentUserProfile(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        User user = userService.getUserByKeycloakId(keycloakId);
        return ResponseEntity.ok(mapToResponseDTO(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateCurrentUserProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserUpdateDTO dto) {
        String keycloakId = jwt.getSubject();
        User updatedUser = userService.updateUserByKeycloakId(keycloakId, dto);
        return ResponseEntity.ok(mapToResponseDTO(updatedUser));
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

    @PostMapping("/internal/synchronize")
    public ResponseEntity<UserResponseDTO> synchronizeUser(@RequestBody UserSyncRequestDTO syncRequest) {
        User user = userService.synchronizeUser(syncRequest.getKeycloakId(), syncRequest.getUsername(), syncRequest.getEmail());
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(mapToResponseDTO(user));
    }
}
