package com.nexusmarket.authservice.controller;

import com.nexusmarket.authservice.dto.AuthRequest;
import com.nexusmarket.authservice.entity.UserCredential;
import com.nexusmarket.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
// Removing @RequestMapping("/auth") here to match the stripped path coming from Gateway
public class AuthController {

    @Autowired
    private AuthService service;

    @PostMapping("/register") // Matches the stripped path
    public String addNewUser(@RequestBody UserCredential user) {
        return service.saveUser(user);
    }

    @PostMapping("/login") // Matches the stripped path
    public String getToken(@RequestBody AuthRequest authRequest) {
        return service.generateToken(authRequest.getUsername(), authRequest.getPassword());
    }
}