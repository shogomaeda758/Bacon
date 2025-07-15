// ===============================
// Order Entity
// ===============================
package com.example.simplezakka.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    @Column(nullable = false)
    private String orderEmail;
    
    @Column(nullable = false)
    private String orderName;
    
    @Column(nullable = false)
    private String orderPhoneNumber;
    
    @Column(nullable = false)
    private String orderAddress;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;
    
    @Column(nullable = false)
    private String paymentMethod;
    
    @Column(nullable = false)
    private LocalDateTime orderDate;
    
    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false)
    private Boolean isGuest;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();
    
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
    
    // Helper method to add order detail
    public void addOrderDetail(OrderDetail orderDetail) {
        orderDetails.add(orderDetail);
        orderDetail.setOrder(this);
    }
    
    public BigDecimal calculateTotal() {
        BigDecimal subtotal = orderDetails.stream()
                .map(OrderDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.add(shippingFee);

    
    }
}
