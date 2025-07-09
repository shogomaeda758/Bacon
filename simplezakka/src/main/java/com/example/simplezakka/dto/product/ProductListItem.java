package com.example.simplezakka.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListItem {
    private Integer productId;
    private String name;
    private Integer price;
    private String imageUrl;
}