// =============================================================================
// CustomerRepository.java
// =============================================================================
package com.example.simplezakka.repository;

import com.example.simplezakka.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    // メールアドレスによる会員検索（ログイン認証用）
    Optional<Customer> findByEmail(String email);
    
    // メールアドレスの重複チェック（会員登録時）
    boolean existsByEmail(String email);
    
    // 名前での部分一致検索
    List<Customer> findByLastNameContainingOrFirstNameContaining(String lastName, String firstName);
    
    List<Customer> findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining(
            String lastName, String firstName, String phoneNumber);
    
    // 会員IDとメールアドレスによる検索（更新時の検証用）
    @Query("SELECT c FROM Customer c WHERE c.customerId = :customerId AND c.email = :email")
    Optional<Customer> findByCustomerIdAndEmail(@Param("customerId") Integer customerId, @Param("email") String email);

}