package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.Cart;
import com.example.simplezakka.dto.cart.CartItem;
import com.example.simplezakka.dto.order.CustomerInfo;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.OrderItemDetailResponse;
import com.example.simplezakka.dto.order.OrderDetailResponse;
import com.example.simplezakka.dto.order.OrderSummaryResponse;

import com.example.simplezakka.entity.CustomerEntity;
import com.example.simplezakka.entity.OrderEntity;
import com.example.simplezakka.entity.OrderDetailEntity;
import com.example.simplezakka.entity.ProductEntity;
import com.example.simplezakka.exception.BusinessException;
import com.example.simplezakka.exception.ErrorCode;

import com.example.simplezakka.repository.CustomerRepository;
import com.example.simplezakka.repository.OrderDetailRepository;
import com.example.simplezakka.repository.OrderRepository;
import com.example.simplezakka.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList; // ArrayList をインポート
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CartService cartService;

    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            CartService cartService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.cartService = cartService;
    }

    @Transactional
    public OrderResponse placeOrder(Cart cart, OrderRequest orderRequest, HttpSession session) {
        if (cart == null || cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY, "カートに商品がありません。");
        }
        CustomerInfo customerInfo = orderRequest.getCustomerInfo();
        if (customerInfo == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "顧客情報が不足しています。");
        }

        for (CartItem cartItem : cart.getItems().values()) {
            ProductEntity product = productRepository.findById(cartItem.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "商品が見つかりません: " + cartItem.getName()));

            if (product.getStock() < cartItem.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "申し訳ございません、" + product.getName() + "の在庫が不足しています。現在の在庫: " + product.getStock());
            }
        }

        OrderEntity order = new OrderEntity();

        Integer customerId = customerInfo.getCustomerId();
        if (customerId != null && customerId != 0) {
            CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "会員情報が見つかりません。ID: " + customerId));
            order.setCustomer(customer);
            order.setIsGuest(false);
        } else {
            order.setCustomer(null);
            order.setIsGuest(true);
        }

        order.setOrderEmail(customerInfo.getEmail());
        order.setCustomerName(customerInfo.getName());
        order.setShippingAddress(customerInfo.getAddress());
        order.setShippingPhoneNumber(customerInfo.getPhoneNumber());
        order.setPaymentMethod(orderRequest.getPaymentMethod());

        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        BigDecimal subtotal = BigDecimal.valueOf(cart.getTotalPrice());
        BigDecimal shippingFee = calculateShippingFee(subtotal);
        order.setShippingFee(shippingFee);

        order.setTotalPrice(subtotal.add(shippingFee));

        for (CartItem cartItem : cart.getItems().values()) {
            ProductEntity product = productRepository.findById(cartItem.getProductId()).orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "在庫確認後に商品が見つかりません: " + cartItem.getName())
            );

            OrderDetailEntity orderDetail = new OrderDetailEntity();
            orderDetail.setProduct(product);
            orderDetail.setUnitPrice(product.getPrice());
            orderDetail.setQuantity(cartItem.getQuantity());

            order.addOrderDetail(orderDetail);

            int updatedRows = productRepository.decreaseStock(product.getProductId(), cartItem.getQuantity());

            if (updatedRows != 1) {
                throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_FAILURE,
                    "商品 " + product.getName() + " の在庫更新に失敗しました。時間をおいて再度お試しください。");
            }
        }

        OrderEntity savedOrder = orderRepository.save(order);

        cartService.clearCart(session);

        List<OrderItemDetailResponse> responseItems = savedOrder.getOrderDetails().stream()
            .map(detail -> {
                ProductEntity product = detail.getProduct();
                return new OrderItemDetailResponse(
                    product.getProductId(),
                    product.getName(),
                    product.getImageUrl(),
                    detail.getQuantity(),
                    detail.getUnitPrice(),
                    detail.getSubtotal()
                );
            })
            .collect(Collectors.toList());

        return new OrderResponse(
            savedOrder.getOrderId(),
            savedOrder.getOrderDate(),
            savedOrder.getTotalPrice(),
            savedOrder.getShippingFee(),
            savedOrder.getPaymentMethod(),
            savedOrder.getStatus(),
            responseItems,
            customerInfo
        );
    }

    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            return BigDecimal.ZERO;
        } else {
            return BigDecimal.valueOf(500);
        }
    }


    public List<OrderSummaryResponse> getOrderHistoryByCustomer(Integer customerId) {
        // コンパイルを通すための最低限の実装。
        // 実際には、customerRepository と orderRepository を使用して
        // データベースから顧客の注文履歴を取得し、OrderSummaryResponse のリストにマッピングするロジックを実装します。
        System.out.println("getOrderHistoryByCustomer called for customer ID: " + customerId);
        return List.of(); // ダミー: 空のリストを返す
    }


    public OrderDetailResponse getOrderDetail(Integer orderId) {
        System.out.println("getOrderDetail called for order ID: " + orderId);

        // ダミーデータを作成します。実際のアプリケーションでは、orderId を使って
        // データベースから OrderEntity と関連する OrderDetailEntity を取得し、
        // それらの情報から OrderDetailResponse と OrderItemDetailResponse のリストを構築します。

        // 例: 注文に紐づくアイテムリストのダミー
        List<OrderItemDetailResponse> dummyItems = new ArrayList<>();
        dummyItems.add(new OrderItemDetailResponse(
            101, // 商品ID
            "ダミー商品A",
            "http://example.com/dummyA.jpg",
            2,   // 数量
            BigDecimal.valueOf(1500), // 単価
            BigDecimal.valueOf(3000)  // 小計
        ));
        dummyItems.add(new OrderItemDetailResponse(
            102, // 商品ID
            "ダミー商品B",
            "http://example.com/dummyB.jpg",
            1,   // 数量
            BigDecimal.valueOf(2500), // 単価
            BigDecimal.valueOf(2500)  // 小計
        ));

        // 顧客情報のダミー
        CustomerInfo dummyCustomerInfo = new CustomerInfo();
        dummyCustomerInfo.setCustomerId(null); // ゲストの場合
        dummyCustomerInfo.setName("ダミーゲスト太郎");
        dummyCustomerInfo.setEmail("guest@example.com");
        dummyCustomerInfo.setAddress("東京都港区ダミー1-2-3");
        dummyCustomerInfo.setPhoneNumber("090-1234-5678");


        // OrderDetailResponse のコンストラクタに合わせてダミーデータを調整
        return new OrderDetailResponse(
            orderId, // 注文ID
            LocalDateTime.now().minusDays(5), // 注文日（5日前とする）
            BigDecimal.valueOf(5500), // 合計金額 (3000 + 2500 + 0)
            BigDecimal.ZERO,          // 配送手数料（合計5000円超なので無料とする）
            "クレジットカード",        // 支払い方法
            "COMPLETED",              // ステータス
            dummyItems,               // 注文アイテムリスト
            dummyCustomerInfo         // 顧客情報
        );
    }
}