package com.example.simplezakka.controller;

import com.example.simplezakka.entity.Category;
import com.example.simplezakka.entity.Product;
import com.example.simplezakka.repository.CategoryRepository;
import com.example.simplezakka.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.LocalDateTime; // 追加

public class ProductIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setCreatedAt(LocalDateTime.now());
        testCategory.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(testCategory);

        Product product = new Product();
        product.setName("あ".repeat(255));
        product.setPrice(new BigDecimal("-100.00"));
        product.setStock(10);
        product.setCategory(testCategory);
        product.setDescription(null);
        product.setImageUrl("broken_url");
        product.setIsRecommended(false);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        Product zeroPriceProduct = new Product();
        zeroPriceProduct.setName("Zero Price Product");
        zeroPriceProduct.setPrice(BigDecimal.ZERO);
        zeroPriceProduct.setStock(5);
        zeroPriceProduct.setCategory(testCategory);
        zeroPriceProduct.setDescription("Zero price description");
        zeroPriceProduct.setImageUrl("valid_url");
        zeroPriceProduct.setIsRecommended(false);
        zeroPriceProduct.setCreatedAt(LocalDateTime.now());
        zeroPriceProduct.setUpdatedAt(LocalDateTime.now());
        productRepository.save(zeroPriceProduct);
    }
}