package com.example.simplezakka.dto.order;

import lombok.Data;
import lombok.NoArgsConstructor; 
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor // デフォルトコンストラクタが必要な場合
 // 全てのフィールドを引数にとるコンストラクタを生成
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

    /** メッセージ（成功時やエラー時など） */
    private String message; 

    // 1. エラーメッセージなどを渡すためのコンストラクタ (String一つ)
    public OrderResponse(String message) {
        this.message = message;
    }

    // 2. 注文確定成功時に全ての注文詳細を返すためのコンストラクタ (placeOrderメソッドの戻り値用)
    // message フィールドを最後に含めます
    public OrderResponse(Integer orderId, LocalDateTime orderDate, BigDecimal totalAmount, BigDecimal shippingFee,
                         String paymentMethod, String status, List<OrderItemDetailResponse> items,
                         CustomerInfo customerInfo, String message) { // messageフィールドも引数に追加
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.shippingFee = shippingFee;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.items = items;
        this.customerInfo = customerInfo;
        this.message = message; // messageも初期化
     }
}