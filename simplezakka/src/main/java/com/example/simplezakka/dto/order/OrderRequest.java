

package com.example.simplezakka.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    
    @NotNull(message = "顧客情報は必須です。")
    @Valid 
    private CustomerInfo customerInfo;

    @NotBlank(message = "支払い方法は必須です。")
    private String paymentMethod;

    @NotNull(message = "注文商品は必須です。")
    @Size(min = 1, message = "注文商品は1つ以上選択してください。")
    @Valid 
    private List<OrderItemRequest> items; 

    @NotNull(message = "商品合計は必須です。")
    
    private Integer totalPrice; 

    @NotNull(message = "送料は必須です。")
    
    private Integer shippingFee; 

    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest { 

        @NotNull(message = "商品IDは必須です。")
        private Long productId; 

        @NotBlank(message = "商品名は必須です。")
        private String name; 

        @NotNull(message = "数量は必須です。")
        private Integer quantity; 

        @NotNull(message = "単価は必須です。")
        
        private Integer price; 
    }
}