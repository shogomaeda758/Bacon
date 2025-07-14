package com.example.simplezakka.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品詳細用 DTO
 * 商品詳細画面で表示する詳細な商品情報を格納
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetail {
    private Integer productId;
    private String name;
    private Integer price;
    private String description;
    private Integer stock;
    private String imageUrl;
}