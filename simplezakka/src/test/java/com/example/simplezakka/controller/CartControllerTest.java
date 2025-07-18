package com.example.simplezakka.controller;

import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpSession session;

    private static final String CART_SESSION_KEY = "CART";

    @BeforeEach
    void setup() {
        // HttpSessionはMockMvcで個別に生成するので、
        // 各テストのリクエストでセッションを保持する形で対応
    }

    @Test
    @DisplayName("getCart_WhenCartExists_ShouldReturnCartWithStatusOk")
    void getCart_WhenCartExists_ShouldReturnCartWithStatusOk() throws Exception {
        CartRespons cartResponse = new CartRespons();
        // 適宜cartResponseのフィールドに値セット（省略）

        when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart")
                .sessionAttr(CART_SESSION_KEY, cartResponse))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // JSON構造に応じて検証。例:
                .andExpect(jsonPath("$.items", is(notNullValue())));
    }

    @Test
    @DisplayName("getCart_WhenCartNotExists_ShouldReturnEmptyCartWithStatusOk")
    void getCart_WhenCartNotExists_ShouldReturnEmptyCartWithStatusOk() throws Exception {
        CartRespons emptyCart = new CartRespons();
        // 空のカートを想定

        when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(emptyCart);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("getCart_WhenServiceThrowsException_ShouldReturnInternalServerError")
    void getCart_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        when(cartService.getCartFromSession(any(HttpSession.class))).thenThrow(new RuntimeException("Service failure"));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("addItem_WithValidData_ShouldReturnUpdatedCartWithStatusOk")
    void addItem_WithValidData_ShouldReturnUpdatedCartWithStatusOk() throws Exception {
        CartRespons updatedCart = new CartRespons();
        // updatedCartの必要情報セット（省略）

        when(cartService.addItemToCart( anyLong(), anyInt(),any(HttpSession.class))).thenReturn(updatedCart);

        mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", is(notNullValue())));
    }

    @Test
    @DisplayName("addItem_WhenServiceReturnsNull_ShouldReturnNotFound")
    void addItem_WhenServiceReturnsNull_ShouldReturnNotFound() throws Exception {
        when(cartService.addItemToCart(anyLong(), anyInt(),any(HttpSession.class))).thenReturn(null);

        mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":999,\"quantity\":2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("addItem_WithNullProductId_ShouldReturnBadRequest")
    void addItem_WithNullProductId_ShouldReturnBadRequest() throws Exception {
        String requestBody = "{\"quantity\":2}";

        mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("addItem_WithZeroQuantity_ShouldReturnBadRequest")
    void addItem_WithZeroQuantity_ShouldReturnBadRequest() throws Exception {
        String requestBody = "{\"productId\":1,\"quantity\":0}";

        mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateItem_WithValidData_ShouldReturnUpdatedCartWithStatusOk")
    void updateItem_WithValidData_ShouldReturnUpdatedCartWithStatusOk() throws Exception {
        CartRespons updatedCart = new CartRespons();

        when(cartService.addItemToCart(any(Long.class), any(Integer.class), any(HttpSession.class))).thenReturn(updatedCart);

        mockMvc.perform(put("/api/cart/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", is(notNullValue())));
    }

    @Test
    @DisplayName("updateItem_WithZeroQuantity_ShouldReturnBadRequest")
    void updateItem_WithZeroQuantity_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/cart/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("removeItem_WhenItemExists_ShouldReturnUpdatedCartWithStatusOk")
    void removeItem_WhenItemExists_ShouldReturnUpdatedCartWithStatusOk() throws Exception {
        CartRespons updatedCart = new CartRespons();

        when(cartService.removeItemFromCart(any(String.class), any(HttpSession.class))).thenReturn(updatedCart);

        mockMvc.perform(delete("/api/cart/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", is(notNullValue())));
    }

    @Test
    @DisplayName("removeItem_WhenItemNotExists_ShouldReturnCartFromServiceWithStatusOk")
    void removeItem_WhenItemNotExists_ShouldReturnCartFromServiceWithStatusOk() throws Exception {
        CartRespons currentCart = new CartRespons();

        when(cartService.removeItemFromCart(any(String.class), any(HttpSession.class))).thenReturn(currentCart);

        mockMvc.perform(delete("/api/cart/items/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", is(notNullValue())));
    }
}
