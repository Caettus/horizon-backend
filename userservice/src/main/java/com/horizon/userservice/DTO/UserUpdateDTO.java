package com.horizon.userservice.dto;

import jakarta.validation.constraints.Email;

public class UserUpdateDTO {
    @Email
    private String email;

    private String age;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }
}

