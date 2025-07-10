// ===============================
// OrderDetail Entity
// ===============================
package com.example.ecsite.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_details")
@Data
@NoArgsConstructor
public class OrderDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderDetailId;
    
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;
    
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper method to calculate subtotal
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(new BigDecimal(quantity));
    }
}
