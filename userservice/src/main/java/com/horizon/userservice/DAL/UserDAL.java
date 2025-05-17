package com.horizon.userservice.DAL;

import com.horizon.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDAL extends JpaRepository<User, Integer> {
    Optional<User> findByKeycloakId(String keycloakId);
    List<User> findAllByKeycloakIdIn(List<String> keycloakIds);
}
