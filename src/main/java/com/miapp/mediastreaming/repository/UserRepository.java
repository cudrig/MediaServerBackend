package com.miapp.mediastreaming.repository;

import com.miapp.mediastreaming.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByToken(String token);
    User findByUsername(String username); // Nuevo método
}