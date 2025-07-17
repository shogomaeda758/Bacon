package com.example.simplezakka.controller;

import com.example.simplezakka.dto.category.CategoryListItem;
import com.example.simplezakka.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * カテゴリー取得REST APIコントローラー
 * カテゴリーの取得機能のエンドポイントを提供
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    
    /**
     * 全カテゴリー取得
     * GET /api/categories
     * @return 全カテゴリーリスト
     */
    @GetMapping
    public ResponseEntity<List<CategoryListItem>> getAllCategories() {
        List<CategoryListItem> categories = categoryService.findAllCategories();
        return ResponseEntity.ok(categories);
    }
}
