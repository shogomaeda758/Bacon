// src/main/java/com/example/simplezakka/dto/order/OrderRequest.java

package com.example.simplezakka.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.math.BigDecimal; // totalPrice, shippingFee を BigDecimal にする場合

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    // 既存の CustomerInfo への参照
    @NotNull(message = "顧客情報は必須です。")
    @Valid // CustomerInfo 内のバリデーションも有効にする
    private CustomerInfo customerInfo;

    @NotBlank(message = "支払い方法は必須です。")
    private String paymentMethod;

    // --- ここから変更するフィールド ---

    // ★削除: customerId を CustomerInfo に移管するため、ここから削除します
    // private Integer customerId; 

    @NotNull(message = "注文商品は必須です。")
    @Size(min = 1, message = "注文商品は1つ以上選択してください。")
    @Valid // OrderItemRequest 内のバリデーションも有効にする
    private List<OrderItemRequest> items; // ★内部クラス OrderItemRequest のリスト

    @NotNull(message = "商品合計は必須です。")
    // ★推奨: Integer から BigDecimal へ変更することを検討
    private Integer totalPrice; // ★商品合計金額

    @NotNull(message = "送料は必須です。")
    // ★推奨: Integer から BigDecimal へ変更することを検討
    private Integer shippingFee; // ★送料

    // --- ここから内部クラスの定義 ---

    // OrderItemRequest を OrderRequest の内部クラスとして定義
    // このクラスは OrderRequest.java の中に書かれますが、独立したファイルは不要です。
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest { // static class にすることで、外部クラスのインスタンスなしで利用可能

        @NotNull(message = "商品IDは必須です。")
        private Long productId; // 商品ID

        @NotBlank(message = "商品名は必須です。")
        private String name; // 商品名

        @NotNull(message = "数量は必須です。")
        private Integer quantity; // 数量

        @NotNull(message = "単価は必須です。")
        // ★推奨: Integer から BigDecimal へ変更することを検討
        private Integer price; // 単価
    }
}