package com.example.simplezakka.dto.order;

import lombok.Data;
import lombok.AllArgsConstructor; // ★ この行を追加
import lombok.NoArgsConstructor;  // ★ 必要であれば、この行も追加

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor // 引数なしのコンストラクタを自動生成（もし必要なら）
@AllArgsConstructor // ★ 全てのフィールドを持つコンストラクタを自動生成 (これが今回のエラーを解決します)
public class OrderSummaryResponse {

    /** 注文ID */
    private Integer orderId;

    /** 注文日時 */
    private LocalDateTime orderDate;

    /** 合計金額（商品合計 + 送料） */
    private BigDecimal totalAmount;

    /** 注文ステータス。例: "PENDING", "CONFIRMED" など */
    private String status;
}