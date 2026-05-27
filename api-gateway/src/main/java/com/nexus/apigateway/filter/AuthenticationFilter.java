package com.nexus.apigateway.filter;

import com.nexus.apigateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. OPEN THE FRONT DOOR: Let login & register requests pass through safely
        if (path.startsWith("/auth/")) {
            return chain.filter(exchange);
        }

        // 2. CHECK FOR A PASS: Does the request have an Authorization header?
        // FIX: Using the modern containsHeader() instead of containsKey()
        if (!exchange.getRequest().getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete(); // Block request
        }

        // 3. EXAMINE THE PASS: Extract the "Bearer [token]"
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // 4. VALIDATE: Check the digital wax seal
                jwtUtil.validateToken(token);
            } catch (Exception e) {
                // If it's tampered with or expired, block it!
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 5. SUCCESS: Let the request pass to the inner microservices!
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}