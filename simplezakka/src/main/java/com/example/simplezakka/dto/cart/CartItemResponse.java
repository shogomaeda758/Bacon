/* カートに入っている商品のデータ*/

package com.example.simplezakka.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse implements Serializable {
    private String id;
    private Integer productId;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private int quantity;
    private BigDecimal subtotal;
}