// =============================================================================
// OrderRepository.java
// =============================================================================
package com.example.simplezakka.repository;

import com.example.simplezakka.entity.OrderEntity;
import com.example.simplezakka.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {
    
    // 会員の注文履歴を取得（注文日時の降順）
    List<OrderEntity> findByCustomerOrderByOrderDateDesc(CustomerEntity customer);
    
    // 会員IDによる注文履歴取得
    @Query("SELECT o FROM Order o WHERE o.customer.customerId = :customerId ORDER BY o.orderDate DESC")
    List<OrderEntity> findByCustomerIdOrderByOrderDateDesc(@Param("customerId") Integer customerId);
    
    // 非会員注文の取得（メールアドレスベース）
    List<OrderEntity> findByOrderEmailAndIsGuestTrueOrderByOrderDateDesc(String orderEmail);
    
    // 注文ステータス別検索
    List<OrderEntity> findByStatusOrderByOrderDateDesc(String status);
    
    // 期間内注文検索
    List<OrderEntity> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 注文番号による検索
    Optional<OrderEntity> findByOrderId(Integer orderId);
    
    // 会員と非会員の注文数を取得
    @Query("SELECT COUNT(o) FROM Order o WHERE o.isGuest = :isGuest")
    long countByIsGuest(@Param("isGuest") Boolean isGuest);
}