package com.example.simplezakka.dto.order;

import com.example.simplezakka.dto.cart.CartRespons; // 必要に応じてインポート
import com.example.simplezakka.dto.order.CustomerInfo;
import com.example.simplezakka.dto.order.OrderItemDetailResponse;
import lombok.Data; // または@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructorなどを個別に追加
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data // @Dataが@NoArgsConstructorと@AllArgsConstructorを自動生成する場合もありますが、明示的に追加すると良いでしょう
public class OrderResponse {
    private Integer orderId;
    private LocalDateTime orderDate;
    private BigDecimal totalPrice;
    private BigDecimal shippingFee;
    private String paymentMethod;
    private String status;
    private List<OrderItemDetailResponse> items;
    private CustomerInfo customerInfo;
    private String message; // エラーメッセージ用
    public OrderResponse(String message) {
        this.message = message;
    }

    // 通常の成功レスポンス用のコンストラクタ
    public OrderResponse(Integer orderId, LocalDateTime orderDate, BigDecimal totalPrice,
                         BigDecimal shippingFee, String paymentMethod, String status,
                         List<OrderItemDetailResponse> items, CustomerInfo customerInfo, String message) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
        this.shippingFee = shippingFee;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.items = items;
        this.customerInfo = customerInfo;
        this.message = message;
    }
}