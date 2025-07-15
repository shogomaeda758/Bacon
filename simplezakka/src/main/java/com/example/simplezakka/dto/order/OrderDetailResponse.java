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

    /** 注文ID */
    private Integer orderId;

    /** 注文日時 */
    private LocalDateTime orderDate;

    /** 送料 */
    private BigDecimal shippingFee;

    /** 合計金額（商品合計 + 送料） */
    private BigDecimal totalAmount; 

    /** 支払い方法 */
    private String paymentMethod;

    /** 注文ステータス
    private String status;

    /** 注文されsた個々の商品アイテムのリスト */
    private List<OrderItemDetailResponse> items;

      /** 顧客情報 */
    private CustomerInfo customerInfo;
}