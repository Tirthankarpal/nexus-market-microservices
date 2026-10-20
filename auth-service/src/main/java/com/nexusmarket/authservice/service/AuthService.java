package com.nexusmarket.authservice.service;

import com.nexusmarket.authservice.entity.UserCredential;
import com.nexusmarket.authservice.repository.UserRepository;
import com.nexusmarket.authservice.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nexusmarket.authservice.exception.InvalidCredentialsException;
import com.nexusmarket.authservice.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 1. Register a new user (Hashes the password before saving!)
    public String saveUser(UserCredential credential) {
        if (credential.getUsername() == null || credential.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (repository.findByUsername(credential.getUsername().trim()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        credential.setUsername(credential.getUsername().trim());
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));

        // Security check: Only allow ADMIN if this is the very first user in the database
        if (repository.count() == 0 && "ADMIN".equalsIgnoreCase(credential.getRole())) {
            credential.setRole("ADMIN");
        } else {
            // Prevent privilege escalation by forcing standard role
            credential.setRole("USER");
        }

        repository.save(credential);
        return "User added to the system securely!";
    }

    // 2. Verify login and generate JWT
    public String generateToken(String username, String rawPassword) {
        // Fetch the user from the database
        UserCredential user = repository.findByUsername(username != null ? username.trim() : "")
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Compare the raw password with the hashed password in the DB
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            // If they match, generate the digital wax seal (JWT)
            return jwtUtil.generateToken(user.getUsername(), user.getRole() != null ? user.getRole() : "USER");
        } else {
            throw new InvalidCredentialsException("Invalid Password");
        }
    }
}
