package com.example.simplezakka.dto.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Integer orderId;
    private LocalDateTime orderDate;
    private BigDecimal totalPrice; // 商品合計
    private BigDecimal shippingFee; // 送料
    private BigDecimal grandTotal;  // 送料込みの最終合計金額
    private String paymentMethod;
    private String status;
    private List<OrderItemDetailResponse> items;
    private CustomerInfo customerInfo;
    private String message;

    public OrderResponse(String message) {
        this.message = message;
        this.totalPrice = BigDecimal.ZERO;
        this.shippingFee = BigDecimal.ZERO;
        this.grandTotal = BigDecimal.ZERO;
    }

    public OrderResponse(
        Integer orderId,
        LocalDateTime orderDate,
        BigDecimal totalPrice,
        BigDecimal shippingFee,
        BigDecimal grandTotal,
        String paymentMethod,
        String status,
        List<OrderItemDetailResponse> items,
        CustomerInfo customerInfo,
        String message
    ) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
        this.shippingFee = shippingFee;
        this.grandTotal = grandTotal;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.items = items;
        this.customerInfo = customerInfo;
        this.message = message;
    }
}