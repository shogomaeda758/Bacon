package com.example.simplezakka.dto.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderSummaryResponse {

    /** 注文ID */
    private Integer orderId;

    /** 注文日時 */
    private LocalDateTime orderDate;

    /** 合計金額（商品合計 + 送料） */
    private BigDecimal totalAmount; // totalPriceからtotalAmountに名称変更し、送料込みの最終金額を示す

    /** 注文ステータス。例: "PENDING", "CONFIRMED" など */
    private String status;
}