package com.example.simplezakka.dto.order;

import lombok.Data;
import lombok.AllArgsConstructor; // ★追加
import lombok.NoArgsConstructor; // ★追加：デフォルトコンストラクタも保持するため
import java.math.BigDecimal;

@Data
@NoArgsConstructor // 引数なしのコンストラクタを自動生成
@AllArgsConstructor // 全てのフィールドを持つコンストラクタを自動生成
public class OrderRequest {

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