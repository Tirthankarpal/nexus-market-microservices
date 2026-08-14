package com.nexusmarket.product.repository;

import com.nexusmarket.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The Repository Layer:
 * Product -> The entity this repository manages.
 * Long    -> The type of the Primary Key (@Id) in the Product entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

}