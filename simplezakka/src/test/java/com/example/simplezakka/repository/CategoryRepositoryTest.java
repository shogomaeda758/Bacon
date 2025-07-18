package com.example.simplezakka.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class CategoryRepositoryTest {

    @BeforeEach
    void setUp() {
        // ... (setUpメソッドの中身)
    }

    @Test
    @DisplayName("カテゴリ名検索: DBに該当Categoryが存在する → Optional<Category>が該当データで返る (正常)")
    void findByCategoryName_Exist_ReturnsCategory() {
        // ... (findByCategoryName_Exist_ReturnsCategoryメソッドの中身)
    }

    @Test
    @DisplayName("カテゴリ名検索（存在しない場合）: DBに該当Categoryが存在しない → Optional.empty()が返る (異常)")
    void findByCategoryName_NotExist_ReturnsEmpty() {
        // ... (findByCategoryName_NotExist_ReturnsEmptyメソッドの中身)
    }

    @Test
    @DisplayName("作成日時順取得: 複数のCategoryが異なるcreatedAtでDBに存在 → createdAtの昇順でList<Category>が返る (正常)")
    void findAllByOrderByCreatedAtAsc_ReturnsOrdered() {
        // ... (findAllByOrderByCreatedAtAsc_ReturnsOrderedメソッドの中身)
    }

    @Test
    @DisplayName("作成日時順取得: カテゴリが存在しない場合 → 空のリストが返る (正常)")
    void findAllByOrderByCreatedAtAsc_NoCategories_ReturnsEmptyList() {
        // ... (findAllByOrderByCreatedAtAsc_NoCategories_ReturnsEmptyListメソッドの中身)
    }

    @Test
    @DisplayName("カテゴリ重複チェック（存在）: 該当カテゴリ名がDBに存在 → trueが返る (正常)")
    void existsByCategoryName_Exists_ReturnsTrue() {
        // ... (existsByCategoryName_Exists_ReturnsTrueメソッドの中身)
    }

    @Test
    @DisplayName("カテゴリ重複チェック（なし）: 該当カテゴリ名がDBに存在しない → falseが返る (正常)")
    void existsByCategoryName_NotExists_ReturnsFalse() {
        // ... (existsByCategoryName_NotExists_ReturnsFalseメソッドの中身)
    }
}