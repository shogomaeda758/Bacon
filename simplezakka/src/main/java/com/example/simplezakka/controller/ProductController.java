package com.example.simplezakka.controller;

import com.example.simplezakka.dto.product.ProductDetail;
import com.example.simplezakka.dto.product.ProductListItem;
import com.example.simplezakka.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;


import java.util.List;

/**
 * 商品取得REST APIコントローラー
 * 商品の検索、取得機能のエンドポイントを提供
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    /**
     * 全商品取得
     * GET /api/products
     * @return 全商品リスト
     */
    @GetMapping
    public ResponseEntity<List<ProductListItem>> getAllProducts() {
        List<ProductListItem> products = productService.findAllProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * 商品詳細取得
     * GET /api/products/{productId}
     * @param productId 商品ID
     * @return 商品詳細情報
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetail> getProductById(@PathVariable Integer productId) {
        ProductDetail product = productService.findProductById(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }
    
    /**
     * カテゴリ別商品取得
     * GET /api/products/category/{categoryId}
     * @param categoryId カテゴリID
     * @return カテゴリに属する商品リスト
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductListItem>> getProductsByCategory(@PathVariable Integer categoryId) {
        List<ProductListItem> products = productService.findProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }
    /**
     * 商品検索
     * GET /api/products/search?keyword={keyword}
     * @param keyword 検索キーワード
     * @return 検索結果商品リスト
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductListItem>> searchProducts(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductListItem> products = productService.searchProducts(keyword.trim());
        return ResponseEntity.ok(products);
    }
}