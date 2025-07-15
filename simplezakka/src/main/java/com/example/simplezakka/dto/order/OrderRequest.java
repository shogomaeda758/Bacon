package com.example.simplezakka.dto.order;

import lombok.Data;
import jakarta.validation.Valid; 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull; 

@Data
public class OrderRequest {
    @NotNull(message = "顧客情報は必須です。") 
    @Valid
    private CustomerInfo customerInfo;

    @NotBlank(message = "支払い方法は必須です。") 
    private String paymentMethod;
}