package com.horizon.userservice.Interface;

import com.horizon.userservice.model.User;
import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.DTO.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserInterface {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserDAL userDAL;

    public com.horizon.userservice.DTO.UserDTO getUserById(int id) {
        Optional<User> userOptional = userDAL.findById(id);

        if (userOptional.isEmpty()) {
            return null;
        }

        User user = userOptional.get();

        UserDTO response = new UserDTO();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setAge(user.getAge());
        response.setEmail(user.getEmail());

        return response;
    }
}
