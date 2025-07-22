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
import static org.mockito.ArgumentMatchers.argThat; // argThat をインポート
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
        // 会員IDをセッションに設定 (OrderControllerがこれを使用する)
        mockSession.setAttribute("customerId", 1);

        cartWithItems = new CartRespons();
        CartItemResponse item = new CartItemResponse(
                "p001", 1, "商品A", BigDecimal.valueOf(1000), "image_url", 1, BigDecimal.valueOf(1000)
        );
        cartWithItems.addItem(item);

        emptyCart = new CartRespons();

        // ★修正: CustomerInfoのコンストラクタにcustomerIdを追加
        // リクエストボディにはcustomerIdを含めない（Controllerでセットするため）
        // テストではControllerがセットするcustomerIdを考慮しないといけない
        // ここでは、リクエストボディからくるCustomerInfoを表現するため、nullとしておく
        validCustomerInfo = new CustomerInfo(
                null, // ControllerがセッションからcustomerIdをセットするため、リクエストボディではnull
                "山田 太郎",
                "yamada@example.com",
                "東京都渋谷区1-1-1",
                "09012345678"
        );

        // ★修正: OrderRequestからcustomerIdフィールドを削除したため、その設定を削除
        validOrderRequest = new OrderRequest();
        validOrderRequest.setCustomerInfo(validCustomerInfo);
        validOrderRequest.setPaymentMethod("クレジットカード");
        // OrderRequestのitems, totalPrice, shippingFeeも設定が必要（バリデーションを通すため）
        validOrderRequest.setItems(Collections.singletonList(new OrderRequest.OrderItemRequest(1L, "商品A", 1, 1000)));
        validOrderRequest.setTotalPrice(1000); // 適切な値に設定
        validOrderRequest.setShippingFee(500); // 適切な値に設定


        sampleOrderItemDetailResponse = new OrderItemDetailResponse(
                1, "商品A", "http://example.com/product_a.jpg", 1, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000)
        );

        // ★修正: OrderResponseのCustomerInfoにもcustomerIdを設定
        // テストケースの検証のため、mockSession.setAttribute("customerId", 1)に対応する値を設定
        CustomerInfo responseCustomerInfo = new CustomerInfo(
            1, // OrderServiceが保存した結果としてcustomerIdがセットされる
            "山田 太郎",
            "yamada@example.com",
            "東京都渋谷区1-1-1",
            "09012345678"
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
                responseCustomerInfo, // ★修正: customerInfoにもcustomerIdが含まれるように
                "注文が正常に完了しました。"
        );

        // ★修正: orderService.placeOrder のモック設定から HttpSession を削除
        lenient().when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(cartWithItems);
        // OrderControllerがvalidOrderRequest.getCustomerInfo().setCustomerId(customerId)を実行するので、
        // placeOrderに渡されるorderRequestはcustomerIdがセットされた状態になっている
        lenient().when(orderService.placeOrder(
            eq(cartWithItems),
            argThat(req -> req.getCustomerInfo() != null && req.getCustomerInfo().getCustomerId() != null) // customerIdがセットされていることを検証
            // any(OrderRequest.class) でも良いが、より厳密に検証する場合
            // (eq(validOrderRequest)) は使えない。なぜなら Controller で orderRequest の中身が変更されるから
           
        )).thenReturn(sampleOrderResponse);
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
                    // ★追加: customerInfo.customerId の検証
                    .andExpect(jsonPath("$.customerInfo.customerId", is(sampleOrderResponse.getCustomerInfo().getCustomerId())))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].productId", is(sampleOrderItemDetailResponse.getProductId())));

            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));

            // ★修正: OrderRequestの中身がControllerで変更されるため、any(OrderRequest.class)を使うか、
            // より厳密に検証する場合は argThat() を使う
            // ここでは OrderController の修正で placeOrder の引数から HttpSession が削除されていない前提で、
            // any(HttpSession.class) を残します。
            verify(orderService, times(1)).placeOrder(
                eq(cartWithItems),
                argThat(req -> req.getCustomerInfo() != null && req.getCustomerInfo().getCustomerId() != null)
               
            );
            verify(cartService, times(1)).clearCart(any(HttpSession.class)); // Controllerでカートクリアを呼び出すため
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
                // バリデーションのために他の必須フィールドも設定（もしあれば）
                invalidRequest.setItems(Collections.singletonList(new OrderRequest.OrderItemRequest(1L, "商品A", 1, 1000)));
                invalidRequest.setTotalPrice(1000);
                invalidRequest.setShippingFee(500);

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
                // ★修正: CustomerInfoのコンストラクタにcustomerIdを追加
                CustomerInfo invalidCustomerInfo = new CustomerInfo(
                        null, "", "test@example.com", "東京都", "09012345678"
                );
                OrderRequest invalidRequest = new OrderRequest();
                invalidRequest.setCustomerInfo(invalidCustomerInfo);
                invalidRequest.setPaymentMethod("クレジットカード");
                invalidRequest.setItems(Collections.singletonList(new OrderRequest.OrderItemRequest(1L, "商品A", 1, 1000)));
                invalidRequest.setTotalPrice(1000);
                invalidRequest.setShippingFee(500);

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
                // ★修正: CustomerInfoのコンストラクタにcustomerIdを追加
                CustomerInfo invalidCustomerInfo = new CustomerInfo(
                        null, "テスト太郎", "invalid-email", "東京都", "09012345678"
                );
                OrderRequest invalidRequest = new OrderRequest();
                invalidRequest.setCustomerInfo(invalidCustomerInfo);
                invalidRequest.setPaymentMethod("クレジットカード");
                invalidRequest.setItems(Collections.singletonList(new OrderRequest.OrderItemRequest(1L, "商品A", 1, 1000)));
                invalidRequest.setTotalPrice(1000);
                invalidRequest.setShippingFee(500);

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
                // ★修正: CustomerInfoのコンストラクタにcustomerIdを追加
                CustomerInfo invalidCustomerInfo = new CustomerInfo(
                        null, "テスト太郎", "test@example.com", "", "09012345678"
                );
                OrderRequest invalidRequest = new OrderRequest();
                invalidRequest.setCustomerInfo(invalidCustomerInfo);
                invalidRequest.setPaymentMethod("クレジットカード");
                invalidRequest.setItems(Collections.singletonList(new OrderRequest.OrderItemRequest(1L, "商品A", 1, 1000)));
                invalidRequest.setTotalPrice(1000);
                invalidRequest.setShippingFee(500);

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
                // ★修正: CustomerInfoのコンストラクタにcustomerIdを追加
                CustomerInfo invalidCustomerInfo = new CustomerInfo(
                        null, "テスト太郎", "test@example.com", "東京都", ""
                );
                OrderRequest invalidRequest = new OrderRequest();
                invalidRequest.setCustomerInfo(invalidCustomerInfo);
                invalidRequest.setPaymentMethod("クレジットカード");
                invalidRequest.setItems(Collections.singletonList(new OrderRequest.OrderItemRequest(1L, "商品A", 1, 1000)));
                invalidRequest.setTotalPrice(1000);
                invalidRequest.setShippingFee(500);

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
                // ★修正: CustomerInfoのコンストラクタにcustomerIdを追加
                CustomerInfo invalidCustomerInfo = new CustomerInfo(
                        null, // customerId は Controller でセットされるためここでは null
                        "",
                        "invalid-email",
                        "",
                        "123"
                );
                OrderRequest invalidRequest = new OrderRequest();
                invalidRequest.setCustomerInfo(invalidCustomerInfo);
                invalidRequest.setPaymentMethod("");
                // バリデーションエラーのために他の必須フィールドも設定
                invalidRequest.setItems(Collections.emptyList()); // min = 1 に引っかかる
                invalidRequest.setTotalPrice(null); // NotNull に引っかかる
                invalidRequest.setShippingFee(null); // NotNull に引っかかる


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
                                containsString("支払い方法は必須です。"),
                                containsString("注文商品は1つ以上選択してください。"),
                                containsString("商品合計は必須です。"),
                                containsString("送料は必須です。")
                        )));

                verifyNoInteractions(cartService, orderService);
            }

            @Test
            @DisplayName("【異常系】JSON構文が不正な場合、400 Bad RequestとJSONパースエラーメッセージを返す")
            void placeOrder_WithInvalidJsonSyntax_ReturnsBadRequestWithParseError() throws Exception {
                String invalidJson = "{ \"customerInfo\": { \"name\": \"Test User\", \"email\": \"test@example.com\" }, \"paymentMethod\": \"クレジットカード\"";

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message", is("リクエストボディのJSON形式が不正です。")));

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
                // ★修正: orderService.placeOrder の引数から HttpSession を削除
                when(orderService.placeOrder(
                    any(CartRespons.class),
                    argThat(req -> req.getCustomerInfo() != null && req.getCustomerInfo().getCustomerId() != null)
                )).thenThrow(new IllegalArgumentException(errorMessage));

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOrderRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message", is(errorMessage)));

                verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
                // ★修正: orderService.placeOrder の引数から HttpSession を削除
                verify(orderService, times(1)).placeOrder(
                    eq(cartWithItems),
                    argThat(req -> req.getCustomerInfo() != null && req.getCustomerInfo().getCustomerId() != null)
                );
                verifyNoMoreInteractions(cartService, orderService);
            }

            @Test
            @DisplayName("【異常系】OrderServiceがIllegalStateExceptionをスローした場合、409 Conflictと例外メッセージを返す")
            void placeOrder_WhenOrderServiceThrowsIllegalStateException_ShouldReturnConflict() throws Exception {
                String errorMessage = "在庫が不足しています。";
                // ★修正: orderService.placeOrder の引数から HttpSession を削除
                when(orderService.placeOrder(
                    any(CartRespons.class),
                    argThat(req -> req.getCustomerInfo() != null && req.getCustomerInfo().getCustomerId() != null)
                )).thenThrow(new IllegalStateException(errorMessage));

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOrderRequest)))
                        .andExpect(status().isConflict())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message", is(errorMessage)));

                verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
                // ★修正: orderService.placeOrder の引数から HttpSession を削除
                verify(orderService, times(1)).placeOrder(
                    eq(cartWithItems),
                    argThat(req -> req.getCustomerInfo() != null && req.getCustomerInfo().getCustomerId() != null)
                );
                verifyNoMoreInteractions(cartService, orderService);
            }

            @Test
            @DisplayName("【異常系】OrderServiceがその他のExceptionをスローした場合、500 Internal Server Errorと汎用エラーメッセージを返す")
            void placeOrder_WhenOrderServiceThrowsGenericException_ShouldReturnInternalServerError() throws Exception {
                // ★修正: orderService.placeOrder の引数から HttpSession を削除
                when(orderService.placeOrder(
                    any(CartRespons.class),
                    argThat(req -> req.getCustomerInfo() != null && req.getCustomerInfo().getCustomerId() != null)
                )).thenThrow(new RuntimeException("DB接続エラーが発生しました。"));

                mockMvc.perform(post("/api/order/confirm")
                                .session(mockSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validOrderRequest)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message", is("注文確定中に予期せぬエラーが発生しました。")));

                verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
                // ★修正: orderService.placeOrder の引数から HttpSession を削除
                verify(orderService, times(1)).placeOrder(
                    eq(cartWithItems),
                    argThat(req -> req.getCustomerInfo() != null && req.getCustomerInfo().getCustomerId() != null)
                );
                verifyNoMoreInteractions(cartService, orderService);
            }
        }
    }
}