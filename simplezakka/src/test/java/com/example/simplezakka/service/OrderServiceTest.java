package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.order.CustomerInfo;
import com.example.simplezakka.dto.order.OrderItemDetailResponse;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.entity.Order;
import com.example.simplezakka.entity.OrderDetail;
import com.example.simplezakka.entity.Product;
import com.example.simplezakka.repository.CustomerRepository;
import com.example.simplezakka.repository.ProductRepository;
import com.example.simplezakka.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CartService cartService;
    @Mock
    private HttpSession session;

    @InjectMocks
    private OrderService orderService;

    private CartRespons mockCart;
    private OrderRequest validOrderRequest;
    private CustomerInfo validCustomerInfo;
    private Product mockProduct;
    private Customer mockCustomer;

    @BeforeEach
    void setUp() {
        // --- モック商品の設定 ---
        mockProduct = new Product();
        mockProduct.setProductId(1);
        mockProduct.setName("テスト商品");
        mockProduct.setPrice(BigDecimal.valueOf(1000));
        mockProduct.setStock(10);
        mockProduct.setImageUrl("http://example.com/test_product.jpg");

        // --- モックカートの設定 ---
        mockCart = new CartRespons();
        CartItemResponse cartItem = new CartItemResponse(
                "p001",
                1,
                "テスト商品",
                BigDecimal.valueOf(1000),
                "http://example.com/test_product.jpg",
                2,
                BigDecimal.valueOf(2000)
        );
        mockCart.addItem(cartItem);

        // --- 有効な顧客情報と注文リクエストの設定 ---
        validCustomerInfo = new CustomerInfo(
                10, // customerId
                "テスト顧客", // name
                "test@example.com", // email
                "テスト住所", // address
                "09011112222" // phoneNumber
        );

        validOrderRequest = new OrderRequest();
        validOrderRequest.setCustomerInfo(validCustomerInfo);
        validOrderRequest.setPaymentMethod("クレジットカード");

        // --- モック顧客の設定 ---
        mockCustomer = new Customer();
        mockCustomer.setCustomerId(10);
        mockCustomer.setLastName("テスト"); // 姓を設定
        mockCustomer.setFirstName("顧客");  // 名を設定
        mockCustomer.setEmail("test@example.com");

        // --- リポジトリのモック挙動設定 (共通部分) ---
        when(productRepository.findById(eq(1))).thenReturn(Optional.of(mockProduct));
        when(productRepository.decreaseStock(eq(1), anyInt())).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getOrderId() == null) {
                order.setOrderId(1);
            }
            order.getOrderDetails().forEach(detail -> {
                detail.setOrder(order);
                if (detail.getOrderDetailId() == null) {
                    detail.setOrderDetailId(101);
                }
                // OrderDetailのsetSubtotal()呼び出しは対象ファイルから削除されたため、テストからも削除
            });
            return order;
        });
        when(customerRepository.findById(eq(10))).thenReturn(Optional.of(mockCustomer));
    }

    // placeOrder method
    @Nested
    @DisplayName("placeOrder method")
    class PlaceOrderTests {

        @Test
        @DisplayName("正常系: 注文が正常に確定されるべき（5000円未満の送料込み）")
        void placeOrder_ValidScenario_SuccessWithShippingFee() {
            // カートの合計金額は2000円なので、送料500円が加算されることを期待
            OrderResponse response = orderService.placeOrder(mockCart, validOrderRequest, session);

            // 検証: OrderResponseの内容
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(1);
            assertThat(response.getMessage()).isEqualTo("注文が正常に完了しました。");
            assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(2000)); // 商品合計
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(2500)); // 最終合計
            assertThat(response.getPaymentMethod()).isEqualTo(validOrderRequest.getPaymentMethod());
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getCustomerInfo()).isEqualTo(validCustomerInfo);
            assertThat(response.getOrderDate()).isNotNull();
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProductName()).isEqualTo("テスト商品");
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);

            // Repositoryの呼び出し
            verify(productRepository, times(1)).findById(eq(1));
            verify(productRepository, times(1)).decreaseStock(eq(1), eq(2));
            verify(orderRepository, times(1)).save(any(Order.class));
            verify(cartService, times(1)).clearCart(eq(session));

            // Orderエンティティに正しい情報が設定されたか
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order capturedOrder = orderCaptor.getValue();

            assertThat(capturedOrder.getOrderEmail()).isEqualTo(validCustomerInfo.getEmail());
            assertThat(capturedOrder.getOrderName()).isEqualTo(validCustomerInfo.getName());
            assertThat(capturedOrder.getOrderAddress()).isEqualTo(validCustomerInfo.getAddress());
            assertThat(capturedOrder.getOrderPhoneNumber()).isEqualTo(validCustomerInfo.getPhoneNumber());
            assertThat(capturedOrder.getPaymentMethod()).isEqualTo(validOrderRequest.getPaymentMethod());
            assertThat(capturedOrder.getStatus()).isEqualTo("PENDING");
            assertThat(capturedOrder.getCustomer()).isEqualTo(mockCustomer);
            assertThat(capturedOrder.getIsGuest()).isFalse();
            assertThat(capturedOrder.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(capturedOrder.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(2500));
            assertThat(capturedOrder.getOrderDetails()).hasSize(1);
            OrderDetail capturedDetail = capturedOrder.getOrderDetails().get(0);
            assertThat(capturedDetail.getProduct()).isEqualTo(mockProduct);
            assertThat(capturedDetail.getQuantity()).isEqualTo(2);
            assertThat(capturedDetail.getUnitPrice()).isEqualByComparingTo(mockProduct.getPrice());
            assertThat(capturedDetail.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2000)); // getSubtotalで検証
        }

        @Test
        @DisplayName("正常系: ゲストユーザーでの注文が正常に確定されるべき")
        void placeOrder_GuestScenario_Success() {
            CustomerInfo guestCustomerInfo = new CustomerInfo(
                    0, // customerId (ゲスト)
                    "ゲスト太郎",
                    "guest@example.com",
                    "ゲスト住所",
                    "09099998888"
            );

            OrderRequest guestOrderRequest = new OrderRequest();
            guestOrderRequest.setCustomerInfo(guestCustomerInfo);
            guestOrderRequest.setPaymentMethod("銀行振込");

            when(customerRepository.findById(anyInt())).thenReturn(Optional.empty());

            OrderResponse response = orderService.placeOrder(mockCart, guestOrderRequest, session);

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("注文が正常に完了しました。");

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order capturedOrder = orderCaptor.getValue();

            assertThat(capturedOrder.getCustomer()).isNull();
            assertThat(capturedOrder.getIsGuest()).isTrue();
            assertThat(capturedOrder.getOrderEmail()).isEqualTo(guestCustomerInfo.getEmail());

            verify(customerRepository, times(1)).findById(eq(0));
        }

        @Test
        @DisplayName("異常系: カートがnullの場合、IllegalArgumentExceptionをスローすべき")
        void placeOrder_NullCart_ThrowsException() {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(null, validOrderRequest, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("カートに商品がありません。");
            verifyNoInteractions(productRepository, orderRepository, customerRepository, cartService);
        }

        @Test
        @DisplayName("異常系: カートが空の場合、IllegalArgumentExceptionをスローすべき")
        void placeOrder_EmptyCart_ThrowsException() {
            CartRespons emptyCart = new CartRespons();
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(emptyCart, validOrderRequest, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("カートに商品がありません。");
            verifyNoInteractions(productRepository, orderRepository, customerRepository, cartService);
        }

        @Test
        @DisplayName("異常系: 顧客情報がnullの場合、IllegalArgumentExceptionをスローすべき")
        void placeOrder_NullCustomerInfo_ThrowsException() {
            OrderRequest requestWithoutCustomerInfo = new OrderRequest();
            requestWithoutCustomerInfo.setPaymentMethod("現金");

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(mockCart, requestWithoutCustomerInfo, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("顧客情報が不足しています。");
            verifyNoInteractions(productRepository, orderRepository, customerRepository, cartService);
        }

        @Test
        @DisplayName("異常系: カート内の商品が見つからない場合、IllegalArgumentExceptionをスローすべき")
        void placeOrder_ProductNotFound_ThrowsException() {
            when(productRepository.findById(eq(1))).thenReturn(Optional.empty());

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(mockCart, validOrderRequest, session);
            });
            assertThat(thrown.getMessage()).contains("商品が見つかりません: テスト商品");
            verify(productRepository, times(1)).findById(eq(1));
            verifyNoMoreInteractions(productRepository);
            verifyNoInteractions(orderRepository, customerRepository, cartService);
        }

        @Test
        @DisplayName("異常系: 在庫が不足している場合、IllegalStateExceptionをスローすべき")
        void placeOrder_InsufficientStock_ThrowsException() {
            mockProduct.setStock(1);
            when(productRepository.findById(eq(1))).thenReturn(Optional.of(mockProduct));

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                orderService.placeOrder(mockCart, validOrderRequest, session);
            });
            assertThat(thrown.getMessage()).contains("申し訳ございません、テスト商品の在庫が不足しています。現在の在庫: 1");
            verify(productRepository, times(1)).findById(eq(1));
            verifyNoMoreInteractions(productRepository);
            verifyNoInteractions(orderRepository, customerRepository, cartService);
        }

        @Test
        @DisplayName("異常系: 会員情報が見つからない場合（customerIdが設定されているがDBにない）、IllegalArgumentExceptionをスローすべき")
        void placeOrder_CustomerNotFound_ThrowsException() {
            when(customerRepository.findById(eq(validCustomerInfo.getCustomerId()))).thenReturn(Optional.empty());

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(mockCart, validOrderRequest, session);
            });
            assertThat(thrown.getMessage()).contains("会員情報が見つかりません。ID: " + validCustomerInfo.getCustomerId());
            verify(customerRepository, times(1)).findById(eq(validCustomerInfo.getCustomerId()));
            verify(productRepository, times(1)).findById(anyInt());
            verifyNoMoreInteractions(customerRepository);
            verifyNoMoreInteractions(productRepository);
            verifyNoInteractions(orderRepository, cartService);
        }

        @Test
        @DisplayName("異常系: 在庫減少に失敗した場合、IllegalStateExceptionをスローすべき")
        void placeOrder_DecreaseStockFails_ThrowsException() {
            when(productRepository.decreaseStock(eq(1), anyInt())).thenReturn(0);

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                orderService.placeOrder(mockCart, validOrderRequest, session);
            });
            assertThat(thrown.getMessage()).contains("商品 テスト商品 の在庫更新に失敗しました。");
            verify(productRepository, times(1)).findById(eq(1));
            verify(productRepository, times(1)).decreaseStock(eq(1), eq(2));
            verify(orderRepository, times(1)).save(any(Order.class));
            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("正常系: 5000円以上の注文で送料が0円になるべき")
        void placeOrder_Over5000Yen_ShippingFeeIsZero() {
            mockCart.getItems().clear();
            CartItemResponse expensiveItem = new CartItemResponse(
                    "p002", 2, "高額商品", BigDecimal.valueOf(6000), "image_url", 1, BigDecimal.valueOf(6000)
            );
            mockCart.addItem(expensiveItem);

            Product expensiveProduct = new Product();
            expensiveProduct.setProductId(2);
            expensiveProduct.setName("高額商品");
            expensiveProduct.setPrice(BigDecimal.valueOf(6000));
            expensiveProduct.setStock(5);
            when(productRepository.findById(eq(2))).thenReturn(Optional.of(expensiveProduct));
            when(productRepository.decreaseStock(eq(2), anyInt())).thenReturn(1);

            OrderResponse response = orderService.placeOrder(mockCart, validOrderRequest, session);

            assertThat(response).isNotNull();
            assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(6000));
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(6000));
        }

        @Test
        @DisplayName("OrderItemDetailResponse に正しい値がマッピングされるべき")
        void placeOrder_MapsOrderItemDetailResponseCorrectly() {
            OrderResponse response = orderService.placeOrder(mockCart, validOrderRequest, session);

            assertThat(response.getItems()).isNotNull().hasSize(1);
            OrderItemDetailResponse responseItem = response.getItems().get(0);
            assertThat(responseItem.getProductId()).isEqualTo(mockProduct.getProductId());
            assertThat(responseItem.getProductName()).isEqualTo(mockProduct.getName());
            assertThat(responseItem.getImageUrl()).isEqualTo(mockProduct.getImageUrl());
            assertThat(responseItem.getQuantity()).isEqualTo(2);
            assertThat(responseItem.getUnitPrice()).isEqualByComparingTo(mockProduct.getPrice());
            assertThat(responseItem.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        }
        @Test
        @DisplayName("セッションが渡され、clearCartが適切に呼び出されるべき")
        void placeOrder_SessionPassedAndCartCleared() {
        orderService.placeOrder(mockCart, validOrderRequest, session);
        verify(cartService, times(1)).clearCart(session);
        }
    }
  
}