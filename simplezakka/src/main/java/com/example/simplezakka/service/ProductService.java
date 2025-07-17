package com.example.simplezakka.service;

import com.example.simplezakka.dto.product.ProductDetail;
import com.example.simplezakka.dto.product.ProductListItem;
import com.example.simplezakka.entity.Product;
import com.example.simplezakka.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 商品サービス
 * 商品の検索、取得、変換処理を行う
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 全商品取得
     * @return 全商品リスト
     */
    public List<ProductListItem> findAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToListItem)
                .collect(Collectors.toList());
    }

    /**
     * 商品詳細取得
     * @param productId 商品ID
     * @return 商品詳細情報（存在しない場合はnull）
     */
    public ProductDetail findProductById(Integer productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        return productOpt.map(this::convertToDetail).orElse(null);
    }

    /**
     * カテゴリ別商品取得
     * @param categoryId カテゴリID
     * @return カテゴリに属する商品リスト
     */
    public List<ProductListItem> findProductsByCategory(Integer categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToListItem)
                .collect(Collectors.toList());
    }

    /**
     * 商品検索
     * @param keyword 検索キーワード
     * @return 検索結果商品リスト
     */
    public List<ProductListItem> searchProducts(String keyword) {
        return productRepository.findByNameContaining(keyword).stream()
                .map(this::convertToListItem)
                .collect(Collectors.toList());
    }

    /**
     * 在庫あり商品取得
     * @param minStock 最小在庫数
     * @return 在庫あり商品リスト
     */
    public List<ProductListItem> findProductsInStock(Integer minStock) {
        return productRepository.findByStockGreaterThan(minStock).stream()
                .map(this::convertToListItem)
                .collect(Collectors.toList());
    }

    /**
     * 在庫減少処理
     * @param productId 商品ID
     * @param quantity 減少数量
     * @return 処理成功時true、失敗時false
     */
    public boolean decreaseStock(Integer productId, Integer quantity) {
        int updatedRows = productRepository.decreaseStock(productId, quantity);
        return updatedRows > 0;
    }

    /**
     * 在庫更新処理
     * @param productId 商品ID
     * @param newStock 新しい在庫数
     * @return 処理成功時true、失敗時false
     */
    public boolean updateStock(Integer productId, Integer newStock) {
        int updatedRows = productRepository.updateStock(productId, newStock);
        return updatedRows > 0;
    }

    /**
     * 商品エンティティをリスト用DTOに変換
     * @param product 商品エンティティ
     * @return 商品リストアイテム
     */
    private ProductListItem convertToListItem(Product product) {
        return new ProductListItem(
                product.getProductId(),
                product.getName(),
<<<<<<< HEAD
                product.getPrice().intValue(),
                product.getImageUrl(),
                product.getCategory().getCategoryName() // ★ 追加：カテゴリ名を含める
=======
                product.getPrice().intValue(), 
                product.getImageUrl(),
                product.getCategory().getCategoryName()
>>>>>>> develop
        );
    }

    /**
     * 商品エンティティを詳細用DTOに変換
     * @param product 商品エンティティ
     * @return 商品詳細
     */
    private ProductDetail convertToDetail(Product product) {
        return new ProductDetail(
                product.getProductId(),
                product.getName(),
                product.getPrice().intValue(),
                product.getDescription(),
                product.getStock(),
                product.getImageUrl()
        );
    }
}
