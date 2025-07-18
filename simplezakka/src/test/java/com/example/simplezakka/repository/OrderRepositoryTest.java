package com.example.simplezakka.repository;

import com.example.simplezakka.entity.Order;
import com.example.simplezakka.entity.OrderDetail;
import com.example.simplezakka.entity.Product;
import com.example.simplezakka.entity.Category;
import jakarta.persistence.PersistenceException; // 制約違反用
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException; // Spring Data JPAの例外


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest // JPA関連のテストに特化した設定（インメモリDB使用、関連Beanのみロード）
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager; // テストデータ準備や永続化の検証に使用

    @Autowired
    private OrderRepository orderRepository; // テスト対象のリポジトリ

    @Autowired // OrderDetailの削除確認用にインジェクト
    private OrderDetailRepository orderDetailRepository;


    private Product product1;
    private Product product2;

    // 各テストメソッド実行前に共通の商品データを準備
        @BeforeEach
        void setUp() {
            // Categoryを作成して永続化
            Category category = new Category();
            category.setCategoryName("サンプルカテゴリ");
            entityManager.persist(category);

            product1 = new Product();
            product1.setName("商品A");
            product1.setPrice(new BigDecimal("1000"));
            product1.setStock(10);
            product1.setCategory(category); // 必須項目の設定
            entityManager.persist(product1);

            product2 = new Product();
            product2.setName("商品B");
            product2.setPrice(new BigDecimal("2000"));
            product2.setStock(5);
            product2.setCategory(category); // 必須項目の設定
            entityManager.persist(product2);

            entityManager.flush();
        }


    // テスト用のOrderオブジェクトを作成するヘルパーメソッド
    private Order createSampleOrder(String customerName) {
        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setOrderName(customerName);
        order.setOrderEmail(customerName.toLowerCase() + "@example.com");
        order.setOrderAddress("住所 " + customerName);
        order.setOrderPhoneNumber("090-" + customerName.hashCode());

        order.setStatus("PENDING"); // 初期ステータス
        order.setIsGuest(false); 
        order.setPaymentMethod("BankTransfer");
        order.setShippingFee(new BigDecimal("500"));

        OrderDetail detail1 = new OrderDetail();
        detail1.setProduct(product1); // 事前に永続化したProductエンティティを設定
        detail1.setUnitPrice(product1.getPrice());
        detail1.setQuantity(1);
        order.addOrderDetail(detail1); // Orderエンティティのヘルパーメソッドで詳細を追加

        OrderDetail detail2 = new OrderDetail();
        detail2.setProduct(product2);
        detail2.setUnitPrice(product2.getPrice());
        detail2.setQuantity(1);
        order.addOrderDetail(detail2);
        BigDecimal total = order.calculateTotal(); // orderDetails合計＋送料
        order.setTotalPrice(total);
        return order;
    }

    @Test
    @DisplayName("注文と注文詳細を正常に保存できる")
    void saveOrderWithDetails_Success() {
        // Arrange
        Order order = createSampleOrder("顧客1");

        // Act
        Order savedOrder = orderRepository.save(order); // Orderを保存 (CascadeType.ALLによりOrderDetailも保存されるはず)
        entityManager.flush(); // DBへ反映
        entityManager.clear(); // 永続化コンテキストキャッシュをクリアし、DBからの取得を確実にする

        // Assert
        // 保存されたOrderをDBから取得して検証
        Order foundOrder = entityManager.find(Order.class, savedOrder.getOrderId());

        assertThat(foundOrder).isNotNull(); // Orderが取得できる
        assertThat(foundOrder.getOrderId()).isNotNull(); // IDが払い出されている
        assertThat(foundOrder.getOrderName()).isEqualTo(order.getOrderName());// 顧客名が正しい
        assertThat(foundOrder.getOrderDetails()).hasSize(2); // 注文詳細が2件含まれている
        // 注文詳細の内容も確認
        assertThat(foundOrder.getOrderDetails().get(0).getProduct().getName()).isEqualTo(product1.getName());
        assertThat(foundOrder.getOrderDetails().get(0).getQuantity()).isEqualTo(1);
        assertThat(foundOrder.getOrderDetails().get(1).getProduct().getName()).isEqualTo(product2.getName());

        assertThat(foundOrder.getOrderDetails().get(1).getQuantity()).isEqualTo(1);

        // 関連するOrderDetailも正しく永続化されていることを確認 (CascadeType.ALLの検証)
        OrderDetail foundDetail1 = entityManager.find(OrderDetail.class, foundOrder.getOrderDetails().get(0).getOrderDetailId());
        assertThat(foundDetail1).isNotNull();
        assertThat(foundDetail1.getOrder().getOrderId()).isEqualTo(foundOrder.getOrderId()); // Orderへの関連が設定されている
    }

    @Test
    @DisplayName("存在するIDで注文を検索できる")
    void findById_WhenOrderExists_ShouldReturnOrder() {
        // Arrange
        Order order1 = createSampleOrder("検索用顧客");
        Order savedOrder = entityManager.persistFlushFind(order1); // persist + flush + find を一括実行
        entityManager.clear();

        // Act
        Optional<Order> foundOrderOpt = orderRepository.findById(savedOrder.getOrderId()); // リポジトリ経由で検索

        // Assert
        assertThat(foundOrderOpt).isPresent(); // Optionalが値を持つ
        Order foundOrder = foundOrderOpt.get();
        assertThat(foundOrder.getOrderId()).isEqualTo(savedOrder.getOrderId());
        assertThat(foundOrder.getOrderName()).isEqualTo(order1.getOrderName());

        // 関連エンティティ(OrderDetails)が取得できるかも確認（FetchTypeに依存するが、@DataJpaTest環境では通常取得可能）
        assertThat(foundOrder.getOrderDetails()).hasSize(2);
    }

    @Test
    @DisplayName("存在しないIDで注文を検索するとOptional.emptyが返る")
    void findById_WhenOrderNotExists_ShouldReturnEmpty() {
        // Arrange
        Integer nonExistingId = 999; // 存在しないであろうID

        // Act
        Optional<Order> foundOrderOpt = orderRepository.findById(nonExistingId);

        // Assert
        assertThat(foundOrderOpt).isNotPresent(); // Optionalが空であること
    }

    @Test
    @DisplayName("すべての注文を取得できる")
    void findAll_ShouldReturnAllOrders() {
        // Arrange
        Order order1 = createSampleOrder("全件顧客1");
        Order order2 = createSampleOrder("全件顧客2");
        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Order> orders = orderRepository.findAll(); // 全件取得

        // Assert
        assertThat(orders).hasSize(2); // 2件取得できること
        // 顧客名などで内容を簡易的に確認
        assertThat(orders).extracting(Order::getOrderName)
                  .containsExactlyInAnyOrder(order1.getOrderName(), order2.getOrderName());

    }

    @Test
    @DisplayName("注文が存在しない場合findAllは空のリストを返す")
    void findAll_WhenNoOrders_ShouldReturnEmptyList() {
        // Arrange (データなしの状態)

        // Act
        List<Order> orders = orderRepository.findAll();

        // Assert
        assertThat(orders).isEmpty(); // 空のリストが返ること
    }

    @Test
    @DisplayName("注文を更新できる")
    void updateOrder_ShouldReflectChanges() {
        // Arrange
        Order order = createSampleOrder("更新前顧客");
        Order savedOrder = entityManager.persistFlushFind(order);
        Integer orderId = savedOrder.getOrderId();
        LocalDateTime initialUpdatedAt = savedOrder.getUpdatedAt(); // 初期の更新日時
        entityManager.detach(savedOrder); // 一度永続化コンテキストから切り離し、取得から行う状況を模倣

        // Act
        // 更新対象のOrderを取得
        Order orderToUpdate = orderRepository.findById(orderId).orElseThrow();
        String newStatus = "SHIPPED"; // 新しいステータス
        String newAddress = "更新後の住所"; // 新しい住所
        orderToUpdate.setStatus(newStatus); // ステータスを変更
        orderToUpdate.setOrderAddress(newAddress);// 住所を変更
        orderRepository.save(orderToUpdate); // 更新処理 (IDが存在するためUPDATE文が発行される)
        entityManager.flush();
        entityManager.clear();

        // Assert
        Order updatedOrder = entityManager.find(Order.class, orderId); // 更新後のデータをDBから取得
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getStatus()).isEqualTo(newStatus); // ステータスが更新されている
        assertThat(updatedOrder.getOrderAddress()).isEqualTo(newAddress);// 住所が更新されている
        assertThat(updatedOrder.getOrderName()).isEqualTo(order.getOrderName());// 変更していない項目はそのまま
        assertThat(updatedOrder.getUpdatedAt()).isAfter(initialUpdatedAt); // @PreUpdateによりupdatedAtが更新されているはず
    }


    @Test
    @DisplayName("IDを指定して注文を削除できる (関連する詳細も削除される)")
    void deleteById_ShouldRemoveOrderAndDetails() {
        // Arrange
        Order order = createSampleOrder("削除対象顧客");
        Order savedOrder = entityManager.persistFlushFind(order);
        Integer orderId = savedOrder.getOrderId();
        // 削除前のOrderDetailのIDを取得 (削除確認用)
        List<Integer> detailIds = savedOrder.getOrderDetails().stream()
                                           .map(OrderDetail::getOrderDetailId)
                                           .toList();
        assertThat(detailIds).isNotEmpty(); // 詳細が存在することを前提とする
        entityManager.clear();

        // Act
        // 削除前に存在することを確認
        assertThat(orderRepository.findById(orderId)).isPresent();
        assertThat(orderDetailRepository.findById(detailIds.get(0))).isPresent();

        orderRepository.deleteById(orderId); // IDで削除
        entityManager.flush(); // DBに反映
        entityManager.clear();

        // Assert
        // Orderが削除されたことを確認
        assertThat(orderRepository.findById(orderId)).isNotPresent();
        // 関連するOrderDetailも削除されていることを確認 (Orderエンティティの CascadeType.ALL と orphanRemoval = true による)
        for (Integer detailId : detailIds) {
             assertThat(orderDetailRepository.findById(detailId)).isNotPresent();
             // entityManager.findでも確認可能
             // assertThat(entityManager.find(OrderDetail.class, detailId)).isNull();
        }
    }


    @Test
    @DisplayName("必須項目nullで保存しようとするとDataIntegrityViolationExceptionが発生する")
    void saveOrder_WithNullRequiredField_ShouldThrowException() {
        // Arrange
        Order order = createSampleOrder("制約違反顧客");
       order.setOrderName(null);// @Column(nullable = false) のカラムにnullを設定

        // Act & Assert
        // save() の時点では例外は発生せず、flush() のタイミングでDB制約により発生することが多い
        assertThatThrownBy(() -> {
            orderRepository.save(order);
            entityManager.flush(); // DBへの反映時に制約違反が発生
        })
        .isInstanceOf(DataIntegrityViolationException.class) // Spring Data JPAがラップした例外
        .hasCauseInstanceOf(PersistenceException.class); // JPAレイヤーの例外が原因
        // .hasMessageContaining("NULL not allowed for column \"CUSTOMER_NAME\""); // DB依存のエラーメッセージ確認は脆い場合がある
    }
}