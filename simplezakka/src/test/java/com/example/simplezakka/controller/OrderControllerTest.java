package com.example.simplezakka.controller; 

import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.order.CustomerInfo;
import com.example.simplezakka.dto.order.OrderDetailResponse;
import com.example.simplezakka.dto.order.OrderItemDetailResponse; // 新しくimport
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.OrderSummaryResponse;
import com.example.simplezakka.service.CartService;
import com.example.simplezakka.service.OrderService; // OrderServiceをimport
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean // OrderServiceをモック化
    private OrderService orderService;

    @MockBean
    private CartService cartService;

    private MockHttpSession mockSession;
    private CartRespons cartWithItems;
    private CartRespons emptyCart;
    private OrderRequest validOrderRequest;
    private CustomerInfo validCustomerInfo;
    private OrderResponse sampleOrderResponse;
    private CustomerInfo sampleCustomerInfo; // テスト用顧客情報
    private OrderItemDetailResponse sampleOrderItemDetailResponse; // テスト用注文アイテム詳細

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
      
        lenient().when(orderService.getOrderDetail(any(Integer.class)))
                 .thenReturn(new OrderDetailResponse(
                     1, // orderId
                     LocalDateTime.now(), // orderDate
                     BigDecimal.valueOf(500), // shippingFee
                     BigDecimal.valueOf(1500), // totalAmount (grandTotal)
                     "銀行振込", // paymentMethod
                     Collections.singletonList(sampleOrderItemDetailResponse), // items
                     sampleCustomerInfo // customerInfo
                 ));
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
                    .andExpect(jsonPath("$.totalPrice", closeTo(sampleOrderResponse.getTotalPrice().doubleValue(), 0.01))) // BigDecimalの比較
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
                    .andExpect(jsonPath("$.message", is("氏名は必須です。"))); // エラーメッセージが日本語に

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


    // GET /api/orders/{orderId} のテスト

    @Nested
    @DisplayName("GET /api/orders/{orderId}")
    class GetOrderDetailTests {

        @Test
        @DisplayName("正常系: 注文詳細取得成功 - 200 OK とOrderDetailResponseを返す")
        void getOrderDetail_ValidOrderId_ReturnsDetail() throws Exception {
            Integer testOrderId = 100;
            // OrderDetailResponse の CustomerInfo は、別途 CustomerInfo のコンストラクタで設定
            CustomerInfo detailCustomerInfo = new CustomerInfo(
                1, "詳細太郎", "detail@example.com", "詳細住所", "09011112222"
            );
            OrderItemDetailResponse detailItem = new OrderItemDetailResponse(
                1, "詳細商品", "http://example.com/detail_item.jpg", 2, BigDecimal.valueOf(1000), BigDecimal.valueOf(2000)
            );
            OrderDetailResponse expectedDetail = new OrderDetailResponse(
                testOrderId,
                LocalDateTime.now().minusDays(1),
                BigDecimal.valueOf(500), // shippingFee
                BigDecimal.valueOf(2500), // totalAmount (商品合計2000 + 送料500)
                "銀行振込",
                Collections.singletonList(detailItem),
                detailCustomerInfo
            );
            when(orderService.getOrderDetail(eq(testOrderId))).thenReturn(expectedDetail);

            mockMvc.perform(get("/api/orders/{orderId}", testOrderId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.orderId", is(testOrderId)))
                    .andExpect(jsonPath("$.orderDate", is(notNullValue())))
                    .andExpect(jsonPath("$.shippingFee", closeTo(expectedDetail.getShippingFee().doubleValue(), 0.01)))
                    .andExpect(jsonPath("$.totalAmount", closeTo(expectedDetail.getTotalAmount().doubleValue(), 0.01)))
                    .andExpect(jsonPath("$.paymentMethod", is(expectedDetail.getPaymentMethod())))
                    .andExpect(jsonPath("$.customerInfo.name", is(detailCustomerInfo.getName())))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].productId", is(detailItem.getProductId())))
                    .andExpect(jsonPath("$.items[0].productName", is(detailItem.getProductName())))
                    .andExpect(jsonPath("$.items[0].imageUrl", is(detailItem.getImageUrl())))
                    .andExpect(jsonPath("$.items[0].quantity", is(detailItem.getQuantity())))
                    .andExpect(jsonPath("$.items[0].unitPrice", closeTo(detailItem.getUnitPrice().doubleValue(), 0.01)))
                    .andExpect(jsonPath("$.items[0].subtotal", closeTo(detailItem.getSubtotal().doubleValue(), 0.01)));

            verify(orderService, times(1)).getOrderDetail(eq(testOrderId));
            verifyNoMoreInteractions(orderService);
        }

        @Test
        @DisplayName("異常系: 注文が存在しない場合、500 Internal Server Error を返す (コントローラーの現在の挙動に基づく)")
        void getOrderDetail_OrderNotFound_ReturnsError() throws Exception {
            Integer nonExistentOrderId = 999;
            when(orderService.getOrderDetail(eq(nonExistentOrderId)))
                    .thenReturn(null);

            mockMvc.perform(get("/api/orders/{orderId}", nonExistentOrderId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message", is("注文確定中に予期せぬエラーが発生しました。")));

            verify(orderService, times(1)).getOrderDetail(eq(nonExistentOrderId));
            verifyNoMoreInteractions(orderService);
        }

        @Test
        @DisplayName("異常系: 不正なorderIdフォーマット（数値以外）の場合、400 Bad Requestとパラメータエラーメッセージを返す")
        void getOrderDetail_InvalidOrderIdFormat_ReturnsBadRequest() throws Exception {
            String invalidOrderId = "abc";

            mockMvc.perform(get("/api/orders/{orderId}", invalidOrderId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("型が一致しません")));

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("異常系: OrderServiceが予期せぬ例外をスローした場合、500 Internal Server Errorを返す")
        void getOrderDetail_ServiceError_ReturnsServerError() throws Exception {
            Integer testOrderId = 100;
            when(orderService.getOrderDetail(eq(testOrderId)))
                    .thenThrow(new RuntimeException("ファイル読み込みエラー"));

            mockMvc.perform(get("/api/orders/{orderId}", testOrderId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(orderService, times(1)).getOrderDetail(eq(testOrderId));
            verifyNoMoreInteractions(orderService);
        }
    }
}