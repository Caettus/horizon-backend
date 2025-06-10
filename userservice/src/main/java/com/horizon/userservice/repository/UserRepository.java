package com.horizon.userservice.repository;

import com.horizon.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByKeycloakId(String keycloakId);
    void deleteByKeycloakId(String keycloakId);
} 