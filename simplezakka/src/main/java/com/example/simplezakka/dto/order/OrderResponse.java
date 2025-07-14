package com.example.simplezakka.dto.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data 
public class OrderResponse {
    private Integer orderId;
    private LocalDateTime orderDate;
    private BigDecimal totalPrice;
    private BigDecimal shippingFee;
    private String paymentMethod;
    private String status;
    private List<OrderItemDetailResponse> items;
    private CustomerInfo customerInfo;
    private String message; 
    public OrderResponse(String message) {
        this.message = message;
    }

    public OrderResponse(Integer orderId, LocalDateTime orderDate, BigDecimal totalPrice,
                         BigDecimal shippingFee, String paymentMethod, String status,
                         List<OrderItemDetailResponse> items, CustomerInfo customerInfo, String message) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
        this.shippingFee = shippingFee;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.items = items;
        this.customerInfo = customerInfo;
        this.message = message;
    }
}