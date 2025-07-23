package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.order.CustomerInfo;
import com.example.simplezakka.dto.order.OrderItemDetailResponse;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.entity.Order;
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
import org.mockito.junit.jupiter.MockitoSettings; 
import org.mockito.quality.Strictness;        

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) 
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

    private CartRespons mockCartMultipleItems; 
    private CartRespons mockCartSingleItem;   
    private CartRespons mockCartEmpty;        
    private OrderRequest validOrderRequestMember; 
    private OrderRequest validOrderRequestGuest;  
    private OrderRequest orderRequestNullCustomerInfo; 
    private CustomerInfo validCustomerInfo;
    private Product mockProduct1;
    private Product mockProduct2; 
    private Customer mockCustomer;

    @BeforeEach
    void setUp() {
        
        mockProduct1 = new Product();
        mockProduct1.setProductId(1);
        mockProduct1.setName("テスト商品A");
        mockProduct1.setPrice(BigDecimal.valueOf(1000));
        mockProduct1.setStock(10);
        mockProduct1.setImageUrl("http://example.com/test_productA.jpg");

        mockProduct2 = new Product();
        mockProduct2.setProductId(2);
        mockProduct2.setName("テスト商品B");
        mockProduct2.setPrice(BigDecimal.valueOf(2500)); 
        mockProduct2.setStock(5);
        mockProduct2.setImageUrl("http://example.com/test_productB.jpg");

        // --- モックカートの設定 (複数商品) ---
        mockCartMultipleItems = new CartRespons();
        CartItemResponse cartItem1 = new CartItemResponse(
                "p001",
                1,
                "テスト商品A",
                BigDecimal.valueOf(1000),
                "http://example.com/test_productA.jpg",
                2,
                BigDecimal.valueOf(2000)
        );
        CartItemResponse cartItem2 = new CartItemResponse(
                "p002",
                2,
                "テスト商品B",
                BigDecimal.valueOf(2500),
                "http://example.com/test_productB.jpg",
                1,
                BigDecimal.valueOf(2500)
        );
        mockCartMultipleItems.addItem(cartItem1);
        mockCartMultipleItems.addItem(cartItem2); 

        // --- モックカートの設定 (商品1つ) ---
        mockCartSingleItem = new CartRespons();
        CartItemResponse singleCartItem = new CartItemResponse(
                "p001",
                1,
                "テスト商品A",
                BigDecimal.valueOf(1000),
                "http://example.com/test_productA.jpg",
                1,
                BigDecimal.valueOf(1000)
        );
        mockCartSingleItem.addItem(singleCartItem);

        
        mockCartEmpty = new CartRespons();


        
        validCustomerInfo = new CustomerInfo(
                10, // customerId
                "テスト顧客", // name
                "test@example.com", // email
                "テスト住所", // address
                "09011112222" // phoneNumber
        );

        validOrderRequestMember = new OrderRequest();
        validOrderRequestMember.setCustomerInfo(validCustomerInfo);
        validOrderRequestMember.setPaymentMethod("クレジットカード");

        // --- 有効な顧客情報と注文リクエストの設定 (ゲスト) ---
        validOrderRequestGuest = new OrderRequest();
        validOrderRequestGuest.setCustomerInfo(
                new CustomerInfo(0, "ゲスト太郎", "guest@example.com", "ゲスト住所", "09099998888")
        );
        validOrderRequestGuest.setPaymentMethod("銀行振込");

        // --- 顧客情報がnullの注文リクエストの設定 ---
        orderRequestNullCustomerInfo = new OrderRequest();
        orderRequestNullCustomerInfo.setPaymentMethod("現金");

        // --- モック顧客の設定 ---
        mockCustomer = new Customer();
        mockCustomer.setCustomerId(10);
        mockCustomer.setLastName("テスト"); // 姓を設定
        mockCustomer.setFirstName("顧客"); // 名を設定
        mockCustomer.setEmail("test@example.com");

        // --- リポジトリのモック挙動設定 (共通部分) ---
        when(productRepository.findById(eq(1))).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(eq(2))).thenReturn(Optional.of(mockProduct2));
        when(productRepository.decreaseStock(eq(1), anyInt())).thenReturn(1);
        when(productRepository.decreaseStock(eq(2), anyInt())).thenReturn(1); 
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
            });
            return order;
        });
        when(customerRepository.findById(eq(10))).thenReturn(Optional.of(mockCustomer));
        
        when(customerRepository.findById(eq(0))).thenReturn(Optional.empty());
    }

    
    @Nested
    @DisplayName("placeOrder method")
    class PlaceOrderTests {

        // --- 正常系テスト ---

        @Test
        @DisplayName("注文処理正常系: 複数の商品を含むカートで注文が正常に確定されるべき（送料あり）")
        void placeOrder_Success_MultipleItems_WithShippingFee() {
            
            OrderResponse response = orderService.placeOrder(mockCartMultipleItems, validOrderRequestMember, session);

            
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(1);
            assertThat(response.getMessage()).isEqualTo("注文が正常に完了しました。");
            assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(4500)); 
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(500)); 
            assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(5000)); 
            assertThat(response.getPaymentMethod()).isEqualTo(validOrderRequestMember.getPaymentMethod());
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getCustomerInfo()).isEqualTo(validCustomerInfo);
            assertThat(response.getOrderDate()).isNotNull();
            assertThat(response.getItems()).hasSize(2); 
            assertThat(response.getItems().get(0).getProductName()).isEqualTo("テスト商品A");
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
            assertThat(response.getItems().get(1).getProductName()).isEqualTo("テスト商品B");
            assertThat(response.getItems().get(1).getQuantity()).isEqualTo(1);

            
            verify(productRepository, times(1)).findById(eq(1));
            verify(productRepository, times(1)).findById(eq(2));
            verify(productRepository, times(1)).decreaseStock(eq(1), eq(2));
            verify(productRepository, times(1)).decreaseStock(eq(2), eq(1));
            verify(orderRepository, times(1)).save(any(Order.class));
            verify(cartService, times(1)).clearCart(eq(session));

            
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order capturedOrder = orderCaptor.getValue();

            assertThat(capturedOrder.getOrderEmail()).isEqualTo(validCustomerInfo.getEmail());
            assertThat(capturedOrder.getOrderName()).isEqualTo(validCustomerInfo.getName());
            assertThat(capturedOrder.getOrderAddress()).isEqualTo(validCustomerInfo.getAddress());
            assertThat(capturedOrder.getOrderPhoneNumber()).isEqualTo(validCustomerInfo.getPhoneNumber());
            assertThat(capturedOrder.getPaymentMethod()).isEqualTo(validOrderRequestMember.getPaymentMethod());
            assertThat(capturedOrder.getStatus()).isEqualTo("PENDING");
            assertThat(capturedOrder.getCustomer()).isEqualTo(mockCustomer);
            assertThat(capturedOrder.getIsGuest()).isFalse();
            assertThat(capturedOrder.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(capturedOrder.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(5000)); 
            assertThat(capturedOrder.getOrderDetails()).hasSize(2);
        }

        @Test
        @DisplayName("注文処理正常系: 商品が1つのカートで注文が正常に確定されるべき")
        void placeOrder_Success_WithSingleItemInCart() {
            
            OrderResponse response = orderService.placeOrder(mockCartSingleItem, validOrderRequestMember, session);

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(1);
            assertThat(response.getMessage()).isEqualTo("注文が正常に完了しました。");
            assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000)); 
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(1500)); 
            assertThat(response.getItems()).hasSize(1);

            verify(productRepository, times(1)).findById(eq(1));
            verify(productRepository, times(1)).decreaseStock(eq(1), eq(1));
            verify(orderRepository, times(1)).save(any(Order.class));
            verify(cartService, times(1)).clearCart(eq(session));
        }

        @Test
