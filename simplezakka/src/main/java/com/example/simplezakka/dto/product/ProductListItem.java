package com.example.simplezakka.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品一覧用 DTO
 * 商品一覧画面で表示する最小限の商品情報を格納
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListItem {
    private Integer productId;
    private String name;
    private Integer price;
    private String imageUrl;
    private String categoryName;
}