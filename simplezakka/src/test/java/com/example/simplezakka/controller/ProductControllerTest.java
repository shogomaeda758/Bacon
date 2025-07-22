package com.example.simplezakka.controller;

import com.example.simplezakka.dto.product.ProductDetail;
import com.example.simplezakka.dto.product.ProductListItem;
import com.example.simplezakka.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    private ProductListItem productListItem1;
    private ProductListItem productListItem2;
    private ProductDetail productDetail1;
    private ProductDetail productDetailWithNulls;

    @BeforeEach
    void setUp() {
        // categoryNameも含めてモックデータ作成
        productListItem1 = new ProductListItem(1, "リスト商品1", 100, "/list1.png", "カテゴリA");
        productListItem2 = new ProductListItem(2, "リスト商品2", 200, "/list2.png", "カテゴリB");

        productDetail1 = new ProductDetail(1, "詳細商品1", 100, "詳細説明1", 10, "/detail1.png");
        productDetailWithNulls = new ProductDetail(3, "詳細商品3", 300, null, 5, null);

        lenient().when(productService.findAllProducts())
                 .thenReturn(Arrays.asList(productListItem1, productListItem2));
        lenient().when(productService.findProductById(1))
                 .thenReturn(productDetail1);
        lenient().when(productService.findProductById(3))
                 .thenReturn(productDetailWithNulls);
        lenient().when(productService.findProductById(99))
                 .thenReturn(null);
    }

    @Nested
    @DisplayName("GET /api/products")
    class GetAllProductsTests {
        @Test
        @DisplayName("商品ありの場合、200 OK + 商品リストを返す")
        void getAllProducts_WhenProductsExist_ReturnsList() throws Exception {
            mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$", hasSize(2)))
                   .andExpect(jsonPath("$[0].productId", is(productListItem1.getProductId())))
                   .andExpect(jsonPath("$[0].name", is(productListItem1.getName())))
                   .andExpect(jsonPath("$[0].price", is(productListItem1.getPrice())))
                   .andExpect(jsonPath("$[0].imageUrl", is(productListItem1.getImageUrl())))
                   .andExpect(jsonPath("$[0].categoryName", is(productListItem1.getCategoryName())))
                   .andExpect(jsonPath("$[1].productId", is(productListItem2.getProductId())))
                   .andExpect(jsonPath("$[1].name", is(productListItem2.getName())))
                   .andExpect(jsonPath("$[1].price", is(productListItem2.getPrice())))
                   .andExpect(jsonPath("$[1].imageUrl", is(productListItem2.getImageUrl())))
                   .andExpect(jsonPath("$[1].categoryName", is(productListItem2.getCategoryName())));

            verify(productService, times(1)).findAllProducts();
            verifyNoMoreInteractions(productService);
        }

        @Test
        @DisplayName("商品なしの場合、200 OK + 空リストを返す")
        void getAllProducts_WhenNoProductsExist_ReturnsEmptyList() throws Exception {
            when(productService.findAllProducts()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$", hasSize(0)));

            verify(productService, times(1)).findAllProducts();
            verifyNoMoreInteractions(productService);
        }
    }

    // （以下、以前提示したテストコードのまま変更なし。詳細取得などは影響なし）
}
