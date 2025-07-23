package com.example.simplezakka.service;

import com.example.simplezakka.dto.product.ProductDetail;
import com.example.simplezakka.dto.product.ProductListItem;
import com.example.simplezakka.entity.Category;
import com.example.simplezakka.entity.Product;
import com.example.simplezakka.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product1;
    private Product product2;
    private Product productWithNullFields;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setCategoryId(1);
        category.setCategoryName("家電");

        product1 = new Product();
        product1.setProductId(1);
        product1.setName("商品1");
        product1.setPrice(BigDecimal.valueOf(100));
        product1.setImageUrl("/img1.png");
        product1.setDescription("説明1");
        product1.setStock(10);
        product1.setCategory(category);

        product2 = new Product();
        product2.setProductId(2);
        product2.setName("商品2");
        product2.setPrice(BigDecimal.valueOf(200));
        product2.setImageUrl("/img2.png");
        product2.setDescription("説明2");
        product2.setStock(5);
        product2.setCategory(category);

        productWithNullFields = new Product();
        productWithNullFields.setProductId(3);
        productWithNullFields.setName("商品3（Nullあり）");
        productWithNullFields.setPrice(BigDecimal.valueOf(300));
        productWithNullFields.setStock(8);
        productWithNullFields.setDescription(null);
        productWithNullFields.setImageUrl(null);
        productWithNullFields.setCategory(category);
    }

    // === findAllProducts ===

    @Test
    @DisplayName("商品が2件以上登録されている場合、商品の一覧(List)が返る")
    void findAllProducts_ReturnsProductList() {
        when(productRepository.findAll()).thenReturn(Arrays.asList(product1, product2));

        List<ProductListItem> result = productService.findAllProducts();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ProductListItem::getProductId, ProductListItem::getName, ProductListItem::getPrice, ProductListItem::getImageUrl, ProductListItem::getCategoryName)
                .containsExactlyInAnyOrder(
                        tuple(product1.getProductId(), product1.getName(), product1.getPrice().intValue(), product1.getImageUrl(), category.getCategoryName()),
                        tuple(product2.getProductId(), product2.getName(), product2.getPrice().intValue(), product2.getImageUrl(), category.getCategoryName())
                );

        verify(productRepository, times(1)).findAll();
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("商品が1件も登録されていない場合、空のリストが返る")
    void findAllProducts_ReturnsEmptyList() {
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        List<ProductListItem> result = productService.findAllProducts();

        assertThat(result).isEmpty();

        verify(productRepository, times(1)).findAll();
        verifyNoMoreInteractions(productRepository);
    }

    // === findProductById ===

    @Test
    @DisplayName("指定IDの商品が存在する場合、商品詳細(ProductDetail)が返る")
    void findProductById_ExistingProduct_ReturnsDetail() {
        Integer productId = 1;
        when(productRepository.findById(productId)).thenReturn(Optional.of(product1));

        ProductDetail result = productService.findProductById(productId);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(product1.getProductId());
        assertThat(result.getName()).isEqualTo(product1.getName());
        assertThat(result.getPrice()).isEqualTo(product1.getPrice().intValue());
        assertThat(result.getDescription()).isEqualTo(product1.getDescription());
        assertThat(result.getStock()).isEqualTo(product1.getStock());
        assertThat(result.getImageUrl()).isEqualTo(product1.getImageUrl());

        verify(productRepository, times(1)).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("指定IDの商品が存在しない場合、nullが返る")
    void findProductById_NonExistingProduct_ReturnsNull() {
        Integer productId = 99;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        ProductDetail result = productService.findProductById(productId);

        assertThat(result).isNull();

        verify(productRepository, times(1)).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("商品説明と画像がnullで登録されている場合、商品詳細のdescription, imageUrlがnullのまま返る")
    void findProductById_WithNullFields_ReturnsPartial() {
        Integer productId = 3;
        when(productRepository.findById(productId)).thenReturn(Optional.of(productWithNullFields));

        ProductDetail result = productService.findProductById(productId);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(productWithNullFields.getProductId());
        assertThat(result.getName()).isEqualTo(productWithNullFields.getName());
        assertThat(result.getPrice()).isEqualTo(productWithNullFields.getPrice().intValue());
        assertThat(result.getDescription()).isNull();
        assertThat(result.getStock()).isEqualTo(productWithNullFields.getStock());
        assertThat(result.getImageUrl()).isNull();

        verify(productRepository, times(1)).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }
}