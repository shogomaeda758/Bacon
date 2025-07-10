package com.example.simplezakka.dto.order;

import lombok.Data;
import lombok.AllArgsConstructor; // ★追加
import lombok.NoArgsConstructor; // ★追加：デフォルトコンストラクタも保持するため
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor // デフォルトコンストラクタが必要な場合
@AllArgsConstructor // 全てのフィールドを引数にとるコンストラクタを生成
public class OrderResponse {

    /** 注文ID */
    private Integer orderId;

    /** 注文日時 */
    private LocalDateTime orderDate;

    /** 合計金額（商品合計 + 送料） */
    private BigDecimal totalAmount;

    /** 送料 */
    private BigDecimal shippingFee;

    /** 支払い方法 */
    private String paymentMethod; // ★このフィールドは含める

    /** 注文ステータス。例: "PENDING", "CONFIRMED", "SHIPPED", "COMPLETED" など */
    private String status;

    /** 注文された商品詳細のリスト */
    private List<OrderItemDetailResponse> items; // ★このフィールドは含める

    /** 顧客情報（注文時の配送先・連絡先） */
    private CustomerInfo customerInfo; // ★このフィールドは含める
}