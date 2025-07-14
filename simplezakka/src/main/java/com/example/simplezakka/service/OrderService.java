package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.order.CustomerInfo;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.OrderItemDetailResponse;
import com.example.simplezakka.dto.order.OrderDetailResponse;
import com.example.simplezakka.dto.order.OrderSummaryResponse;

import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.entity.Order;
import com.example.simplezakka.entity.OrderDetail;
import com.example.simplezakka.entity.Product;

import com.example.simplezakka.repository.CustomerRepository;
import com.example.simplezakka.repository.OrderDetailRepository;
import com.example.simplezakka.repository.OrderRepository;
import com.example.simplezakka.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList; 
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CartService cartService;

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
    public OrderResponse placeOrder(CartRespons cart, OrderRequest orderRequest, HttpSession session) {
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("カートに商品がありません。");
        }
        CustomerInfo customerInfo = orderRequest.getCustomerInfo();
        if (customerInfo == null) {
            throw new IllegalArgumentException("顧客情報が不足しています。");
        }

        for (CartItemResponse cartItem : cart.getItems().values()) {
            Product product = productRepository.findById(cartItem.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + cartItem.getName()));

            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException(
                    "申し訳ございません、" + product.getName() + "の在庫が不足しています。現在の在庫: " + product.getStock());
            }
        }

        Order order = new Order();

        Integer customerId = customerInfo.getCustomerId();
        if (customerId != null && customerId != 0) {
            Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("会員情報が見つかりません。ID: " + customerId));
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

        for (CartItemResponse cartItem : cart.getItems().values()) {
            Product product = productRepository.findById(cartItem.getProductId()).orElseThrow(
                () -> new IllegalArgumentException("在庫確認後に商品が見つかりません: " + cartItem.getName())
            );

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

        cartService.clearCart(session);

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

        return new OrderResponse(
            savedOrder.getOrderId(),
            savedOrder.getOrderDate(),
            savedOrder.getTotalPrice(),
            savedOrder.getShippingFee(),
            savedOrder.getPaymentMethod(),
            savedOrder.getStatus(),
            responseItems,
            customerInfo,
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

    public List<OrderSummaryResponse> getOrderHistoryByCustomer(Integer customerId) {
        System.out.println("getOrderHistoryByCustomer called for customer ID: " + customerId);
        return List.of();
    }

    public OrderDetailResponse getOrderDetail(Integer orderId) {
        System.out.println("getOrderDetail called for order ID: " + orderId);

        List<OrderItemDetailResponse> dummyItems = new ArrayList<>();
        dummyItems.add(new OrderItemDetailResponse(
            101,
            "ダミー商品A",
            "http://example.com/dummyA.jpg",
            2,
            BigDecimal.valueOf(1500),
            BigDecimal.valueOf(3000)
        ));
        dummyItems.add(new OrderItemDetailResponse(
            102,
            "ダミー商品B",
            "http://example.com/dummyB.jpg",
            1,
            BigDecimal.valueOf(2500),
            BigDecimal.valueOf(2500)
        ));

        CustomerInfo dummyCustomerInfo = new CustomerInfo();
        dummyCustomerInfo.setCustomerId(null);
        dummyCustomerInfo.setName("ダミーゲスト太郎");
        dummyCustomerInfo.setEmail("guest@example.com");
        dummyCustomerInfo.setAddress("東京都港区ダミー1-2-3");
        dummyCustomerInfo.setPhoneNumber("090-1234-5678");

        return new OrderDetailResponse(
            orderId,
            LocalDateTime.now().minusDays(5),
            BigDecimal.valueOf(5500),
            BigDecimal.ZERO,
            "クレジットカード",
            "COMPLETED",
            dummyItems,
            dummyCustomerInfo
        );
    }
}
