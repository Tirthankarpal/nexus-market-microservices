package com.nexusmarket.authservice.controller;

import com.nexusmarket.authservice.dto.AuthRequest;
import com.nexusmarket.authservice.entity.UserCredential;
import com.nexusmarket.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService service;

    // Endpoint to create a new user account
    @PostMapping("/register")
    public String addNewUser(@RequestBody UserCredential user) {
        return service.saveUser(user);
    }

    // Endpoint to login and get a token
    @PostMapping("/login")
    public String getToken(@RequestBody AuthRequest authRequest) {
        return service.generateToken(authRequest.getUsername(), authRequest.getPassword());
    }
}