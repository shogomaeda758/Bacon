package com.example.simplezakka.dto.order;

import lombok.Data;
import lombok.AllArgsConstructor; // ★ この行を追加
import lombok.NoArgsConstructor;  // ★ 柔軟性のため、これも追加しておくのが良いプラクティス

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor // 引数なしのコンストラクタを自動生成
@AllArgsConstructor // ★ これが、全てのフィールドを引数にとるコンストラクタを生成し、今回のエラーを解決します
public class OrderDetailResponse {

    /** 注文ID */
    private Integer orderId;

    /** 注文日時 */
    private LocalDateTime orderDate;

    /** 送料 */
    private BigDecimal shippingFee;

    /** 合計金額（商品合計 + 送料） */
    private BigDecimal totalAmount; // OrderResponse との整合性を考慮し、totalAmount にしています

    /** 支払い方法 */
    private String paymentMethod;

    /** 注文ステータス。例: "PENDING", "CONFIRMED" */
    private String status;

    /** 注文されsた個々の商品アイテムのリスト */
    private List<OrderItemDetailResponse> items;

      /** 顧客情報（注文時の配送先・連絡先） */
    private CustomerInfo customerInfo;
}