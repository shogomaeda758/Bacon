package com.example.simplezakka.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data; 
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderItemDetailResponse {

    /** 商品ID */
    private Integer productId; 

    /** 商品名 */
    private String productName;

    /** 商品画像URL */
    private String imageUrl; 

    /** 数量 */
    private Integer quantity;

    /** 単価 */
    private BigDecimal unitPrice;

    /** 小計（単価 * 数量） */
    private BigDecimal subtotal;
}