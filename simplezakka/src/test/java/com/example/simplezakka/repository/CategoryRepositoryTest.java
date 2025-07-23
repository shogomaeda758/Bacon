package com.example.simplezakka.repository;

import com.example.simplezakka.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;
    private Category category1;
    private Category category2;
    private Category category3;
    private Category category4;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        category1 = new Category(null, "interior", null, null);
        category2 = new Category(null, "homeGoods", null, null);
        category3 = new Category(null, "fashion", null, null);
        category4 = new Category(null,"kitchen", null, null);

        categoryRepository.save(category1);
        categoryRepository.save(category2);
        categoryRepository.save(category3);
        categoryRepository.save(category4);
    }

    @Test
    @DisplayName("カテゴリ名検索: DBに該当Categoryが存在する → Optional<Category>が該当データで返る (正常)")
    void findByCategoryName_Exist_ReturnsCategory() {
        String existingCategoryName = "Electronics";

        Optional<Category> foundCategoryOptional = categoryRepository.findByCategoryName(existingCategoryName);

        assertThat(foundCategoryOptional).isPresent();
        Category foundCategory = foundCategoryOptional.get();
        assertThat(foundCategory.getCategoryName()).isEqualTo(existingCategoryName);
        assertThat(foundCategory.getCategoryId()).isNotNull(); // IDはInteger型なのでisNotNull()でOK
        assertThat(foundCategory.getCreatedAt()).isNotNull(); // 自動設定されたcreatedAtもnullではないことを確認
    }

    @Test
    @DisplayName("カテゴリ名検索（存在しない場合）: DBに該当Categoryが存在しない → Optional.empty()が返る (異常)")
    void findByCategoryName_NotExist_ReturnsEmpty() {
        String nonExistingCategoryName = "NonExistentCategory";

        Optional<Category> foundCategoryOptional = categoryRepository.findByCategoryName(nonExistingCategoryName);

        assertThat(foundCategoryOptional).isEmpty();
    }

    @Test
    @DisplayName("作成日時順取得: 複数のCategoryが異なるcreatedAtでDBに存在 → createdAtの昇順でList<Category>が返る (正常)")
    void findAllByOrderByCreatedAtAsc_ReturnsOrdered() {
        // @PrePersist で自動設定される createdAt の値を考慮するため、
        // 保存後にDBから全てのカテゴリを改めて取得し、その createdAt を基に比較します。
        List<Category> savedCategories = categoryRepository.findAll();

        // 期待される順序にソートしたリストを作成 (createdAt 昇順)
        savedCategories.sort(Comparator.comparing(Category::getCreatedAt));

        // リポジトリメソッドを実行
        List<Category> categories = categoryRepository.findAllByOrderByCreatedAtAsc();

        // 結果の検証
        assertThat(categories).isNotNull();
        assertThat(categories).hasSize(3);

        // 取得したリストの順序が、savedCategories (createdAtでソート済み) と同じであることを確認
        // これにより、DBから取得された実際のcreatedAtに基づいて順序が正しいかを確認できます。
        assertThat(categories.get(0).getCategoryName()).isEqualTo(savedCategories.get(0).getCategoryName());
        assertThat(categories.get(1).getCategoryName()).isEqualTo(savedCategories.get(1).getCategoryName());
        assertThat(categories.get(2).getCategoryName()).isEqualTo(savedCategories.get(2).getCategoryName());

        // または、createdAt 自体の順序を直接アサート
        assertThat(categories.get(0).getCreatedAt()).isBefore(categories.get(1).getCreatedAt());
        assertThat(categories.get(1).getCreatedAt()).isBefore(categories.get(2).getCreatedAt());
    }

    @Test
    @DisplayName("作成日時順取得: カテゴリが存在しない場合 → 空のリストが返る (正常)")
    void findAllByOrderByCreatedAtAsc_NoCategories_ReturnsEmptyList() {
        categoryRepository.deleteAll(); // 全てのカテゴリを削除し、空の状態にする

        List<Category> categories = categoryRepository.findAllByOrderByCreatedAtAsc();

        assertThat(categories).isNotNull();
        assertThat(categories).isEmpty();
        assertThat(categories).hasSize(0);
    }

    @Test
    @DisplayName("カテゴリ重複チェック（存在）: 該当カテゴリ名がDBに存在 → trueが返る (正常)")
    void existsByCategoryName_Exists_ReturnsTrue() {
        String existingCategoryName = "Books";

        boolean exists = categoryRepository.existsByCategoryName(existingCategoryName);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("カテゴリ重複チェック（なし）: 該当カテゴリ名がDBに存在しない → falseが返る (正常)")
    void existsByCategoryName_NotExists_ReturnsFalse() {
        String nonExistingCategoryName = "NonExistentCategory";

        boolean exists = categoryRepository.existsByCategoryName(nonExistingCategoryName);

        assertThat(exists).isFalse();
    }
}