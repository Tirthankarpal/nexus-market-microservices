package com.nexusmarket.authservice.repository;

import com.nexusmarket.authservice.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserCredential, Long> {

    // Spring automatically generates the SQL to find a user by this exact column
    Optional<UserCredential> findByUsername(String username);
}