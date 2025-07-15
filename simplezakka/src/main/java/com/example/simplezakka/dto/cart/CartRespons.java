package com.example.simplezakka.dto.cart;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CartRespons implements Serializable {
    private Map<String, CartItemResponse> items = new LinkedHashMap<>();
    private int totalQuantity;
    private BigDecimal totalPrice;
    private BigDecimal shippingFee;
    private BigDecimal grandTotal;

    public CartRespons() {
        this.totalQuantity = 0;
        this.totalPrice = BigDecimal.ZERO;
        this.shippingFee = BigDecimal.ZERO;
        this.grandTotal = BigDecimal.ZERO;
    }

    public void addItem(CartItemResponse item) {
        String itemId = String.valueOf(item.getProductId());

        if (items.containsKey(itemId)) {
            CartItemResponse existingItem = items.get(itemId);
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            existingItem.setSubtotal(existingItem.getPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
        } else {
            item.setId(itemId);
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            items.put(itemId, item);
        }
        calculateTotals();
    }

    public void updateQuantity(String itemId, int quantity) {
        if (items.containsKey(itemId)) {
            CartItemResponse item = items.get(itemId);
            item.setQuantity(quantity);
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
        totalPrice = BigDecimal.ZERO;

        for (CartItemResponse item : items.values()) {
            totalQuantity += item.getQuantity();
            totalPrice = totalPrice.add(item.getSubtotal());
        }

        this.shippingFee = calculateShippingFeeInternal(this.totalPrice);
        this.grandTotal = this.totalPrice.add(this.shippingFee);
    }

    private BigDecimal calculateShippingFeeInternal(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            return BigDecimal.ZERO;
        } else {
            return BigDecimal.valueOf(500);
        }
    }
}