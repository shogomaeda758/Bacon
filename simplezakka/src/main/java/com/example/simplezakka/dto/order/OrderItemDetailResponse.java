package com.example.simplezakka.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data; 
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderItemDetailResponse {

    /** 商品ID */
    private Integer productId; // 追加：商品特定のため

    /** 商品名 */
    private String productName;

    /** 商品画像URL */
    private String imageUrl; // 追加：詳細表示に便利

    /** 数量 */
    private Integer quantity;

    /** 単価 */
    private BigDecimal unitPrice;

    /** 小計（単価 * 数量） */
    private BigDecimal subtotal;
}