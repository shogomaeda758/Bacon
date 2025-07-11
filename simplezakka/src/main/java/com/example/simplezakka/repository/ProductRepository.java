// =============================================================================
// ProductRepository.java
// =============================================================================
package com.example.simplezakka.repository;

import com.example.simplezakka.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {
    
    // カテゴリID別商品検索
    @Query("SELECT p FROM ProductEntity p WHERE p.category.categoryId = :categoryId")
    List<ProductEntity> findByCategory(@Param("categoryId") Integer categoryId);
    
    // 商品名での部分一致検索
    List<ProductEntity> findByNameContaining(String keyword);
    
    // おすすめ商品の取得
    List<ProductEntity> findByIsRecommendedTrue();
    
    // 在庫あり商品の取得
    List<ProductEntity> findByStockGreaterThan(Integer stock);
    
    // 価格範囲での検索
    List<ProductEntity> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    // 在庫減少処理（カスタムクエリ）
    @Modifying
    @Transactional
    @Query("UPDATE ProductEntity p SET p.stock = p.stock - :quantity WHERE p.productId = :productId AND p.stock >= :quantity")
    int decreaseStock(@Param("productId") Integer productId, @Param("quantity") Integer quantity);
    
    // 在庫更新処理
    @Modifying
    @Transactional
    @Query("UPDATE ProductEntity p SET p.stock = :newStock WHERE p.productId = :productId")
    int updateStock(@Param("productId") Integer productId, @Param("newStock") Integer newStock);
    
    // 価格更新処理
    @Modifying
    @Transactional
    @Query("UPDATE ProductEntity p SET p.price = :newPrice WHERE p.productId = :productId")
    int updatePrice(@Param("productId") Integer productId, @Param("newPrice") BigDecimal newPrice);
}