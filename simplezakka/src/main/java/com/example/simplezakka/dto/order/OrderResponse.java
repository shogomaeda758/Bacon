package com.example.simplezakka.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ★ 独立した CustomerInfo をインポート ★
import com.example.simplezakka.dto.order.CustomerInfo;
// ★ 独立した OrderItemDetailResponse をインポート ★
import com.example.simplezakka.dto.order.OrderItemDetailResponse;


@Data
@NoArgsConstructor
// Lombok の @AllArgsConstructor が全てのフィールドを引数に取るコンストラクタを生成します
@AllArgsConstructor
public class OrderResponse {

    private Integer orderId;
    private LocalDateTime orderDate;
    private BigDecimal totalPrice; // 商品合計
    private BigDecimal shippingFee;
    private BigDecimal grandTotal; // 送料込みの最終合計
    private String paymentMethod;
    private String status;
    private List<OrderItemDetailResponse> items; // ★ 独立した OrderItemDetailResponse を参照 ★
    private CustomerInfo customerInfo; // ★ 独立した CustomerInfo を参照 ★
    private String message;

    // エラーメッセージ専用コンストラクタ (これは既存のままでOK)
    public OrderResponse(String message) {
        this.message = message;
    }

    // Lombok の @AllArgsConstructor があれば、このフルコンストラクタは手動で書かなくても良い
    // もし Lombok がうまく機能しない場合や、特定のコンストラクタが必要な場合は以下を手動で定義
    /*
    public OrderResponse(
            Integer orderId,
            LocalDateTime orderDate,
            BigDecimal totalPrice,
            BigDecimal shippingFee,
            BigDecimal grandTotal,
            String paymentMethod,
            String status,
            List<OrderItemDetailResponse> items,
            CustomerInfo customerInfo, // ここで独立した CustomerInfo を受け取る
            String message) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
        this.shippingFee = shippingFee;
        this.grandTotal = grandTotal;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.items = items;
        this.customerInfo = customerInfo;
        this.message = message;
    }
    */
}