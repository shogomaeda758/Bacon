package com.example.simplezakka.dto.order;

import lombok.Data;
import jakarta.validation.Valid; // CustomerInfoに@Validを使う場合、これも必要
import jakarta.validation.constraints.NotBlank; // ★このインポートを追加！
import jakarta.validation.constraints.NotNull; // ★必要であればCustomerInfoにも追加！

/**
 * 注文リクエストDTO。
 * クライアントから注文情報を受け取るためのデータ構造。
 */
@Data
public class OrderRequest {
    @NotNull(message = "顧客情報は必須です。") // CustomerInfoがnullでないことを保証
    @Valid // CustomerInfo内のバリデーションも有効にする場合
    private CustomerInfo customerInfo;

    @NotBlank(message = "支払い方法は必須です。") // ★これを追加！
    private String paymentMethod;
}