package com.nexus.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @GetMapping("/fallback/product")
    public Mono<String> productFallback() {
        // factory method for static text strings
        return Mono.just("The Product Catalog is temporarily unavailable. Our engineering team has been notified. Please try again shortly.");
    }
}