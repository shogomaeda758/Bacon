package com.example.simplezakka.service;


import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.entity.Product;
import com.example.simplezakka.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.util.Optional;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CartServiceTest {


    @Mock
    private ProductRepository productRepository;


    @InjectMocks
    private CartService cartService;


    private HttpSession session;
    private Product product1;
    private Product product2;


    @BeforeEach
    void setUp() {
        session = new MockHttpSession();


        product1 = new Product();
        product1.setProductId(1);
        product1.setName("商品1");
        product1.setPrice(BigDecimal.valueOf(500));
        product1.setImageUrl("/img1.png");
        product1.setStock(10);


        product2 = new Product();
        product2.setProductId(2);
        product2.setName("商品2");
        product2.setPrice(BigDecimal.valueOf(800));
        product2.setImageUrl("/img2.png");
        product2.setStock(5);
    }


    @Test
    @DisplayName("セッションにカートが存在しない場合、新しい空のカートを作成してセッションに保存し、それを返す")
    void getCartFromSession_WhenCartNotExists_ShouldCreateNewCartAndSaveToSession() {
        CartRespons cart = cartService.getCartFromSession(session);


        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getTotalPrice()).isZero();
        assertThat(cart.getTotalQuantity()).isZero();
        assertThat(session.getAttribute("cart")).isSameAs(cart);
    }


    @Test
@DisplayName("セッションにカートが存在する場合、既存のカートをそのまま返す")
void getCartFromSession_WhenCartExists_ShouldReturnExistingCart() {
    CartRespons cart = new CartRespons();

    CartItemResponse item = new CartItemResponse();
    item.setId("1");
    item.setProductId(1);
    item.setName("test");
    item.setPrice(BigDecimal.valueOf(100));
    item.setImageUrl("/img.png");
    item.setQuantity(1);
    item.setSubtotal(BigDecimal.valueOf(100));

    cart.getItems().put("1", item);
    cart.calculateTotals(); 

    session.setAttribute("cart", cart);

    CartRespons result = cartService.getCartFromSession(session);

    assertThat(result).isSameAs(cart);
    assertThat(result.getItems()).hasSize(1);
}



    @Test
    @DisplayName("有効な商品IDで新規に商品をカートに追加する")
    void addItemToCart_WhenProductExistsAndCartIsEmpty_ShouldAddToCartAndUpdateSession() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));


        CartRespons cart = cartService.addItemToCart(1, 2, session);



        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).hasSize(1);
        CartItemResponse item = cart.getItems().get("1");

        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(productRepository).findById(1);
    }


    @Test
    @DisplayName("既に存在する商品を追加すると数量と小計が加算される")
    void addItemToCart_WhenAddingExistingProduct_ShouldIncreaseQuantityAndUpdateTotals() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 1, session);


        CartRespons cart = cartService.addItemToCart(1, 3, session);


        CartItemResponse item = cart.getItems().get("1");
        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2000));    }


    @Test
    @DisplayName("存在しない商品IDを指定するとnullが返る")
    void addItemToCart_WhenProductNotExists_ShouldReturnNullAndNotUpdateCart() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());


        CartRespons cart = cartService.addItemToCart(99, 1, session);


        assertThat(cart).isNull();
        assertThat(session.getAttribute("cart")).isNull();
    }


    @Test
    @DisplayName("productIdがnullの場合、nullが返る")
    void addItemToCart_WithNullProductId_ShouldReturnNull() {
        when(productRepository.findById(null)).thenReturn(Optional.empty());


        CartRespons cart = cartService.addItemToCart(null, 1, session);


        assertThat(cart).isNull();
        verify(productRepository).findById(null);
    }


    @Test
    @DisplayName("quantityがnullの場合、NullPointerExceptionが発生")
    void addItemToCart_WithNullQuantity_ShouldThrowException() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));


        assertThatThrownBy(() -> cartService.addItemToCart(1, null, session))
            .isInstanceOf(NullPointerException.class);
    }


    @Test
    @DisplayName("quantityが0以下の場合、IllegalArgumentExceptionが発生")
    void addItemToCart_WithZeroOrNegativeQuantity_ShouldThrowException() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));


        assertThatThrownBy(() -> cartService.addItemToCart(1, 0, session))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cartService.addItemToCart(1, -1, session))
            .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    @DisplayName("追加後の数量が在庫を超えるとIllegalArgumentExceptionが発生")
    void addItemToCart_WhenQuantityExceedsStock_ShouldThrowException() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 9, session);


        assertThatThrownBy(() -> cartService.addItemToCart(1, 2, session))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("在庫が足りません");
    }
}

  