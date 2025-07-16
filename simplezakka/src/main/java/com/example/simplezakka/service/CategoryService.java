package com.example.simplezakka.service;

import com.example.simplezakka.dto.category.CategoryListItem;
import com.example.simplezakka.entity.Category;
import com.example.simplezakka.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * カテゴリーサービス
 * カテゴリーの検索、取得、変換処理を行う
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * 全カテゴリー取得
     * @return 全カテゴリーリスト
     */
    public List<CategoryListItem> findAllCategories() {
        return categoryRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(this::convertToListItem)
                .collect(Collectors.toList());
    }
    
    /**
     * カテゴリーエンティティをDTOに変換
     * @param category カテゴリーエンティティ
     * @return カテゴリーリストアイテム
     */
    private CategoryListItem convertToListItem(Category category) {
        return new CategoryListItem(
                category.getCategoryId(),
                category.getCategoryName()
        );
    }
}