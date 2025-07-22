package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.order.CustomerInfo; // OrderRequest 内の CustomerInfo を使用
import com.example.simplezakka.dto.order.OrderRequest; // 修正した OrderRequest を使用
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.OrderItemDetailResponse; // OrderResponse の内部クラスをインポート

import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.entity.Order;
import com.example.simplezakka.entity.OrderDetail; // OrderDetailエンティティを使用
import com.example.simplezakka.entity.Product;

import com.example.simplezakka.repository.CustomerRepository;
import com.example.simplezakka.repository.OrderRepository;
import com.example.simplezakka.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CartService cartService;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            CartService cartService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.cartService = cartService;
    }

    @Transactional
    public OrderResponse placeOrder(CartRespons cart, OrderRequest orderRequest) {
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("カートに商品がありません。");
        }
        
        CustomerInfo customerInfoFromRequest = orderRequest.getCustomerInfo();
        if (customerInfoFromRequest == null) {
            throw new IllegalArgumentException("顧客情報が不足しています。");
        }

        Map<Integer, Product> productsInCart = new HashMap<>();

        for (CartItemResponse cartItem : cart.getItems().values()) {
            Product product = productRepository.findById(cartItem.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + cartItem.getName()));

            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException(
                    "申し訳ございません、" + product.getName() + "の在庫が不足しています。現在の在庫: " + product.getStock());
            }
            productsInCart.put(product.getProductId(), product);
        }

        Order order = new Order();

        // --- isGuest の判断と Customer / customer_id の設定ロジック ---
        // ★修正: customerId を OrderRequest ではなく CustomerInfo から取得する
        Integer customerId = customerInfoFromRequest.getCustomerId(); 
        if (customerId != null) { // customerId が null でない場合 (会員の注文)
            Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("会員情報が見つかりません。ID: " + customerId));
            order.setCustomer(customer); // OrderエンティティにCustomerエンティティを関連付け
            order.setIsGuest(false); // 会員フラグをfalseに設定
        } else { // customerId が null の場合 (非会員の注文)
            order.setCustomer(null); // 会員情報はnull
            order.setIsGuest(true); // ゲストフラグをtrueに設定
        }

        // --- 注文者情報のマッピング (OrderRequestのcustomerInfoから取得) ---
        order.setOrderName(customerInfoFromRequest.getName());
        order.setOrderEmail(customerInfoFromRequest.getEmail());
        order.setOrderAddress(customerInfoFromRequest.getAddress());
        order.setOrderPhoneNumber(customerInfoFromRequest.getPhoneNumber());

        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        // --- 金額計算 ---
        BigDecimal productSubtotal = cart.getTotalPrice(); // カートから商品小計を取得
        BigDecimal shippingFee = calculateShippingFee(productSubtotal);
        BigDecimal orderGrandTotal = productSubtotal.add(shippingFee); // 送料込みの最終合計

        order.setShippingFee(shippingFee);
        order.setTotalPrice(productSubtotal); // OrderエンティティのtotalPriceには商品合計を設定
        // Orderエンティティに grandTotal フィールドがあれば設定
        // private BigDecimal grandTotal; をエンティティに追加した場合
        // order.setGrandTotal(orderGrandTotal); 


        // 注文詳細の生成と在庫の更新
        for (CartItemResponse cartItem : cart.getItems().values()) {
            Product product = productsInCart.get(cartItem.getProductId());
            if (product == null) {
                throw new IllegalStateException("予期せぬエラー: カート内の商品情報が不足しています。商品ID: " + cartItem.getProductId());
            }

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setProduct(product);
            orderDetail.setUnitPrice(product.getPrice());
            orderDetail.setQuantity(cartItem.getQuantity());
            order.addOrderDetail(orderDetail);

            int updatedRows = productRepository.decreaseStock(product.getProductId(), cartItem.getQuantity());
            if (updatedRows != 1) {
                throw new IllegalStateException(
                    "商品 " + product.getName() + " の在庫更新に失敗しました。時間をおいて再度お試しください。");
            }
        }

        Order savedOrder = orderRepository.save(order);

        // --- OrderResponse の作成 ---
        List<OrderItemDetailResponse> responseItems = savedOrder.getOrderDetails().stream()
            .map(detail -> {
                Product product = detail.getProduct(); 
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

        // OrderResponse の customerInfo に customerId を含める
        CustomerInfo responseCustomerInfo = new CustomerInfo(
            savedOrder.getCustomer() != null ? savedOrder.getCustomer().getCustomerId() : null, // 会員IDがあればセット、なければnull
            savedOrder.getOrderName(),
            savedOrder.getOrderEmail(),
            savedOrder.getOrderAddress(),
            savedOrder.getOrderPhoneNumber()
        );


        return new OrderResponse(
            savedOrder.getOrderId(),
            savedOrder.getOrderDate(),
            savedOrder.getTotalPrice(), // 商品合計
            savedOrder.getShippingFee(),
            // savedOrder.getGrandTotal(), // Orderエンティティに grandTotal フィールドがあれば使用
            // ない場合は計算値を使用
            savedOrder.getTotalPrice().add(savedOrder.getShippingFee()), // totalPrice と shippingFee から計算
            savedOrder.getPaymentMethod(),
            savedOrder.getStatus(),
            responseItems,
            responseCustomerInfo,
            "注文が正常に完了しました。"
        );
    }

    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            return BigDecimal.ZERO;
        } else {
            return BigDecimal.valueOf(500);
        }
    }
}