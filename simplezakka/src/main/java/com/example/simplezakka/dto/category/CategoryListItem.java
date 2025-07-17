package com.example.simplezakka.dto.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * カテゴリー一覧用 DTO
 * カテゴリー選択画面で表示するカテゴリー情報を格納
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryListItem {
    private Integer categoryId;
    private String categoryName;
}