package com.example.simplezakka.dto.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailResponse {

    /** 注文ID */
    private Integer orderId;

    /** 注文日時 */
    private LocalDateTime orderDate;

    /** 合計金額（商品合計 + 送料） */
    private BigDecimal totalAmount;

    /** 送料 */
    private BigDecimal shippingFee;

    /** 支払い方法 */
    private String paymentMethod;

    /** 注文ステータス */
    private String status;

    /** 顧客情報（注文時の配送先・連絡先） */
    private CustomerInfo customerInfo;

    /** 注文された個々の商品詳細のリスト */
    private List<OrderItemDetailResponse> items;
}