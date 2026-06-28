package com.nexusmarket.product.service;

import com.nexusmarket.product.model.Product;
import com.nexusmarket.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;

import com.nexusmarket.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public org.springframework.data.domain.Page<Product> getAllProducts(org.springframework.data.domain.Pageable pageable) {
        return productRepository.findAll(pageable);
    }
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }
    public Product saveProduct(Product product) {
        // This is where you would add business rules, like:
        // if (product.getPrice() < 0) throw new InvalidPriceException();
        return productRepository.save(product);
    }
    public Product updateProduct(Long id, Product updatedProduct) {
        Product existingProduct = getProductById(id); // Reuses our 404 logic!

        existingProduct.setName(updatedProduct.getName());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setStockQuantity(updatedProduct.getStockQuantity());

        return productRepository.save(existingProduct);
    }
    public void deleteProduct(Long id) {
        Product product = getProductById(id); // If not found, this throws the 404 error
        productRepository.delete(product);
    }
}