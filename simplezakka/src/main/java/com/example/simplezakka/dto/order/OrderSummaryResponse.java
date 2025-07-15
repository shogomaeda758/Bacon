package com.example.simplezakka.dto.order;

import lombok.Data;
import lombok.AllArgsConstructor; 
import lombok.NoArgsConstructor; 

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class OrderSummaryResponse {
    /** 注文ID */
    private Integer orderId;

    /** 注文日時 */
    private LocalDateTime orderDate;

    /** 合計金額（商品合計 + 送料） */
    private BigDecimal totalAmount;

    /** 注文ステータス*/
    private String status;
}