package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.order.CustomerInfo; 
import com.example.simplezakka.dto.order.OrderRequest; 
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.OrderItemDetailResponse; 

import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.entity.Order;
import com.example.simplezakka.entity.OrderDetail; 
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
        if (customerInfoFromRequest != null) {
        System.out.println("OrderService received customerId from OrderRequest: " + customerInfoFromRequest.getCustomerId());
    } else {
        System.out.println("OrderService received customerInfoFromRequest as null.");
    }
    
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

        
        
        Integer customerId = customerInfoFromRequest.getCustomerId(); 
        if (customerId != null) { 
            Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("会員情報が見つかりません。ID: " + customerId));
            order.setCustomer(customer); 
            order.setIsGuest(false); 
        } else { 
            order.setCustomer(null); 
            order.setIsGuest(true); 
        }

        
        order.setOrderName(customerInfoFromRequest.getName());
        order.setOrderEmail(customerInfoFromRequest.getEmail());
        order.setOrderAddress(customerInfoFromRequest.getAddress());
        order.setOrderPhoneNumber(customerInfoFromRequest.getPhoneNumber());

        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        
        BigDecimal productSubtotal = cart.getTotalPrice(); 
        BigDecimal shippingFee = calculateShippingFee(productSubtotal);
        BigDecimal orderGrandTotal = productSubtotal.add(shippingFee); 

        order.setShippingFee(shippingFee);
        order.setTotalPrice(productSubtotal); 
        
        
        


        
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

        
        CustomerInfo responseCustomerInfo = new CustomerInfo(
            savedOrder.getCustomer() != null ? savedOrder.getCustomer().getCustomerId() : null, 
            savedOrder.getOrderName(),
            savedOrder.getOrderEmail(),
            savedOrder.getOrderAddress(),
            savedOrder.getOrderPhoneNumber()
        );


        return new OrderResponse(
            savedOrder.getOrderId(),
            savedOrder.getOrderDate(),
            savedOrder.getTotalPrice(), 
            savedOrder.getShippingFee(),
            
            
            savedOrder.getTotalPrice().add(savedOrder.getShippingFee()), 
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