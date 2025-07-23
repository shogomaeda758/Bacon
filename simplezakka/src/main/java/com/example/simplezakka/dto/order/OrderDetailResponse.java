package com.example.simplezakka.dto.order;

import lombok.Data;
import lombok.AllArgsConstructor; 
import lombok.NoArgsConstructor; 

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class OrderDetailResponse {

    
    private Integer orderId;

    
    private LocalDateTime orderDate;

    
    private BigDecimal shippingFee;

    
    private BigDecimal totalAmount; 

    
    private String paymentMethod;

    
    private String status;

    
    private List<OrderItemDetailResponse> items;


}