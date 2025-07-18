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
    private CustomerInfo sampleCustomerInfo;
    private OrderItemDetailResponse sampleOrderItemDetailResponse;

    @BeforeEach
    void setUp() {
        mockSession = new MockHttpSession();
        mockSession.setAttribute("customerId", 1);

        // --- カート準備 ---
        cartWithItems = new CartRespons();
        CartItemResponse item = new CartItemResponse(
                "p001", 1, "商品A", BigDecimal.valueOf(1000), "image_url", 1, BigDecimal.valueOf(1000)
        );
        cartWithItems.addItem(item);

        emptyCart = new CartRespons();

        // --- 顧客情報準備 ---
        sampleCustomerInfo = new CustomerInfo(
                1, "山田 太郎", "yamada@example.com", "東京都渋谷区1-1-1", "09012345678"
        );

        // --- 注文リクエスト準備 ---
        validOrderRequest = new OrderRequest();
        validOrderRequest.setCustomerInfo(sampleCustomerInfo);
        validOrderRequest.setPaymentMethod("クレジットカード");

        // --- 注文アイテム詳細準備 ---
        sampleOrderItemDetailResponse = new OrderItemDetailResponse(
                1, "商品A", "http://example.com/product_a.jpg", 1, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000)
        );

        // --- 注文レスポンス準備 (全引数コンストラクタを使用) ---
        sampleOrderResponse = new OrderResponse(
                123, // orderId
                LocalDateTime.now(), // orderDate
                BigDecimal.valueOf(1000), // totalPrice (商品合計)
                BigDecimal.valueOf(500), // shippingFee
                BigDecimal.valueOf(1500), // grandTotal (商品合計 + 送料)
                "クレジットカード", // paymentMethod
                "PENDING", // status
                Collections.singletonList(sampleOrderItemDetailResponse), // items
                sampleCustomerInfo, // customerInfo
                "注文が正常に完了しました。" // message
        );

        // --- Serviceメソッドのデフォルトモック設定 (lenient) ---
        lenient().when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(cartWithItems);
        lenient().when(orderService.placeOrder(any(CartRespons.class), any(OrderRequest.class), any(HttpSession.class)))
                .thenReturn(sampleOrderResponse);

    }

    // POST /api/order/confirm のテスト
   
    @Nested
    @DisplayName("POST /api/order/confirm")
    class PlaceOrderTests {

        @Test
        @DisplayName("正常系: 注文確定成功 - 201 Created を返す")
        void confirmOrder_ValidRequest_ReturnsCreated() throws Exception {
            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOrderRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.orderId", is(sampleOrderResponse.getOrderId())))
                    .andExpect(jsonPath("$.orderDate", is(notNullValue())))
                    .andExpect(jsonPath("$.message", is(sampleOrderResponse.getMessage())))
                    .andExpect(jsonPath("$.totalPrice", closeTo(sampleOrderResponse.getTotalPrice().doubleValue(), 0.01)))
                    .andExpect(jsonPath("$.shippingFee", closeTo(sampleOrderResponse.getShippingFee().doubleValue(), 0.01)))
                    .andExpect(jsonPath("$.grandTotal", closeTo(sampleOrderResponse.getGrandTotal().doubleValue(), 0.01)))
                    .andExpect(jsonPath("$.paymentMethod", is(sampleOrderResponse.getPaymentMethod())))
                    .andExpect(jsonPath("$.status", is(sampleOrderResponse.getStatus())))
                    .andExpect(jsonPath("$.customerInfo.name", is(sampleCustomerInfo.getName())))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].productId", is(sampleOrderItemDetailResponse.getProductId())));


            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
            verify(orderService, times(1)).placeOrder(eq(cartWithItems), eq(validOrderRequest), any(HttpSession.class));
            verifyNoMoreInteractions(cartService, orderService);
        }

        @Test
        @DisplayName("異常系: カートが空またはnullの場合、400 Bad Requestとエラーメッセージを返す")
        void confirmOrder_EmptyOrNullCart_ReturnsBadRequest() throws Exception {
            // 空のカートの場合
            when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(emptyCart);

            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOrderRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("カートが空か無効です。注文を確定できません。")));

            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
            verifyNoInteractions(orderService);

            // nullのカートの場合
            reset(cartService);
            when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(null);

            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOrderRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("カートが空か無効です。注文を確定できません。")));

            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("異常系: OrderRequestのバリデーションエラー（単一項目）、400 Bad Requestとエラーメッセージを返す")
        void confirmOrder_SingleValidationError_ReturnsBadRequest() throws Exception {
            CustomerInfo invalidCustomerInfo = new CustomerInfo(
                    null, // customerId
                    null, // name (NotBlank違反)
                    "test@example.com",
                    "Test Address",
                    "09012345678"
            );
            OrderRequest invalidRequest = new OrderRequest();
            invalidRequest.setCustomerInfo(invalidCustomerInfo);
            invalidRequest.setPaymentMethod("クレジットカード");

            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", is("氏名は必須です。")));

            verifyNoInteractions(cartService, orderService);
        }

        @Test
        @DisplayName("異常系: OrderRequestのバリデーションエラー（複数項目）、400 Bad Requestと連結されたエラーメッセージを返す")
        void confirmOrder_MultipleValidationErrors_ReturnsBadRequest() throws Exception {
            CustomerInfo invalidCustomerInfo = new CustomerInfo(
                    null,
                    "", // Blank
                    "invalid-email", // Email形式違反
                    "", // Blank
                    "123" // Pattern違反
            );
            OrderRequest invalidRequest = new OrderRequest();
            invalidRequest.setCustomerInfo(invalidCustomerInfo);
            invalidRequest.setPaymentMethod(""); // Blank

            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", allOf(
                            containsString("氏名は必須です。"),
                            containsString("メールアドレスは必須です。"),
                            containsString("有効なメールアドレス形式で入力してください。"),
                            containsString("住所は必須です。"),
                            containsString("電話番号は10桁または11桁の数字で入力してください。"),
                            containsString("支払い方法は必須です。")
                    )));

            verifyNoInteractions(cartService, orderService);
        }

        @Test
        @DisplayName("正常系: Content-TypeがJSONで返るか確認")
        void confirmOrder_ResponseIsJson() throws Exception {
            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOrderRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("正常系: placeOrderに正しい引数が渡るか")
        void confirmOrder_PlaceOrderCalledWithCorrectArguments() throws Exception {
            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOrderRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated());

            verify(orderService, times(1)).placeOrder(
                    eq(cartWithItems),
                    eq(validOrderRequest),
                    any(HttpSession.class)
            );
        }

        @Test
        @DisplayName("異常系: OrderServiceがIllegalArgumentExceptionをスローした場合、400 Bad Requestと例外メッセージを返す")
        void confirmOrder_ServiceThrowsIllegalArgument_ReturnsBadRequest() throws Exception {
            String errorMessage = "商品が見つかりません。";
            when(orderService.placeOrder(any(CartRespons.class), any(OrderRequest.class), any(HttpSession.class)))
                    .thenThrow(new IllegalArgumentException(errorMessage));

            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOrderRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", is(errorMessage)));

            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
            verify(orderService, times(1)).placeOrder(eq(cartWithItems), eq(validOrderRequest), any(HttpSession.class));
            verifyNoMoreInteractions(cartService, orderService);
        }

        @Test
        @DisplayName("異常系: OrderServiceがIllegalStateExceptionをスローした場合、409 Conflictと例外メッセージを返す")
        void confirmOrder_ServiceThrowsIllegalState_ReturnsConflict() throws Exception {
            String errorMessage = "在庫が不足しています。";
            when(orderService.placeOrder(any(CartRespons.class), any(OrderRequest.class), any(HttpSession.class)))
                    .thenThrow(new IllegalStateException(errorMessage));

            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOrderRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", is(errorMessage)));

            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
            verify(orderService, times(1)).placeOrder(eq(cartWithItems), eq(validOrderRequest), any(HttpSession.class));
            verifyNoMoreInteractions(cartService, orderService);
        }

        @Test
        @DisplayName("異常系: OrderServiceが予期せぬExceptionをスローした場合、500 Internal Server Errorと汎用エラーメッセージを返す")
        void confirmOrder_ServiceThrowsUnexpectedException_ReturnsServerError() throws Exception {
            when(orderService.placeOrder(any(CartRespons.class), any(OrderRequest.class), any(HttpSession.class)))
                    .thenThrow(new RuntimeException("DB接続エラー"));

            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOrderRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", is("注文確定中に予期せぬエラーが発生しました。")));

            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
            verify(orderService, times(1)).placeOrder(eq(cartWithItems), eq(validOrderRequest), any(HttpSession.class));
            verifyNoMoreInteractions(cartService, orderService);
        }

        @Test
        @DisplayName("異常系: JSON構文が不正な場合、400 Bad RequestとJSON解析エラーメッセージを返す")
        void confirmOrder_InvalidJsonSyntax_ReturnsBadRequest() throws Exception {
            String invalidJson = "{\"customerInfo\": \"invalid\",";

            mockMvc.perform(post("/api/order/confirm")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("JSON parse error")));

            verifyNoInteractions(cartService, orderService);
        }
    }
}