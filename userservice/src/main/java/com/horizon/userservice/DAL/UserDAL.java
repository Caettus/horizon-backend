package com.horizon.userservice.DAL;

import com.horizon.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDAL extends JpaRepository<User, Integer> {
}
