package com.example.simplezakka.repository;

import com.example.simplezakka.entity.Category;
import com.example.simplezakka.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category category1;
    private Category category2;
    private Product productA; // 在庫: 10, カテゴリ: キッチン, 名前: マグカップ
    private Product productB; // 在庫: 5, カテゴリ: キッチン, 名前: お皿
    private Product productC; // 在庫: 3, カテゴリ: バス, 名前: バスマット
    private Product productD; // 在庫: 0, カテゴリ: バス, 名前: タオル

    @BeforeEach
    void setUp() {
        // カテゴリデータを準備し永続化
        category1 = new Category();
        category1.setCategoryName("キッチン");
        entityManager.persistAndFlush(category1);

        category2 = new Category();
        category2.setCategoryName("バス");
        entityManager.persistAndFlush(category2);

        // 商品データを準備し永続化
        productA = new Product();
        productA.setName("マグカップ");
        productA.setDescription("かわいいマグカップ");
        productA.setPrice(new BigDecimal("1500.00"));
        productA.setStock(10);
        productA.setImageUrl("http://example.com/mugcup.jpg");
        productA.setIsRecommended(true);
        productA.setCategory(category1);
        entityManager.persistAndFlush(productA);

        productB = new Product();
        productB.setName("お皿");
        productB.setDescription("白いお皿");
        productB.setPrice(new BigDecimal("1000.00"));
        productB.setStock(5);
        productB.setImageUrl("http://example.com/dish.jpg");
        productB.setIsRecommended(false);
        productB.setCategory(category1);
        entityManager.persistAndFlush(productB);

        productC = new Product();
        productC.setName("バスマット");
        productC.setDescription("ふわふわバスマット");
        productC.setPrice(new BigDecimal("2000.00"));
        productC.setStock(3);
        productC.setImageUrl("http://example.com/bathmat.jpg");
        productC.setIsRecommended(true);
        productC.setCategory(category2);
        entityManager.persistAndFlush(productC);

        productD = new Product();
        productD.setName("タオル");
        productD.setDescription("吸水性の良いタオル");
        productD.setPrice(new BigDecimal("800.00"));
        productD.setStock(0); // 在庫0の商品
        productD.setImageUrl("http://example.com/towel.jpg");
        productD.setIsRecommended(false);
        productD.setCategory(category2);
        entityManager.persistAndFlush(productD);
    }

    @Test
    @DisplayName("商品取得 by categoryId: カテゴリID=1に商品2件存在 → 商品が2件返るリスト (正常)")
    void findByCategoryId_ShouldReturnItems() {
        // GIVEN: setUp()でcategoryId=1に2件の商品が永続化済み

        // WHEN:
        List<Product> products = productRepository.findByCategoryId(category1.getCategoryId());

        // THEN:
        assertThat(products).isNotNull();
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getCategory)
                            .extracting(Category::getCategoryId)
                            .containsOnly(category1.getCategoryId());
        assertThat(products).extracting(Product::getName)
                            .containsExactlyInAnyOrder("マグカップ", "お皿");
    }

    @Test
    @DisplayName("商品取得 by categoryId: 該当カテゴリIDに商品が存在しない → 空のリストが返る (正常)")
    void findByCategoryId_NoItems_ShouldReturnEmptyList() {
        // GIVEN: 存在しないカテゴリID (例: 999)

        // WHEN:
        List<Product> products = productRepository.findByCategoryId(999);

        // THEN:
        assertThat(products).isNotNull();
        assertThat(products).isEmpty();
    }

    @Test
    @DisplayName("decreaseStock（正常減算）: 在庫10の商品ID=1, quantity=5 → 戻り値 1（更新件数） (正常)")
    @Transactional // @Modifying を含むメソッドはトランザクション内で実行する必要がある
    void decreaseStock_EnoughStock_ShouldUpdate() {
        // GIVEN: productA (ID: productA.getProductId(), 在庫: 10) が存在

        // WHEN:
        int updatedRows = productRepository.decreaseStock(productA.getProductId(), 5);
        entityManager.flush(); // 更新をDBに同期

        // THEN:
        assertThat(updatedRows).isEqualTo(1); // 1件更新されたことを確認

        // 実際に在庫が減っていることを確認
        Optional<Product> updatedProduct = productRepository.findById(productA.getProductId());
        assertThat(updatedProduct).isPresent();
        assertThat(updatedProduct.get().getStock()).isEqualTo(5); // 10 - 5 = 5
    }

    @Test
    @DisplayName("decreaseStock（在庫不足）: 在庫3の商品, quantity=10 → 戻り値 0（stock < quantity） (異常)")
    @Transactional // @Modifying を含むメソッドはトランザクション内で実行する必要がある
    void decreaseStock_InsufficientStock_ShouldNotUpdate() {
        // GIVEN: productC (ID: productC.getProductId(), 在庫: 3) が存在

        // WHEN:
        int updatedRows = productRepository.decreaseStock(productC.getProductId(), 10); // 在庫3に対して10を減らそうとする
        entityManager.flush(); // 更新をDBに同期

        // THEN:
        assertThat(updatedRows).isEqualTo(0); // 更新件数が0であることを確認

        // 在庫が変わっていないことを確認
        Optional<Product> originalProduct = productRepository.findById(productC.getProductId());
        assertThat(originalProduct).isPresent();
        assertThat(originalProduct.get().getStock()).isEqualTo(3); // 在庫は3のまま
    }

    @Test
    @DisplayName("findByNameContaining: 'マグ'含む名前の商品が存在 → マッチする Product のリスト返る (正常)")
    void findByNameContaining_ShouldReturnResults() {
        // GIVEN: setUp()で"マグカップ"が永続化済み

        // WHEN:
        List<Product> products = productRepository.findByNameContaining("マグ");

        // THEN:
        assertThat(products).isNotNull();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("マグカップ");
    }

    @Test
    @DisplayName("findByNameContaining: マッチする名前の商品が存在しない → 空のリストが返る (正常)")
    void findByNameContaining_NoMatchingResults_ShouldReturnEmptyList() {
        // GIVEN: "存在しないキーワード"にマッチする商品が存在しない

        // WHEN:
        List<Product> products = productRepository.findByNameContaining("存在しないキーワード");

        // THEN:
        assertThat(products).isNotNull();
        assertThat(products).isEmpty();
    }

    @Test
    @DisplayName("findByStockGreaterThan: 在庫が指定値より大きい商品を取得 → 該当商品が返るリスト (正常)")
    void findByStockGreaterThan_ShouldReturnProductsWithMoreStock() {
        // GIVEN: productA(10), productB(5), productC(3), productD(0) が存在

        // WHEN:
        List<Product> products = productRepository.findByStockGreaterThan(4); // 在庫が4より大きい商品 (productA:10, productB:5)

        // THEN:
        assertThat(products).isNotNull();
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName)
                            .containsExactlyInAnyOrder("マグカップ", "お皿");
    }

    @Test
    @DisplayName("findByStockGreaterThan: 在庫が指定値より大きい商品が存在しない → 空のリストが返る (正常)")
    void findByStockGreaterThan_NoProducts_ShouldReturnEmptyList() {
        // GIVEN: productA(10), productB(5), productC(3), productD(0) が存在

        // WHEN:
        List<Product> products = productRepository.findByStockGreaterThan(10); // 在庫が10より大きい商品 (存在しない)

        // THEN:
        assertThat(products).isNotNull();
        assertThat(products).isEmpty();
    }

    @Test
    @DisplayName("updateStock: 指定した商品の在庫を更新 → 戻り値 1（更新件数） (正常)")
    @Transactional // @Modifying を含むメソッドはトランザクション内で実行する必要がある
    void updateStock_ShouldUpdateProductStock() {
        // GIVEN: productA (ID: productA.getProductId(), 在庫: 10) が存在

        // WHEN:
        int updatedRows = productRepository.updateStock(productA.getProductId(), 20); // 在庫を20に更新
        entityManager.flush(); // 更新をDBに同期

        // THEN:
        assertThat(updatedRows).isEqualTo(1); // 1件更新されたことを確認

        // 実際に在庫が更新されていることを確認
        Optional<Product> updatedProduct = productRepository.findById(productA.getProductId());
        assertThat(updatedProduct).isPresent();
        assertThat(updatedProduct.get().getStock()).isEqualTo(20); // 在庫が20になっていることを確認
    }

    @Test
    @DisplayName("updateStock: 存在しない商品の在庫を更新しようとする → 戻り値 0（更新件数） (正常)")
    @Transactional // @Modifying を含むメソッドはトランザクション内で実行する必要がある
    void updateStock_NonExistentProduct_ShouldReturnZero() {
        // GIVEN: 存在しない商品ID (例: 999)

        // WHEN:
        int updatedRows = productRepository.updateStock(999, 50); // 存在しない商品の在庫を更新しようとする
        entityManager.flush(); // 更新をDBに同期

        // THEN:
        assertThat(updatedRows).isEqualTo(0); // 更新件数が0であることを確認
    }
}