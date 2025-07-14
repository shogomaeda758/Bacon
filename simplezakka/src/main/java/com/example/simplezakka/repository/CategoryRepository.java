// =============================================================================
// CategoryRepository.java
// =============================================================================
package com.example.simplezakka.repository;

import com.example.simplezakka.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // カテゴリ名による検索
    Optional<Category> findByCategoryName(String categoryName);
    
    // 全カテゴリを作成日時順で取得
    List<Category> findAllByOrderByCreatedAtAsc();
    
    // カテゴリ名の重複チェック
    boolean existsByCategoryName(String categoryName);
}
