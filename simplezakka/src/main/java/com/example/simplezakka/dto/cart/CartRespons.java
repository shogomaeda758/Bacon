package com.example.simplezakka.dto.cart;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CartRespons implements Serializable {
    private Map<String, CartItemResponse> items = new LinkedHashMap<>(); // カート内の商品のリスト化
    private int totalQuantity; // 数量なのでintでOK
    private BigDecimal totalPrice; // 合計金額なのでBigDecimalでOK

    // コンストラクタでBigDecimalフィールドを初期化（Lombokの@Dataで自動生成されるが、明示するとより安全）
    public CartRespons() {
        this.totalQuantity = 0;
        this.totalPrice = BigDecimal.ZERO; // ★BigDecimal.ZERO で初期化★
    }

    public void addItem(CartItemResponse item) {
        // CartItemResponseのIDは通常、UUIDなどでユニークに生成されますが、
        // ここではProduct IDをそのままキーとして使用している前提で進めます。
        // CartService側でnewItem.setId(itemId)が呼ばれていることを想定。
        String itemId = String.valueOf(item.getProductId()); 
        
        // 既存のアイテムがあれば数量を加算（または新しい数量で上書き）
        if (items.containsKey(itemId)) {
            CartItemResponse existingItem = items.get(itemId);
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            
            // ★BigDecimal * int の修正: multiply() メソッドを使用★
            existingItem.setSubtotal(existingItem.getPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
        } else {
            // 新しいアイテムを追加
            // itemIdが設定されていない可能性があるので、ここで設定を確実にする
            item.setId(itemId); 
            
            // ★BigDecimal * int の修正: multiply() メソッドを使用★
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            items.put(itemId, item);
        }
        
        // 合計計算
        calculateTotals();
    }
    
    public void updateQuantity(String itemId, int quantity) {
        if (items.containsKey(itemId)) {
            CartItemResponse item = items.get(itemId);
            item.setQuantity(quantity);
            
            // ★BigDecimal * int の修正: multiply() メソッドを使用★
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
            calculateTotals();
        }
    }
    
    public void removeItem(String itemId) {
        items.remove(itemId);
        calculateTotals();
    }
    
    public void calculateTotals() {
        totalQuantity = 0;
        totalPrice = BigDecimal.ZERO; // ★BigDecimal.ZERO で初期化★
        
        for (CartItemResponse item : items.values()) {
            totalQuantity += item.getQuantity(); // int同士の加算はOK
            // ★BigDecimal += BigDecimal の修正: add() メソッドを使用★
            totalPrice = totalPrice.add(item.getSubtotal()); 
        }
    }
}