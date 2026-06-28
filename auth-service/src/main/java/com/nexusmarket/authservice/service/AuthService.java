package com.nexusmarket.authservice.service;

import com.nexusmarket.authservice.entity.UserCredential;
import com.nexusmarket.authservice.repository.UserRepository;
import com.nexusmarket.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        repository.save(credential);
        return "User added to the system securely!";
    }

    // 2. Verify login and generate JWT
    public String generateToken(String username, String rawPassword) {
        // Fetch the user from the database
        UserCredential user = repository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Compare the raw password with the hashed password in the DB
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            // If they match, generate the digital wax seal (JWT)
            return jwtUtil.generateToken(username, user.getRole());
        } else {
            throw new InvalidCredentialsException("Invalid Password");
        }
    }
}