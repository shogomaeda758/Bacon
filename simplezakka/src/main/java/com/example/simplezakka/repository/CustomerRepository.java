// =============================================================================
// CustomerRepository.java
// =============================================================================
package com.example.simplezakka.repository;

import com.example.simplezakka.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Integer> {
    
    // メールアドレスによる会員検索（ログイン認証用）
    Optional<CustomerEntity> findByEmail(String email);
    
    // メールアドレスの重複チェック（会員登録時）
    boolean existsByEmail(String email);
    
    // 名前での部分一致検索
    List<CustomerEntity> findByLastNameContainingOrFirstNameContaining(String lastName, String firstName);
    
    // 電話番号による検索
    Optional<CustomerEntity> findByPhoneNumber(String phoneNumber);
    
    // 会員IDとメールアドレスによる検索（更新時の検証用）
    @Query("SELECT c FROM Customer c WHERE c.customerId = :customerId AND c.email = :email")
    Optional<CustomerEntity> findByCustomerIdAndEmail(@Param("customerId") Integer customerId, @Param("email") String email);
    
    // アクティブな会員の取得（削除フラグがある場合に使用）
    // 現在の設計では削除フラグはないが、将来的な拡張を考慮
    @Query("SELECT c FROM Customer c WHERE c.customerId = :customerId")
    Optional<CustomerEntity> findActiveCustomer(@Param("customerId") Integer customerId);
}