@DisplayName("顧客IDが0の場合、ゲスト注文として設定し、注文が正常に完了すべき")
void placeOrder_Success_WhenCustomerIdIsZero_ShouldSetGuestOrder() {
    OrderResponse response = orderService.placeOrder(mockCartSingleItem, validOrderRequestGuest, session);
    assertNotNull(response);
    assertThat(response.getMessage()).isEqualTo("注文が正常に完了しました。");
    verify(productRepository, times(1)).findById(eq(1)); 
    verifyNoInteractions(customerRepository); 

    verify(productRepository, times(1)).decreaseStock(eq(1), eq(1)); 
    verify(orderRepository, times(1)).save(any(Order.class)); 
    verify(cartService, times(1)).clearCart(eq(session)); 

    
    
    
    
    
}

        @Test
        @DisplayName("顧客情報設定, 正常系: 会員ユーザーでの注文が正常に確定されるべき")
        void placeOrder_Success_WhenCustomerIdIsValid_ShouldSetCustomerAndNotGuest() { 
            OrderResponse response = orderService.placeOrder(mockCartSingleItem, validOrderRequestMember, session);

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("注文が正常に完了しました。");

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order capturedOrder = orderCaptor.getValue();

            assertThat(capturedOrder.getCustomer()).isEqualTo(mockCustomer);
            assertThat(capturedOrder.getIsGuest()).isFalse();
            assertThat(capturedOrder.getOrderEmail()).isEqualTo(validOrderRequestMember.getCustomerInfo().getEmail());

            verify(customerRepository, times(1)).findById(eq(validCustomerInfo.getCustomerId()));
        }

        @Test
        @DisplayName("送料判定, 正常系: 商品合計が5000円以上の注文で送料が0円になるべき")
        void placeOrder_Success_WhenSubtotalOver5000_ShouldSetFreeShipping() { 
            mockCartMultipleItems.getItems().clear();
            CartItemResponse expensiveItem = new CartItemResponse(
                    "p002", 2, "高額商品", BigDecimal.valueOf(6000), "image_url", 1, BigDecimal.valueOf(6000)
            );
            mockCartMultipleItems.addItem(expensiveItem);

            OrderResponse response = orderService.placeOrder(mockCartMultipleItems, validOrderRequestMember, session);

            assertThat(response).isNotNull();
            assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(6000)); 
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(6000)); 
        }

        @Test
        @DisplayName("送料判定, 正常系: 商品合計がぴったり5000円の場合、送料が0円になるべき")
        void placeOrder_Success_WhenSubtotalIsExactly5000_ShouldSetFreeShipping() { 
            mockCartMultipleItems.getItems().clear();
            CartItemResponse item1 = new CartItemResponse(
                    "p001", 1, "商品A", BigDecimal.valueOf(2500), "image_url_a", 1, BigDecimal.valueOf(2500)
            );
            CartItemResponse item2 = new CartItemResponse(
                    "p002", 2, "商品B", BigDecimal.valueOf(2500), "image_url_b", 1, BigDecimal.valueOf(2500)
            );
            mockCartMultipleItems.addItem(item1);
            mockCartMultipleItems.addItem(item2); 

            
            Product productA = new Product();
            productA.setProductId(1); productA.setName("商品A"); productA.setPrice(BigDecimal.valueOf(2500)); productA.setStock(10);
            Product productB = new Product();
            productB.setProductId(2); productB.setName("商品B"); productB.setPrice(BigDecimal.valueOf(2500)); productB.setStock(10);

            when(productRepository.findById(eq(1))).thenReturn(Optional.of(productA));
            when(productRepository.findById(eq(2))).thenReturn(Optional.of(productB));


            OrderResponse response = orderService.placeOrder(mockCartMultipleItems, validOrderRequestMember, session);

            assertThat(response).isNotNull();
            assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(5000)); 
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(5000)); 
        }

        @Test
        @DisplayName("送料判定, 正常系: 商品合計が5000円未満の場合、送料が加算されるべき")
        void placeOrder_Success_WhenSubtotalUnder5000_ShouldAddShippingFee() { 
            
            OrderResponse response = orderService.placeOrder(mockCartMultipleItems, validOrderRequestMember, session);

            assertThat(response).isNotNull();
            assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(4500)); 
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(5000)); 
        }

        @Test
        @DisplayName("OrderItemDetailResponse に正しい値がマッピングされるべき")
        void placeOrder_MapsOrderItemDetailResponseCorrectly() {
            
            OrderResponse response = orderService.placeOrder(mockCartSingleItem, validOrderRequestMember, session);

            assertThat(response.getItems()).isNotNull().hasSize(1);
            OrderItemDetailResponse responseItem = response.getItems().get(0);
            assertThat(responseItem.getProductId()).isEqualTo(mockProduct1.getProductId());
            assertThat(responseItem.getProductName()).isEqualTo(mockProduct1.getName());
            assertThat(responseItem.getImageUrl()).isEqualTo(mockProduct1.getImageUrl());
            assertThat(responseItem.getQuantity()).isEqualTo(1); 
            assertThat(responseItem.getUnitPrice()).isEqualByComparingTo(mockProduct1.getPrice());
            assertThat(responseItem.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(1000)); 
        }

        @Test
        @DisplayName("セッションが渡され、clearCartが適切に呼び出されるべき")
        void placeOrder_SessionPassedAndCartCleared() {
            orderService.placeOrder(mockCartSingleItem, validOrderRequestMember, session);
            verify(cartService, times(1)).clearCart(session);
        }


        @Test
        @DisplayName("入力値検証, 異常系: カートがnullの場合、IllegalArgumentExceptionをスローすべき")
        void placeOrder_Fail_WhenCartIsNull_ShouldThrowIllegalArgumentException() { 
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(null, validOrderRequestMember, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("カートに商品がありません。");
            verifyNoInteractions(productRepository, orderRepository, customerRepository, cartService);
        }

        @Test
        @DisplayName("入力値検証, 異常系: カートが空の場合、IllegalArgumentExceptionをスローすべき")
        void placeOrder_Fail_WhenCartIsEmpty_ShouldThrowIllegalArgumentException() { 
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(mockCartEmpty, validOrderRequestMember, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("カートに商品がありません。");
            verifyNoInteractions(productRepository, orderRepository, customerRepository, cartService);
        }

        @Test
        @DisplayName("入力値検証, 異常系: 顧客情報がnullの場合、IllegalArgumentExceptionをスローすべき")
        void placeOrder_Fail_WhenOrderRequestCustomerInfoIsNull_ShouldThrowIllegalArgumentException() { 
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(mockCartSingleItem, orderRequestNullCustomerInfo, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("顧客情報が不足しています。");
            verifyNoInteractions(productRepository, orderRepository, customerRepository, cartService);
        }

        @Test
        @DisplayName("在庫チェック, 異常系: カート内の商品が見つからない場合、IllegalArgumentExceptionをスローすべき")
        void placeOrder_Fail_WhenProductNotFoundDuringStockCheck_ShouldThrowIllegalArgumentException() { 
            when(productRepository.findById(eq(1))).thenReturn(Optional.empty());

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(mockCartSingleItem, validOrderRequestMember, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("商品が見つかりません: テスト商品A"); 
            verify(productRepository, times(1)).findById(eq(1));
            verifyNoMoreInteractions(productRepository);
            verifyNoInteractions(orderRepository, customerRepository, cartService);
        }

     @Test
@DisplayName("在庫不足, 異常系: 複数商品のうち一部の在庫が不足している場合、IllegalStateExceptionをスローすべき")
void placeOrder_Fail_WhenPartialStockIsInsufficient_ShouldThrowIllegalStateException() {
    mockProduct1.setStock(1);
    when(productRepository.findById(eq(1))).thenReturn(Optional.of(mockProduct1));
    IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
        orderService.placeOrder(mockCartMultipleItems, validOrderRequestMember, session);
    });
    assertThat(thrown.getMessage()).isEqualTo("申し訳ございません、テスト商品Aの在庫が不足しています。現在の在庫: 1");
    verify(productRepository, times(1)).findById(eq(1)); 
    verifyNoInteractions(customerRepository); 
    verify(productRepository, times(0)).decreaseStock(anyInt(), anyInt()); 
    verifyNoInteractions(orderRepository); 
    verifyNoInteractions(cartService); 
}

        @Test
        @DisplayName("顧客情報設定, 異常系: 会員情報が見つからない場合、IllegalArgumentExceptionをスローすべき")
        void placeOrder_Fail_WhenCustomerNotFoundById_ShouldThrowIllegalArgumentException() { 
            when(customerRepository.findById(eq(validCustomerInfo.getCustomerId()))).thenReturn(Optional.empty());

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                orderService.placeOrder(mockCartSingleItem, validOrderRequestMember, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("会員情報が見つかりません。ID: " + validCustomerInfo.getCustomerId());
            verify(customerRepository, times(1)).findById(eq(validCustomerInfo.getCustomerId()));
            verify(productRepository, times(1)).findById(anyInt()); 
            verifyNoMoreInteractions(customerRepository);
            verifyNoMoreInteractions(productRepository);
            verifyNoInteractions(orderRepository, cartService);
        }

        @Test
@DisplayName("在庫減算, 異常系: 在庫減少に失敗した場合、IllegalStateExceptionをスローすべき")
void placeOrder_Fail_WhenDecreaseStockFails_ShouldThrowIllegalStateException() {
    when(productRepository.decreaseStock(eq(1), anyInt())).thenReturn(0);
    IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
        orderService.placeOrder(mockCartSingleItem, validOrderRequestMember, session);
    });

    assertThat(thrown.getMessage()).isEqualTo("商品 テスト商品A の在庫更新に失敗しました。時間をおいて再度お試しください。");
    verify(productRepository, times(1)).findById(eq(1));
    verify(productRepository, times(1)).decreaseStock(eq(1), eq(1));
    verify(customerRepository, times(1)).findById(eq(validCustomerInfo.getCustomerId()));
    verifyNoInteractions(orderRepository);
    verifyNoInteractions(cartService);
}

        @Test
        @DisplayName("依存関係連携(エラー), 異常系: OrderRepository.saveが例外をスローする場合、その例外が伝播すべき")
        void placeOrder_Fail_WhenOrderSaveThrowsException_ShouldRollback() { 
            when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DBエラー"));

            RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                orderService.placeOrder(mockCartSingleItem, validOrderRequestMember, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("DBエラー");
            verify(productRepository, times(1)).findById(eq(1));
            verify(productRepository, times(1)).decreaseStock(eq(1), eq(1));
            verify(orderRepository, times(1)).save(any(Order.class));
            verifyNoInteractions(cartService); 
        }

        @Test
        @DisplayName("依存関係連携(エラー), 異常系: CartService.clearCartが例外をスローする場合、その例外が伝播すべき")
        void placeOrder_Fail_WhenClearCartThrowsException_ShouldRollback() { 
            doThrow(new RuntimeException("カートクリア失敗")).when(cartService).clearCart(any(HttpSession.class));

            RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                orderService.placeOrder(mockCartSingleItem, validOrderRequestMember, session);
            });
            assertThat(thrown.getMessage()).isEqualTo("カートクリア失敗");
            verify(productRepository, times(1)).findById(eq(1));
            verify(productRepository, times(1)).decreaseStock(eq(1), eq(1));
            verify(orderRepository, times(1)).save(any(Order.class)); 
            verify(cartService, times(1)).clearCart(eq(session)); 
        }
    }
}