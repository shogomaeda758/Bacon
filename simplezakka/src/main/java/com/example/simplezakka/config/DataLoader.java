package com.example.simplezakka.config;

import com.example.simplezakka.entity.Category; // CategoryEntityをインポート
import com.example.simplezakka.entity.Product;
import com.example.simplezakka.repository.CategoryRepository; // CategoryRepositoryをインポート
import com.example.simplezakka.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal; // BigDecimalをインポート
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository; // CategoryRepositoryを追加

    // コンストラクタにCategoryRepositoryを追加
    public DataLoader(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        loadSampleData(); // メソッド名を汎用的に変更
    }

    private void loadSampleCategories() {
        if (categoryRepository.count() > 0) {
            return; // すでにデータが存在する場合はスキップ
        }

        List<Category> categories = Arrays.asList(
            createCategory("インテリア"),
            createCategory("キッチン用品"),
            createCategory("バッグ・アクセサリー"),
            createCategory("生活雑貨"),
            createCategory("文房具・オフィス用品")
        );
        
        categoryRepository.saveAll(categories);
    }

    private void loadSampleData() {
        // データがすでに存在する場合はスキップ
        if (productRepository.count() > 0 || categoryRepository.count() > 0) {
            return;
        }

        // --- 1. カテゴリデータを先に保存する ---
        Category interior = createCategory("インテリア雑貨");
        Category homeGoods = createCategory("生活雑貨");
        Category fashion = createCategory("ファッション雑貨");
        Category kitchen = createCategory("キッチン用品");

        // カテゴリを保存 (saveAllでまとめても良い)
        categoryRepository.save(interior);
        categoryRepository.save(homeGoods);
        categoryRepository.save(fashion);
        categoryRepository.save(kitchen);

        // --- 2. 商品データを作成し、カテゴリを紐付けて保存する ---
        List<Product> products = Arrays.asList(
            createProduct(
                "シンプルデスクオーガナイザー",
                "机の上をすっきり整理できる木製オーガナイザー。ペン、メモ、スマートフォンなどを収納できます。",
                3500,
                20,
                "/images/desk-organizer.png",
                true,
                interior // ここでカテゴリを設定
            ),
            createProduct(
                "アロマディフューザー（ウッド）",
                "天然木を使用したシンプルなデザインのアロマディフューザー。LEDライト付き。",
                4200,
                15,
                "/images/aroma-diffuser.png",
                true,
                interior // ここでカテゴリを設定
            ),
            createProduct(
                "コットンブランケット",
                "オーガニックコットン100%のやわらかブランケット。シンプルなデザインで様々なインテリアに合います。",
                5800,
                10,
                "/images/cotton-blanket.png",
                false,
                homeGoods // ここでカテゴリを設定
            ),
            createProduct(
                "ステンレスタンブラー",
                "保温・保冷機能に優れたシンプルなデザインのステンレスタンブラー。容量350ml。",
                2800,
                30,
                "/images/tumbler.png",
                false,
                kitchen // ここでカテゴリを設定
            ),
            createProduct(
                "ミニマルウォールクロック",
                "余計な装飾のないシンプルな壁掛け時計。静音設計。",
                3200,
                25,
                "/images/wall-clock.png",
                false,
                interior // ここでカテゴリを設定
            ),
            createProduct(
                "リネンクッションカバー",
                "天然リネン100%のクッションカバー。取り外して洗濯可能。45×45cm対応。",
                2500,
                40,
                "/images/cushion-cover.png",
                true,
                homeGoods // ここでカテゴリを設定
            ),
            createProduct(
                "陶器フラワーベース",
                "手作りの風合いが魅力の陶器製フラワーベース。シンプルな形状で花を引き立てます。",
                4000,
                15,
                "/images/flower-vase.png",
                false,
                interior // ここでカテゴリを設定
            ),
            createProduct(
                "木製コースター（4枚セット）",
                "天然木を使用したシンプルなデザインのコースター。4枚セット。",
                1800,
                50,
                "/images/wooden-coaster.png",
                false,
                kitchen // ここでカテゴリを設定
            ),
            createProduct(
                "キャンバストートバッグ",
                "丈夫なキャンバス地で作られたシンプルなトートバッグ。内ポケット付き。",
                3600,
                35,
                "/images/tote-bag.png",
                true,
                fashion // ここでカテゴリを設定
            ),
            createProduct(
                "ガラス保存容器セット",
                "電子レンジ・食洗機対応のガラス製保存容器。3サイズセット。",
                4500,
                20,
                "/images/glass-container.png",
                false,
                kitchen // ここでカテゴリを設定
            )
        );

        productRepository.saveAll(products);
    }

    // CategoryEntityを作成するヘルパーメソッド
    private Category createCategory(String categoryName) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    // Productを作成するヘルパーメソッドにCategoryEntity引数を追加
    private Product createProduct(String name, String description, Integer price, Integer stock, String imageUrl, Boolean isRecommended, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(BigDecimal.valueOf(price)); // priceはBigDecimal型なので変換
        product.setStock(stock);
        product.setImageUrl(imageUrl);
        product.setIsRecommended(isRecommended);
        product.setCategory(category); // ★ここが最も重要です★
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }
}