package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.CartResponse;
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
        product1.setPrice(500);
        product1.setImageUrl("/img1.png");
        product1.setStock(10);

        product2 = new Product();
        product2.setProductId(2);
        product2.setName("商品2");
        product2.setPrice(800);
        product2.setImageUrl("/img2.png");
        product2.setStock(5);
    }

    @Test
    @DisplayName("セッションにカートが存在しない場合、新しい空のカートを作成してセッションに保存し、それを返す")
    void getCartFromSession_WhenCartNotExists_ShouldCreateNewCartAndSaveToSession() {
        CartResponse cart = cartService.getCartFromSession(session);

        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getTotalPrice()).isZero();
        assertThat(cart.getTotalQuantity()).isZero();
        assertThat(session.getAttribute("cart")).isSameAs(cart);
    }

    @Test
    @DisplayName("セッションにカートが存在する場合、既存のカートをそのまま返す")
    void getCartFromSession_WhenCartExists_ShouldReturnExistingCart() {
        Cart cart = new Cart();
        CartItem item = new CartItem("1", 1, "test", 100, "/img.png", 1, 100);
        cart.addItem(item);
        session.setAttribute("cart", cart);

        Cart result = cartService.getCartFromSession(session);

        assertThat(result).isSameAs(cart);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("有効な商品IDで新規に商品をカートに追加する")
    void addItemToCart_WhenProductExistsAndCartIsEmpty_ShouldAddToCartAndUpdateSession() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));

        Cart cart = cartService.addItemToCart(1, 2, session);

        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).hasSize(1);
        CartItem item = cart.getItems().get("1");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualTo(1000);
        verify(productRepository).findById(1);
    }

    @Test
    @DisplayName("既に存在する商品を追加すると数量と小計が加算される")
    void addItemToCart_WhenAddingExistingProduct_ShouldIncreaseQuantityAndUpdateTotals() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 1, session);

        Cart cart = cartService.addItemToCart(1, 3, session);

        CartItem item = cart.getItems().get("1");
        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getSubtotal()).isEqualTo(2000);
    }

    @Test
    @DisplayName("存在しない商品IDを指定するとnullが返る")
    void addItemToCart_WhenProductNotExists_ShouldReturnNullAndNotUpdateCart() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        Cart cart = cartService.addItemToCart(99, 1, session);

        assertThat(cart).isNull();
        assertThat(session.getAttribute("cart")).isNull();
    }

    @Test
    @DisplayName("productIdがnullの場合、nullが返る")
    void addItemToCart_WithNullProductId_ShouldReturnNull() {
        when(productRepository.findById(null)).thenReturn(Optional.empty());

        Cart cart = cartService.addItemToCart(null, 1, session);

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
        product1.setPrice(500);
        product1.setImageUrl("/img1.png");
        product1.setStock(10);

        product2 = new Product();
        product2.setProductId(2);
        product2.setName("商品2");
        product2.setPrice(800);
        product2.setImageUrl("/img2.png");
        product2.setStock(5);
    }

    @Test
    @DisplayName("セッションにカートが存在しない場合、新しい空のカートを作成してセッションに保存し、それを返す")
    void getCartFromSession_WhenCartNotExists_ShouldCreateNewCartAndSaveToSession() {
        CartResponse cart = cartService.getCartFromSession(session);

        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getTotalPrice()).isZero();
        assertThat(cart.getTotalQuantity()).isZero();
        assertThat(session.getAttribute("cart")).isSameAs(cart);
    }

    @Test
    @DisplayName("セッションにカートが存在する場合、既存のカートをそのまま返す")
    void getCartFromSession_WhenCartExists_ShouldReturnExistingCart() {
        Cart cart = new Cart();
        CartItem item = new CartItem("1", 1, "test", 100, "/img.png", 1, 100);
        cart.addItem(item);
        session.setAttribute("cart", cart);

        Cart result = cartService.getCartFromSession(session);

        assertThat(result).isSameAs(cart);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("数量更新：存在するitemIdの商品数量を正常に更新し、小計と合計も正しく再計算される")
    void updateItemQuantity_WhenItemExists_ShouldUpdateQuantityAndTotals() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2)).thenReturn(Optional.of(product2));
        cartService.addItemToCart(1, 2, session);
        cartService.addItemToCart(2, 1, session);

        Cart cart = cartService.updateItemQuantity("1", 5, session);

        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).hasSize(2);
        CartItem updatedItem = cart.getItems().get("1");
        assertThat(updatedItem.getQuantity()).isEqualTo(5);
        assertThat(updatedItem.getSubtotal()).isEqualTo(product1.getPrice() * 5);

        CartItem otherItem = cart.getItems().get("2");
        assertThat(otherItem.getQuantity()).isEqualTo(1);

        int expectedTotalQty = 5 + 1;
        int expectedTotalPrice = product1.getPrice() * 5 + product2.getPrice() * 1;
        assertThat(cart.getTotalQuantity()).isEqualTo(expectedTotalQty);
        assertThat(cart.getTotalPrice()).isEqualTo(expectedTotalPrice);
    }

    @Test
    @DisplayName("数量更新：存在しないitemIdの場合は例外をスローする")
    void updateItemQuantity_WhenItemIdNotExists_ShouldThrowException() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 1, session);

        assertThatThrownBy(() -> cartService.updateItemQuantity("99", 2, session))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("カートに商品が見つかりません");
    }

    @Test
    @DisplayName("数量更新：対象商品のProductが存在しない場合は例外をスローする")
    void updateItemQuantity_WhenProductNotExists_ShouldThrowException() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());
        cartService.addItemToCart(1, 1, session);

        assertThatThrownBy(() -> cartService.updateItemQuantity("1", 2, session))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("商品が見つかりません");
    }

    @Test
    @DisplayName("数量更新：更新数量が在庫を超える場合は例外をスローする")
    void updateItemQuantity_WhenQuantityExceedsStock_ShouldThrowException() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 1, session);

        int overStockQuantity = product1.getStock() + 1;
        assertThatThrownBy(() -> cartService.updateItemQuantity("1", overStockQuantity, session))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("在庫が足りません");
    }

    @Test
    @DisplayName("数量更新：数量に0を指定すると商品がカートから削除される")
    void updateItemQuantity_WithZeroOrLessQuantity_ShouldRemoveItem() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 3, session);

        Cart cart = cartService.updateItemQuantity("1", 0, session);

        assertThat(cart.getItems()).doesNotContainKey("1");
        assertThat(cart.getTotalQuantity()).isZero();
        assertThat(cart.getTotalPrice()).isZero();
    }

    @Test
    @DisplayName("数量更新：quantityがnullの場合はNullPointerExceptionが発生する")
    void updateItemQuantity_WithNullQuantity_ShouldThrowException() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 1, session);

        assertThatThrownBy(() -> cartService.updateItemQuantity("1", null, session))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("削除：存在するitemIdの商品のみ削除し合計再計算")
    void removeItemFromCart_WhenItemExists_ShouldRemoveItemAndRecalculateTotals() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2)).thenReturn(Optional.of(product2));
        cartService.addItemToCart(1, 2, session);
        cartService.addItemToCart(2, 1, session);

        Cart cart = cartService.removeItemFromCart("1", session);

        assertThat(cart.getItems()).doesNotContainKey("1");
        assertThat(cart.getItems()).containsKey("2");
        assertThat(cart.getTotalQuantity()).isEqualTo(1);
        assertThat(cart.getTotalPrice()).isEqualTo(product2.getPrice());
    }

    @Test
    @DisplayName("削除：存在しないitemIdではカートは変化しない")
    void removeItemFromCart_WhenItemNotExists_ShouldReturnCartUnchanged() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 1, session);

        Cart before = cartService.getCartFromSession(session);
        Cart after = cartService.removeItemFromCart("99", session);

        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("削除：itemIdがnullでも例外なくカートは変化しない")
    void removeItemFromCart_WithNullItemId_ShouldNotThrowAndCartUnchanged() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 1, session);

        Cart before = cartService.getCartFromSession(session);
        Cart after = cartService.removeItemFromCart(null, session);

        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("クリア：カートクリアでセッションからcart属性が削除される")
    void clearCart_ShouldRemoveCartAttributeFromSession() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product1));
        cartService.addItemToCart(1, 1, session);

        assertThat(session.getAttribute("cart")).isNotNull();
        cartService.clearCart(session);
        assertThat(session.getAttribute("cart")).isNull();
    }
} 
} 

