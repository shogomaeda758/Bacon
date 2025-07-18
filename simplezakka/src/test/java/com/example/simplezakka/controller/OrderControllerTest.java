package com.example.simplezakka.controller;

import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.order.CustomerInfo;
import com.example.simplezakka.dto.order.OrderItemDetailResponse;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.service.CartService;
import com.example.simplezakka.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; 

    @MockBean
    private OrderService orderService;

    @MockBean
    private CartService cartService;

    private MockHttpSession mockSession;
    private CartRespons cartWithItems;
    private CartRespons emptyCart;
    private OrderRequest validOrderRequest;
    private CustomerInfo validCustomerInfo; 
    private OrderResponse sampleOrderResponse;
    private OrderItemDetailResponse sampleOrderItemDetailResponse;

    @BeforeEach
    void setUp() {
        mockSession = new MockHttpSession();
        mockSession.setAttribute("customerId", 1); 

        
        cartWithItems = new CartRespons();
        CartItemResponse item = new CartItemResponse(
                "p001", 1, "商品A", BigDecimal.valueOf(1000), "image_url", 1, BigDecimal.valueOf(1000)
        );
        cartWithItems.addItem(item);

        emptyCart = new CartRespons();

        
        validCustomerInfo = new CustomerInfo(
                1, "山田 太郎", "yamada@example.com", "東京都渋谷区1-1-1", "09012345678"
        );

        
        validOrderRequest = new OrderRequest();
        validOrderRequest.setCustomerInfo(validCustomerInfo);
        validOrderRequest.setPaymentMethod("クレジットカード");

        
        sampleOrderItemDetailResponse = new OrderItemDetailResponse(
                1, "商品A", "http://example.com/product_a.jpg", 1, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000)
        );

        
        sampleOrderResponse = new OrderResponse(
                123, 
                LocalDateTime.now(), 
                BigDecimal.valueOf(1000), 
                BigDecimal.valueOf(500), 
                BigDecimal.valueOf(1500), 
                "クレジットカード", 
                "PENDING", 
                Collections.singletonList(sampleOrderItemDetailResponse), 
                validCustomerInfo, 
                "注文が正常に完了しました。" 
        );

        
        
        lenient().when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(cartWithItems);
        lenient().when(orderService.placeOrder(any(CartRespons.class), any(OrderRequest.class), any(HttpSession.class)))
                .thenReturn(sampleOrderResponse);
    }


    @Nested
    @DisplayName("POST /api/order/confirm - 注文確定APIのテスト")
    class PlaceOrderTests {

        @Test
@DisplayName("【正常系】有効なリクエストとカートで注文確定が成功し、201 Createdを返す")
void placeOrder_WithValidRequestAndCart_ShouldReturnCreated() throws Exception {
    

    mockMvc.perform(post("/api/order/confirm")
                    .session(mockSession) 
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validOrderRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.orderId", is(sampleOrderResponse.getOrderId())))
            .andExpect(jsonPath("$.orderDate", is(notNullValue())))
            .andExpect(jsonPath("$.message", is(sampleOrderResponse.getMessage())))
            .andExpect(jsonPath("$.totalPrice", is(sampleOrderResponse.getTotalPrice().intValue()))) 
            .andExpect(jsonPath("$.shippingFee", is(sampleOrderResponse.getShippingFee().intValue()))) 
            .andExpect(jsonPath("$.grandTotal", is(sampleOrderResponse.getGrandTotal().intValue()))) 
            .andExpect(jsonPath("$.paymentMethod", is(sampleOrderResponse.getPaymentMethod())))
            .andExpect(jsonPath("$.status", is(sampleOrderResponse.getStatus())))
            .andExpect(jsonPath("$.customerInfo.name", is(validCustomerInfo.getName())))
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].productId", is(sampleOrderItemDetailResponse.getProductId())));

    
    verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
    verify(orderService, times(1)).placeOrder(eq(cartWithItems), eq(validOrderRequest), any(HttpSession.class));
    verifyNoMoreInteractions(cartService, orderService); 
}

        @Nested
        @DisplayName("事前条件チェック")
        class PreconditionChecks {

            @Test
            @DisplayName("【異常系】カートが空の場合、400 Bad Requestとエラーメッセージを返す")
            void placeOrder_WithEmptyCart_ShouldReturnBadRequest() throws Exception {
                
                when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(emptyCart);

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOrderRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message", is("カートが空か無効です。注文を確定できません。")));

                
                verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
                verifyNoInteractions(orderService);
                verifyNoMoreInteractions(cartService);
            }

            @Test
            @DisplayName("【異常系】カートがnullの場合、400 Bad Requestとエラーメッセージを返す")
            void placeOrder_WithNullCart_ShouldReturnBadRequest() throws Exception {
                
                when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(null);

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOrderRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message", is("カートが空か無効です。注文を確定できません。")));

                
                verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
                verifyNoInteractions(orderService);
                verifyNoMoreInteractions(cartService);
            }
        }



        @Nested
        @DisplayName("入力バリデーションエラー")
        class InputValidationErrors {

            @Test
@DisplayName("【異常系】顧客情報がnullの場合、400 Bad Requestとエラーメッセージを返す")
void placeOrder_WithNullCustomerInfo_ShouldReturnBadRequest() throws Exception {
    OrderRequest invalidRequest = new OrderRequest();
    invalidRequest.setCustomerInfo(null); 
    invalidRequest.setPaymentMethod("クレジットカード");

    mockMvc.perform(post("/api/order/confirm")
                    .session(mockSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            
            .andExpect(jsonPath("$.message", is("顧客情報は必須です。"))); 

    
    verifyNoInteractions(cartService, orderService);
}
            @Test
            @DisplayName("【異常系】氏名が空文字列の場合、400 Bad Requestとエラーメッセージを返す")
            void placeOrder_WithBlankName_ShouldReturnBadRequest() throws Exception {
                CustomerInfo invalidCustomerInfo = new CustomerInfo(
                        1, "", "test@example.com", "東京都", "09012345678"
                );
                OrderRequest invalidRequest = new OrderRequest();
                invalidRequest.setCustomerInfo(invalidCustomerInfo);
                invalidRequest.setPaymentMethod("クレジットカード");

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message", is("氏名は必須です。"))); 

                verifyNoInteractions(cartService, orderService);
            }

            @Test
            @DisplayName("【異常系】メールアドレスの形式が不正な場合、400 Bad Requestとエラーメッセージを返す")
            void placeOrder_WithInvalidEmailFormat_ShouldReturnBadRequest() throws Exception {
                CustomerInfo invalidCustomerInfo = new CustomerInfo(
                        1, "テスト太郎", "invalid-email", "東京都", "09012345678"
                );
                OrderRequest invalidRequest = new OrderRequest();
                invalidRequest.setCustomerInfo(invalidCustomerInfo);
                invalidRequest.setPaymentMethod("クレジットカード");

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message", is("有効なメールアドレス形式で入力してください。"))); 

                verifyNoInteractions(cartService, orderService);
            }

            @Test
            @DisplayName("【異常系】住所が空文字列の場合、400 Bad Requestとエラーメッセージを返す")
            void placeOrder_WithBlankAddress_ShouldReturnBadRequest() throws Exception {
                CustomerInfo invalidCustomerInfo = new CustomerInfo(
                        1, "テスト太郎", "test@example.com", "", "09012345678"
                );
                OrderRequest invalidRequest = new OrderRequest();
                invalidRequest.setCustomerInfo(invalidCustomerInfo);
                invalidRequest.setPaymentMethod("クレジットカード");

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message", is("住所は必須です。"))); 

                verifyNoInteractions(cartService, orderService);
            }

            

@Test
@DisplayName("【異常系】電話番号が空文字列の場合、400 Bad Requestとエラーメッセージを返す")
void placeOrder_WithBlankPhoneNumber_ShouldReturnBadRequest() throws Exception {
    CustomerInfo invalidCustomerInfo = new CustomerInfo(
            1, "テスト太郎", "test@example.com", "東京都", "" 
    );
    OrderRequest invalidRequest = new OrderRequest();
    invalidRequest.setCustomerInfo(invalidCustomerInfo);
    invalidRequest.setPaymentMethod("クレジットカード");

    mockMvc.perform(post("/api/order/confirm")
                    .session(mockSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            
            .andExpect(jsonPath("$.message", allOf(
                    containsString("電話番号は必須です。"),
                    containsString("電話番号は10桁または11桁の数字で入力してください。")
            )));

    verifyNoInteractions(cartService, orderService);
}



            @Test
            @DisplayName("【異常系】複数のバリデーションエラーが発生した場合、400 Bad Requestと連結されたエラーメッセージを返す")
            void placeOrder_WithMultipleValidationErrors_ReturnsBadRequest() throws Exception {
                CustomerInfo invalidCustomerInfo = new CustomerInfo(
                        null, 
                        "", 
                        "invalid-email", 
                        "", 
                        "123" 
                );
                OrderRequest invalidRequest = new OrderRequest();
                invalidRequest.setCustomerInfo(invalidCustomerInfo);
                invalidRequest.setPaymentMethod(""); 

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message", allOf(
                                containsString("氏名は必須です。"),
                                containsString("有効なメールアドレス形式で入力してください。"),
                                containsString("住所は必須です。"),
                                containsString("電話番号は10桁または11桁の数字で入力してください。"),
                                containsString("支払い方法は必須です。")
                        )));
                
                
                

                verifyNoInteractions(cartService, orderService);
            }

            @Test
@DisplayName("【異常系】JSON構文が不正な場合、500 Internal Server Errorと汎用エラーメッセージを返す") 
void placeOrder_WithInvalidJsonSyntax_ReturnsInternalServerError() throws Exception { 
    String invalidJson = "{\"customerInfo\": \"invalid\","; 

    mockMvc.perform(post("/api/order/confirm")
                    .session(mockSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
            
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            
            .andExpect(jsonPath("$.message", is("注文確定中に予期せぬエラーが発生しました。"))); 

    verifyNoInteractions(cartService, orderService);
}
        }



        @Nested
        @DisplayName("OrderServiceからの例外ハンドリング")
        class OrderServiceExceptionHandling {

            @Test
            @DisplayName("【異常系】OrderServiceがIllegalArgumentExceptionをスローした場合、400 Bad Requestと例外メッセージを返す")
            void placeOrder_WhenOrderServiceThrowsIllegalArgumentException_ShouldReturnBadRequest() throws Exception {
                String errorMessage = "商品が見つかりません。";
                when(orderService.placeOrder(any(CartRespons.class), any(OrderRequest.class), any(HttpSession.class)))
                        .thenThrow(new IllegalArgumentException(errorMessage));

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOrderRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message", is(errorMessage)));

                
                verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
                verify(orderService, times(1)).placeOrder(eq(cartWithItems), eq(validOrderRequest), any(HttpSession.class));
                verifyNoMoreInteractions(cartService, orderService);
            }

            @Test
            @DisplayName("【異常系】OrderServiceがIllegalStateExceptionをスローした場合、409 Conflictと例外メッセージを返す")
            void placeOrder_WhenOrderServiceThrowsIllegalStateException_ShouldReturnConflict() throws Exception {
                String errorMessage = "在庫が不足しています。";
                when(orderService.placeOrder(any(CartRespons.class), any(OrderRequest.class), any(HttpSession.class)))
                        .thenThrow(new IllegalStateException(errorMessage));

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOrderRequest)))
                        .andExpect(status().isConflict())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message", is(errorMessage)));

                
                verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
                verify(orderService, times(1)).placeOrder(eq(cartWithItems), eq(validOrderRequest), any(HttpSession.class));
                verifyNoMoreInteractions(cartService, orderService);
            }

            @Test
            @DisplayName("【異常系】OrderServiceがその他のExceptionをスローした場合、500 Internal Server Errorと汎用エラーメッセージを返す")
            void placeOrder_WhenOrderServiceThrowsGenericException_ShouldReturnInternalServerError() throws Exception {
                when(orderService.placeOrder(any(CartRespons.class), any(OrderRequest.class), any(HttpSession.class)))
                        .thenThrow(new RuntimeException("DB接続エラーが発生しました。")); 

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOrderRequest)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message", is("注文確定中に予期せぬエラーが発生しました。")));

                
                verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
                verify(orderService, times(1)).placeOrder(eq(cartWithItems), eq(validOrderRequest), any(HttpSession.class));
                verifyNoMoreInteractions(cartService, orderService);
            }
        }
    }
}