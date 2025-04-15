package com.horizon.userservice.Interface;

import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import com.horizon.userservice.DTO.UserUpdateDTO;

import java.util.List;

public interface UserService {
    UserResponseDTO getUserById(int id);
    List<UserResponseDTO> getAllUsers();
    UserResponseDTO createUser(UserCreateDTO dto);
    UserResponseDTO updateUser(int id, UserUpdateDTO dto);
    void deleteUser(int id);
}
