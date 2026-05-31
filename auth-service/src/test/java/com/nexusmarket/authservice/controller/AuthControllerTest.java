package com.nexusmarket.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusmarket.authservice.config.SecurityConfig;
import com.nexusmarket.authservice.dto.AuthRequest;
import com.nexusmarket.authservice.entity.UserCredential;
import com.nexusmarket.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void addNewUser_WithValidInput_ShouldReturnSuccess() throws Exception {
        // Arrange
        UserCredential user = new UserCredential();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setRole("USER");

        when(authService.saveUser(any(UserCredential.class))).thenReturn("User added to the system securely!");

        // Act & Assert
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("User added to the system securely!"));
    }

    @Test
    void addNewUser_WithBlankUsername_ShouldReturnBadRequest() throws Exception {
        // Arrange
        UserCredential user = new UserCredential();
        user.setUsername(""); // Invalid
        user.setPassword("password123");
        user.setRole("USER");

        // Act & Assert
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getToken_WithValidInput_ShouldReturnToken() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(authService.generateToken("testuser", "password123")).thenReturn("mocked-jwt-token");

        // Act & Assert
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("mocked-jwt-token"));
    }

    @Test
    void getToken_WithBlankPassword_ShouldReturnBadRequest() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword(""); // Invalid

        // Act & Assert
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
