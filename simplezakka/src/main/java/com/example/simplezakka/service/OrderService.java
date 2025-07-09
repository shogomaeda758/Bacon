package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.Cart;
import com.example.simplezakka.dto.cart.CartItem;
import com.example.simplezakka.dto.order.CustomerInfo;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.entity.Order;
import com.example.simplezakka.entity.OrderDetail;
import com.example.simplezakka.entity.Product;
import com.example.simplezakka.repository.OrderDetailRepository;
import com.example.simplezakka.repository.OrderRepository;
import com.example.simplezakka.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            ProductRepository productRepository,
            CartService cartService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
    }

    @Transactional
    public OrderResponse placeOrder(Cart cart, OrderRequest orderRequest, HttpSession session) {
        if (cart == null || cart.getItems().isEmpty()) {
            return null;
        }

        // 在庫確認
        for (CartItem cartItem : cart.getItems().values()) {
            Optional<Product> productOpt = productRepository.findById(cartItem.getProductId());
            if (productOpt.isEmpty() || productOpt.get().getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("在庫不足または商品未存在: " + cartItem.getName());
            }
        }

        // 注文エンティティ作成
        Order order = new Order();
        CustomerInfo customerInfo = orderRequest.getCustomerInfo();
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setCustomerName(customerInfo.getName());
        order.setCustomerEmail(customerInfo.getEmail());
        order.setShippingAddress(customerInfo.getAddress());
        order.setShippingPhoneNumber(customerInfo.getPhoneNumber());
        order.setStatus("PENDING");

        // 注文明細作成と在庫減算
        for (CartItem cartItem : cart.getItems().values()) {
            Product product = productRepository.findById(cartItem.getProductId()).orElseThrow(
                () -> new IllegalStateException("在庫確認後に商品が見つかりません: " + cartItem.getName())
            );

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setProduct(product);
            orderDetail.setProductName(product.getName());
            orderDetail.setPrice(product.getPrice());
            orderDetail.setQuantity(cartItem.getQuantity());

            order.addOrderDetail(orderDetail);

            // 在庫減算処理と結果のチェック
            int updatedRows = productRepository.decreaseStock(product.getProductId(), cartItem.getQuantity());

            // 更新された行数が1でない場合（在庫更新に失敗した場合）
            if (updatedRows != 1) {
                throw new IllegalStateException(
                    "在庫の更新に失敗しました (更新行数: " + updatedRows + ")。" +
                    "商品ID: " + product.getProductId() +
                    ", 商品名: " + product.getName() +
                    ", 要求数量: " + cartItem.getQuantity()
                    // 必要であれば、考えられる原因（競合など）を示すメッセージを追加
                );
            }
        }

        // 注文保存
        Order savedOrder = orderRepository.save(order);

        // カートクリア
        cartService.clearCart(session);

        return new OrderResponse(savedOrder.getOrderId(), savedOrder.getOrderDate());
    }
}