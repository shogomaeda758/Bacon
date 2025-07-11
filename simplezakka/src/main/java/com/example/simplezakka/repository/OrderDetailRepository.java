// =============================================================================
// OrderDetailRepository.java
// =============================================================================
package com.example.simplezakka.repository;

import com.example.simplezakka.entity.OrderDetailEntity;
import com.example.simplezakka.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetailEntity, Integer> {
    
    // 注文IDによる注文明細取得
    List<OrderDetailEntity> findByOrderOrderIdOrderByOrderDetailIdAsc(OrderEntity order);
    
    // 注文IDによる注文明細取得（Order entityを使わない版）
    @Query("SELECT od FROM OrderDetailEntity od WHERE od.order.orderId = :orderId ORDER BY od.orderDetailId ASC")
    List<OrderDetailEntity> findByOrderIdOrderByOrderDetailIdAsc(@Param("orderId") Integer orderId);
    
    // 商品IDによる注文明細検索（売上分析等で使用）
    @Query("SELECT od FROM OrderDetailEntity od WHERE od.product.productId = :productId")
    List<OrderDetailEntity> findByProductId(@Param("productId") Integer productId);
    
    // 商品の売上数量合計取得
    @Query("SELECT SUM(od.quantity) FROM OrderDetailEntity od WHERE od.product.productId = :productId")
    Integer sumQuantityByProductId(@Param("productId") Integer productId);
    
    // 注文明細の一括保存（デフォルトのsaveAllを使用可能だが、明示的に定義）
    @Override
    <S extends OrderDetailEntity> List<S> saveAll(Iterable<S> entities);
}