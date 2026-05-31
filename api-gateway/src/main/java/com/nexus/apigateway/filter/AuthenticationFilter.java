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
        String method = exchange.getRequest().getMethod().name();

        // Bypass authentication for public auth, actuator, and GET products catalog endpoints
        if (path.startsWith("/auth/") || path.startsWith("/actuator/") || 
            ((path.startsWith("/api/v1/products") || path.equals("/api/v1/products")) && "GET".equalsIgnoreCase(method))) {
            return chain.filter(exchange);
        }

        // Verify the presence of the Authorization header
        if (!exchange.getRequest().getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Validate JWT signature and expiration
                jwtUtil.validateToken(token);

                // Extract principal and role from JWT claims
                io.jsonwebtoken.Claims claims = jwtUtil.getClaims(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                // Enforce ADMIN role on write operations to product or inventory paths
                if ((path.startsWith("/api/v1/products") || path.startsWith("/api/v1/inventory"))
                        && (method.equals("POST") || method.equals("PUT") || method.equals("DELETE"))) {
                    if (role == null || !role.equalsIgnoreCase("ADMIN")) {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                }

                // Propagate authenticated user context downstream
                org.springframework.http.server.reactive.ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-Authenticated-User", username)
                        .header("X-Authenticated-Role", role != null ? role : "USER")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}