package com.horizon.userservice.controller;

import com.horizon.userservice.DTO.UserDTO;
import com.horizon.userservice.Interface.UserInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UsersController {

    @Autowired
    private UserInterface userInterface;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserDetails(@PathVariable("id") int id) {
        UserDTO response = userInterface.getUserById(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping(value = "/test", produces = MediaType.TEXT_PLAIN_VALUE)
    public String testEndpoint() {
        return "UserController is working!";
    }
}