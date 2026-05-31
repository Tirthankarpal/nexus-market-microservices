package com.nexus.apigateway.filter;

import com.nexus.apigateway.rate.TokenBucket;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global rate-limiting filter configured to mitigate brute-force attempts
 * on authentication endpoints using the Token Bucket algorithm.
 */
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final Map<String, TokenBucket> limiters = new ConcurrentHashMap<>();

    private static final long BUCKET_CAPACITY = 5;
    private static final long REFILL_TOKENS = 1;
    private static final long REFILL_PERIOD_MS = 12000; // 1 token every 12 seconds (5 requests/minute capacity)

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.startsWith("/auth/login") || path.startsWith("/auth/register")) {
            String clientIp = getClientIp(exchange.getRequest());
            TokenBucket bucket = limiters.computeIfAbsent(clientIp, 
                ip -> new TokenBucket(BUCKET_CAPACITY, REFILL_TOKENS, REFILL_PERIOD_MS));

            if (!bucket.tryConsume()) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                
                String errorBody = "{\"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Too many login attempts. Please try again later.\"}";
                byte[] bytes = errorBody.getBytes();
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            }
        }

        return chain.filter(exchange);
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown-ip";
    }

    @Override
    public int getOrder() {
        return -2; // Runs before authentication filters to reject early
    }
}
