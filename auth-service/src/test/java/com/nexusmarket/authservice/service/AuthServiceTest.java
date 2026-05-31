package com.nexusmarket.authservice.service;

import com.nexusmarket.authservice.entity.UserCredential;
import com.nexusmarket.authservice.repository.UserRepository;
import com.nexusmarket.authservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private UserCredential user;

    @BeforeEach
    void setUp() {
        user = new UserCredential();
        user.setUsername("john_doe");
        user.setPassword("rawPassword123");
        user.setRole("USER");
    }

    @Test
    void saveUser_ShouldHashPasswordAndSaveUser() {
        // Arrange
        when(passwordEncoder.encode("rawPassword123")).thenReturn("hashedPasswordXYZ");
        when(userRepository.save(any(UserCredential.class))).thenReturn(user);

        // Act
        String result = authService.saveUser(user);

        // Assert
        assertEquals("User added to the system securely!", result);
        assertEquals("hashedPasswordXYZ", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void generateToken_WithValidCredentials_ShouldReturnToken() {
        // Arrange
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword123", "rawPassword123")).thenReturn(true);
        when(jwtUtil.generateToken("john_doe", "USER")).thenReturn("mocked-jwt-token");

        // Act
        String token = authService.generateToken("john_doe", "rawPassword123");

        // Assert
        assertEquals("mocked-jwt-token", token);
    }

    @Test
    void generateToken_WithInvalidPassword_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "rawPassword123")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.generateToken("john_doe", "wrongPassword");
        });
        assertEquals("Invalid Password", exception.getMessage());
    }
}
