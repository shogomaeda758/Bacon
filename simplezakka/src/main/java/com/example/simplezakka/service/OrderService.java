package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.Cart;
import com.example.simplezakka.dto.cart.CartItem;
import com.example.simplezakka.dto.order.CustomerInfo;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.OrderDetailResponse;
import com.example.simplezakka.dto.order.OrderSummaryResponse;
import com.example.simplezakka.dto.order.OrderItemDetailResponse;
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

    /**
     * 注文を確定し、データベースに保存する。
     *
     * @param cart ユーザーのカート情報
     * @param orderRequest 注文リクエストDTO（顧客情報、支払い方法などを含む、送料は含まない）
     * @param session HTTPセッション
     * @return 注文レスポンスDTO
     * @throws BusinessException 在庫不足、商品未存在、カートが空などのビジネスロジックエラー
     */
    @Transactional
    public OrderResponse placeOrder(Cart cart, OrderRequest orderRequest, HttpSession session) {
        // --- 1. 事前チェック ---
        if (cart == null || cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY, "カートに商品がありません。");
        }
        CustomerInfo customerInfo = orderRequest.getCustomerInfo();
        if (customerInfo == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "顧客情報が不足しています。");
        }

        // --- 2. 在庫と商品情報の最終確認 ---
        for (CartItem cartItem : cart.getItems().values()) {
            ProductEntity product = productRepository.findById(cartItem.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "商品が見つかりません: " + cartItem.getName()));

            if (product.getStock() < cartItem.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "申し訳ございません、" + product.getName() + "の在庫が不足しています。現在の在庫: " + product.getStock());
            }
        }

        // --- 3. OrderEntity の作成 ---
        OrderEntity order = new OrderEntity();

        // 会員情報のマッピングロジック
        Integer customerId = customerInfo.getCustomerId();
        if (customerId != null && customerId != 0) {
            CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "会員情報が見つかりません。"));
            order.setCustomer(customer);
            order.setIsGuest(false);
        } else {
            order.setCustomer(null);
            order.setIsGuest(true);
        }

        // 注文者情報のマッピング (CustomerInfoからOrderEntityへ)
        order.setOrderEmail(customerInfo.getEmail());
        order.setOrderName(customerInfo.getName());
        order.setOrderAddress(customerInfo.getAddress());
        order.setOrderPhoneNumber(customerInfo.getPhoneNumber());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setOrderDate(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setStatus("PENDING");

        // 送料の計算と設定 (小計のみを考慮) 
        BigDecimal subtotal = BigDecimal.valueOf(cart.getTotalPrice()); //カート内の商品の合計金額(/intをBigDecimalに変換  )
        BigDecimal shippingFee = calculateShippingFee(subtotal);
        order.setShippingFee(shippingFee);

        // 合計金額の最終計算（小計 + 送料）
        order.setTotalPrice(subtotal.add(shippingFee));

        // --- 4. OrderDetailEntity の作成と在庫減算 ---
        for (CartItem cartItem : cart.getItems().values()) {
            ProductEntity product = productRepository.findById(cartItem.getProductId()).get(); // 前段のチェックで存在は保証されている

            OrderDetailEntity orderDetail = new OrderDetailEntity();
            orderDetail.setProduct(product); // ProductEntityを関連付け
            orderDetail.setProductName(product.getName());
            orderDetail.setQuantity(cartItem.getQuantity());
            orderDetail.setUnitPrice(product.getPrice());
            orderDetail.setCreatedAt(LocalDateTime.now());
            orderDetail.setUpdatedAt(LocalDateTime.now());

            order.addOrderDetail(orderDetail);

            int updatedRows = productRepository.decreaseStock(product.getProductId(), cartItem.getQuantity());
            if (updatedRows != 1) {
                throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_FAILURE,
                    "商品 " + product.getName() + " の在庫更新に失敗しました。時間をおいて再度お試しください。");
            }
        }

        // --- 5. 注文の保存 ---
        OrderEntity savedOrder = orderRepository.save(order);

        // --- 6. カートのクリア ---
        cartService.clearCart(session);

        // --- 7. OrderResponse DTOの作成と返却 ---
        // 注文確定後にpaymentMethod, customerInfo, items を表示するための修正

        // OrderItemDetailResponse のリストを構築
        List<OrderItemDetailResponse> responseItems = savedOrder.getOrderDetails().stream()
            .map(detail -> {
                ProductEntity product = detail.getProduct(); // OrderDetailEntityにProductEntityが関連付けられていることを前提
                return new OrderItemDetailResponse(
                    product.getProductId(),
                    detail.getProductName(),
                    product.getImageUrl(), // ProductEntityにgetImageUrl()メソッドが必要
                    detail.getQuantity(),
                    detail.getUnitPrice(),
                    detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()))
                );
            })
            .collect(Collectors.toList());

        // CustomerInfo は orderRequest から直接取得したものをそのまま利用
        CustomerInfo finalCustomerInfo = orderRequest.getCustomerInfo();

        return new OrderResponse(
            savedOrder.getOrderId(),
            savedOrder.getOrderDate(),
            savedOrder.getTotalPrice(), // OrderResponseの totalAmount に対応
            savedOrder.getShippingFee(),
            savedOrder.getPaymentMethod(),
            savedOrder.getStatus(),
            responseItems,
            finalCustomerInfo // orderRequestから受け取ったCustomerInfoをそのまま渡す
        );
    }

    /**
     * 配送料を計算するロジック（小計のみを考慮）。
     *
     * @param subtotal 商品の合計金額（送料除く）
     * @return 計算された配送料
     */
    public BigDecimal calculateShippingFee(BigDecimal subtotal) {
        // 5000円以上で送料無料、それ以外は一律500円
        if (subtotal.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            return BigDecimal.ZERO; // 送料無料
        } else {
            return BigDecimal.valueOf(500); // 通常送料
        }
    }


    // --- 既存の注文履歴・詳細取得メソッドは変更なし ---

    /**
     * 顧客IDに基づいて注文履歴を取得する。
     */
    public List<OrderSummaryResponse> getOrderHistoryByCustomer(Integer customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "会員情報が見つかりません。"));

        List<OrderEntity> orders = orderRepository.findByCustomerOrderByOrderDateDesc(customer);

        return orders.stream()
            .map(order -> new OrderSummaryResponse(
                order.getOrderId(),
                order.getOrderDate(),
                order.getTotalPrice(),
                order.getStatus()
            ))
            .collect(Collectors.toList());
    }

    /**
     * 特定の注文の詳細情報を取得する。
     */
    public OrderDetailResponse getOrderDetail(Integer orderId) {
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "注文が見つかりません。"));

        // OrderDetailEntityはProductEntityへの関連付けがあることを前提とする
        List<OrderDetailEntity> orderDetails = order.getOrderDetails();

        // CustomerInfo を構築
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setCustomerId(order.getCustomer() != null ? order.getCustomer().getCustomerId() : null); // nullを許容
        customerInfo.setName(order.getOrderName());
        customerInfo.setEmail(order.getOrderEmail());
        customerInfo.setAddress(order.getOrderAddress());
        customerInfo.setPhoneNumber(order.getOrderPhoneNumber());

        List<OrderItemDetailResponse> itemDetails = orderDetails.stream()
            .map(detail -> {
                ProductEntity product = detail.getProduct(); // ここでProductEntityが取得できることを想定

                return new OrderItemDetailResponse(
                    product.getProductId(),
                    detail.getProductName(),
                    product.getImageUrl(),
                    detail.getQuantity(),
                    detail.getUnitPrice(),
                    detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()))
                );
            })
            .collect(Collectors.toList());

        return new OrderDetailResponse(
            order.getOrderId(),
            customerInfo,
            order.getOrderDate(),
            order.getShippingFee(),
            order.getTotalPrice(),
            order.getPaymentMethod(),
            order.getStatus(),
            itemDetails
        );
    }
}