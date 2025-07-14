package com.example.simplezakka.dto.order;

import lombok.Data;

/**
 * 注文リクエストDTO。
 * クライアントから注文情報を受け取るためのデータ構造。
 */
@Data
public class OrderRequest {
    private CustomerInfo customerInfo; 
    private String paymentMethod;
    
    
}