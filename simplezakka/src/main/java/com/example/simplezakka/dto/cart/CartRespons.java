package com.example.simplezakka.dto.cart;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CartRespons implements Serializable {
    private Map<String, CartItemResponse> items = new LinkedHashMap<>();//カート内の商品のリスト化
    private int totalQuantity;
    private int totalPrice;
    
    public void addItem(CartItemResponse item) {
        String itemId = String.valueOf(item.getProductId());
        
        // 既存のアイテムがあれば数量を加算
        if (items.containsKey(itemId)) {
            CartItemResponse existingItem = items.get(itemId);
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            existingItem.setSubtotal(existingItem.getPrice() * existingItem.getQuantity());
        } else {
            // 新しいアイテムを追加
            item.setId(itemId);
            item.setSubtotal(item.getPrice() * item.getQuantity());
            items.put(itemId, item);
        }
        
        // 合計計算
        calculateTotals();
    }
    
    public void updateQuantity(String itemId, int quantity) {
        if (items.containsKey(itemId)) {
            CartItemResponse item = items.get(itemId);
            item.setQuantity(quantity);
            item.setSubtotal(item.getPrice() * quantity);
            calculateTotals();
        }
    }
    
    public void removeItem(String itemId) {
        items.remove(itemId);
        calculateTotals();
    }
    
    public void calculateTotals() {
        totalQuantity = 0;
        totalPrice = 0;
        
        for (CartItemResponse item : items.values()) {
            totalQuantity += item.getQuantity();
            totalPrice += item.getSubtotal();
        }
    }
}