// =============================================================================
// OrderRepository.java
// =============================================================================
package com.example.simplezakka.repository;

import com.example.simplezakka.entity.Order;
import com.example.simplezakka.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    // 会員の注文履歴を取得（注文日時の降順）
    List<Order> findByCustomerOrderByOrderDateDesc(Customer customer);
    
    // 会員IDによる注文履歴取得
    @Query("SELECT o FROM Order o WHERE o.customer.customerId = :customerId ORDER BY o.orderDate DESC")
    List<Order> findByCustomerIdOrderByOrderDateDesc(@Param("customerId") Integer customerId);
    
    // 非会員注文の取得（メールアドレスベース）
    List<Order> findByOrderEmailAndIsGuestTrueOrderByOrderDateDesc(String orderEmail);
    
    // 注文ステータス別検索
    List<Order> findByStatusOrderByOrderDateDesc(String status);
    
    // 期間内注文検索
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 注文番号による検索
    Optional<Order> findByOrderId(Integer orderId);
    
    // 会員と非会員の注文数を取得
    @Query("SELECT COUNT(o) FROM Order o WHERE o.isGuest = :isGuest")
    long countByIsGuest(@Param("isGuest") Boolean isGuest);
}