package com.horizon.userservice.Interface;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import com.horizon.userservice.DTO.UserUpdateDTO;
import com.horizon.userservice.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserDAL userDAL;

    @Autowired
    public UserServiceImpl(UserDAL userDAL) {
        this.userDAL = userDAL;
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
        user.setPassword(dto.getPassword()); // hash dit later!
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
        if (dto.getPassword() != null) user.setPassword(dto.getPassword()); // hash!

        return mapToResponseDTO(userDAL.save(user));
    }

    @Override
    public void deleteUser(int id) {
        if (!userDAL.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userDAL.deleteById(id);
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAge(user.getAge());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}

