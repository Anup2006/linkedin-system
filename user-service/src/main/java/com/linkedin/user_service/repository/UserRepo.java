package com.linkedin.user_service.repository;

import com.linkedin.user_service.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User,String> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
