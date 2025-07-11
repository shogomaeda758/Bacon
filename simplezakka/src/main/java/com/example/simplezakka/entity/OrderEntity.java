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

@Entity(name = "Order")
@Table(name = "orders")
@Data
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;
    
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
    
    @Column(name="is_guest",nullable = false)
    private Boolean isGuest;
    
    public Boolean getIsGuest() {
    return isGuest;
    }

    public void setIsGuest(Boolean isGuest) {
        this.isGuest = isGuest;
    }
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetailEntity> orderDetails = new ArrayList<>();
    
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
    public void addOrderDetail(OrderDetailEntity orderDetail) {
        orderDetails.add(orderDetail);
        orderDetail.setOrder(this);
    }
    
    // Helper method to calculate total
    public BigDecimal calculateTotal() {
        BigDecimal subtotal = orderDetails.stream()
                .map(OrderDetailEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.add(shippingFee);

    
    }

    public void setTotalAmount(int totalPrice2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTotalAmount'");
    }

    public void setShippingAddress(String address) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setShippingAddress'");
    }

    public void setShippingPhoneNumber(String phoneNumber) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setShippingPhoneNumber'");
    }

    public void setCustomerName(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCustomerName'");
    }
}
