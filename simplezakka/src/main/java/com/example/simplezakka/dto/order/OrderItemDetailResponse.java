package com.example.simplezakka.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data; 
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderItemDetailResponse {

    
    private Integer productId; 

    
    private String productName;

    
    private String imageUrl; 

    
    private Integer quantity;

    
    private BigDecimal unitPrice;

    
    private BigDecimal subtotal;
}