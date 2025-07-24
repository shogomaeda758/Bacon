package com.example.simplezakka.controller;

import com.example.simplezakka.entity.Category;
import com.example.simplezakka.entity.Product;
import com.example.simplezakka.repository.CategoryRepository;
import com.example.simplezakka.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        categoryRepository.save(testCategory);
    }

    @Nested
    @DisplayName("商品一覧取得API (/api/products)")
    class ProductListTests {

        @Test
        @DisplayName("1: 商品一覧表示が成功し、正しい形式で返る")
        void testGetProductList() throws Exception {
            // テストデータ作成
            Product product = new Product();
            product.setName("Test Product");
            product.setPrice(new BigDecimal("1000.00"));
            product.setStock(10);
            product.setCategory(testCategory);
            productRepository.save(product);

            mockMvc.perform(get("/api/products")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Test Product")));
        }

        @Test
        @DisplayName("8: 商品データが空の場合、空配列が返る")
        void testEmptyProductList() throws Exception {
            mockMvc.perform(get("/api/products")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("商品詳細取得API (/api/products/{id})")
    class ProductDetailTests {

        @Test
        @DisplayName("2: 商品詳細表示が成功し、ProductDetail形式で返る")
        void testGetProductDetail() throws Exception {
            // テストデータ作成
            Product product = new Product();
            product.setName("Detail Product");
            product.setPrice(new BigDecimal("2000.00"));
            product.setStock(5);
            product.setCategory(testCategory);
            product.setDescription("A detailed product");
            productRepository.save(product);

            Integer productId = product.getProductId();

            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.name", is("Detail Product")))
                    .andExpect(jsonPath("$.description", is("A detailed product")));
        }

        @Test
        @DisplayName("6: 存在しない商品IDを指定した場合は404が返る")
        void testProductDetailNotFound() throws Exception {
            mockMvc.perform(get("/api/products/99999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("7: 不正な形式のID（文字列）指定で500エラー")
        void testInvalidProductIdFormat() throws Exception {
            mockMvc.perform(get("/api/products/abc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("9: 商品の一部フィールドがnullでもエラーにならず返る")
        void testProductDetailWithNullFields() throws Exception {
            // nullフィールドを含むテストデータ作成
            Product product = new Product();
            product.setName("Null Field Product");
            product.setPrice(new BigDecimal("1500.00"));
            product.setStock(3);
            product.setCategory(testCategory);
            // descriptionをnullのままにする
            product.setDescription(null);
            productRepository.save(product);

            Integer productId = product.getProductId();

            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Null Field Product")))
                    .andExpect(jsonPath("$.description").doesNotExist());
        }

        @Test
        @DisplayName("13: 異常価格（0円、負数）の商品は特別な表示になる")
        void testInvalidPriceProduct() throws Exception {
            // 0円商品のテストデータ作成
            Product zeroProduct = new Product();
            zeroProduct.setName("Free Product");
            zeroProduct.setPrice(BigDecimal.ZERO);
            zeroProduct.setStock(5);
            zeroProduct.setCategory(testCategory);
            productRepository.save(zeroProduct);

            Integer productId = zeroProduct.getProductId();

            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price", is(0)));
        }

        @Test
        @DisplayName("13-2: 負数価格の商品も正しく処理される")
        void testNegativePriceProduct() throws Exception {
            // 負数価格商品のテストデータ作成
            Product negativeProduct = new Product();
            negativeProduct.setName("Negative Price Product");
            negativeProduct.setPrice(new BigDecimal("-100.00"));
            negativeProduct.setStock(1);
            negativeProduct.setCategory(testCategory);
            productRepository.save(negativeProduct);

            Integer productId = negativeProduct.getProductId();

            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price", is(-100)));
        }
    }

    @Nested
    @DisplayName("境界値・エラーハンドリングテスト")
    class EdgeCaseTests {

        @Test
        @DisplayName("在庫が0の商品も正しく取得できる")
        void testZeroStockProduct() throws Exception {
            Product product = new Product();
            product.setName("Out of Stock Product");
            product.setPrice(new BigDecimal("2000.00"));
            product.setStock(0);
            product.setCategory(testCategory);
            productRepository.save(product);

            Integer productId = product.getProductId();

            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock", is(0)));
        }

        @Test
        @DisplayName("商品名が非常に長い場合も正しく取得できる")
        void testLongProductName() throws Exception {
            String longName = "あ".repeat(255); // 255文字の長い商品名
            
            Product product = new Product();
            product.setName(longName);
            product.setPrice(new BigDecimal("1000.00"));
            product.setStock(10);
            product.setCategory(testCategory);
            productRepository.save(product);

            Integer productId = product.getProductId();

            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is(longName)));
        }
    }
}
