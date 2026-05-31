package com.nexusmarket.product.controller;
import com.nexusmarket.product.model.Product;
import com.nexusmarket.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Tells Spring this class handles REST API requests
@RequestMapping("/api/v1/products") // The base URL for all endpoints in this class
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    @GetMapping // Handles GET requests to /api/v1/products
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }
    @PostMapping // Handles POST requests to /api/v1/products to create a new product
    public Product createProduct(@Valid @RequestBody Product product) {
        return productService.saveProduct(product);
    }
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build(); // Returns a 204 No Content status
    }
}