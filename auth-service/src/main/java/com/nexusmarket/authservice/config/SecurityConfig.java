package com.nexusmarket.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // This tells Spring to use BCrypt when we encode or verify passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // This configures our network firewall rules for this specific service
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // We disable CSRF because JWTs are immune to it
                .authorizeHttpRequests(auth -> auth.requestMatchers("/auth/login", "/auth/register").permitAll() // Open the front door.anyRequest().authenticated() // Lock everything else down
                );
        return http.build();
    }
}