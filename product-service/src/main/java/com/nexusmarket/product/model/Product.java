package com.nexusmarket.product.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;//jakarta docs: https://jakarta.ee/learn/docs/jakartaee-tutorial/current/persist/persistence-intro/persistence-intro.html
import lombok.AllArgsConstructor;//Lombok docs: https://www.geeksforgeeks.org/java/introduction-to-project-lombok-in-java-and-how-to-get-started/
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @NotBlank: Java check (no empty strings)
    // @Column: DB check (NOT NULL and UNIQUE constraint)
    @NotBlank(message = "Product name is mandatory")
    @Column(nullable = false, unique = true)
    private String name;

    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;
}