package com.example.simplezakka.dto.product;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListItem {
    public ProductListItem(Integer productId2, String name2, BigDecimal price2, String imageUrl2) {
        //TODO Auto-generated constructor stub
    }
    private Integer productId;
    private String name;
    private Integer price;
    private String imageUrl;
}