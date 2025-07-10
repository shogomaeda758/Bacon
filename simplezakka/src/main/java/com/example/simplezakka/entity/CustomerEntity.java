// ===============================
// Customer Entity
// ===============================
package com.example.simplezakka.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer customerId;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Column(nullable = false)
    private String address;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderEntity> orders = new ArrayList<>();
    
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
    
    // Helper method to add order
    public void addOrder(OrderEntity order) {
        orders.add(order);
        order.setCustomer(this);
    }
}
