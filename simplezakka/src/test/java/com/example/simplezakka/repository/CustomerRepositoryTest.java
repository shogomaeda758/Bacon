package com.example.simplezakka.repository;

import com.example.simplezakka.entity.Customer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class CustomerRepositoryTest {
    @Autowired
    CustomerRepository customerRepository;

    Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setLastName("Harada");
        customer.setFirstName("Taro");
        customer.setEmail("test@example.com");
        customer.setPhoneNumber("09087654321");
        customer.setPassword("abcDEF123");
        customer.setAddress("Osaka");
    }

    // 会員作成 (Create) と Read
    @Test
    void saveAndFindById_Success() {
        Customer saved = customerRepository.save(customer);
        Optional<Customer> found = customerRepository.findById(saved.getCustomerId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    // findByEmail 正常系
    @Test
    void findByEmail_Exists_ShouldReturnCustomer() {
        customerRepository.save(customer);
        Optional<Customer> found = customerRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getLastName()).isEqualTo("Harada");
    }

    // findByEmail 異常系
    @Test
    void findByEmail_NotFound_ShouldReturnEmpty() {
        assertThat(customerRepository.findByEmail("none@example.com")).isEmpty();
    }

    // existsByEmail（正常/異常）
    @Test
    void existsByEmail_Exists_ShouldReturnTrue() {
        customerRepository.save(customer);
        assertThat(customerRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    void existsByEmail_NotExists_ShouldReturnFalse() {
        assertThat(customerRepository.existsByEmail("nope@domain.com")).isFalse();
    }

    // 名前・電話 検索
    @Test
    void findByNameOrPhone_WithMatch_ShouldReturnList() {
        customerRepository.save(customer);
        List<Customer> list = customerRepository
            .findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining("Harada", "", "");
        assertThat(list).isNotEmpty();
    }

    @Test
    void findByNameOrPhone_PhoneMatch_ShouldReturnList() {
        customerRepository.save(customer);
        List<Customer> list = customerRepository
            .findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining("", "", "090");
        assertThat(list).isNotEmpty();
    }

    @Test
    void findByNameOrPhone_NoMatch_ShouldReturnEmptyList() {
        customerRepository.save(customer);
        List<Customer> list = customerRepository
            .findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining("XYZ", "XYZ", "XYZ");
        assertThat(list).isEmpty();
    }

    // findAll・境界値
    @Test
    void findAll_ShouldReturnAllCustomers() {
        customerRepository.save(customer);
        Customer c2 = new Customer(); c2.setLastName("Suzuki"); c2.setFirstName("Saburo");
        c2.setEmail("suzuki@ex.co"); c2.setPassword("pw"); c2.setPhoneNumber("09013245768"); c2.setAddress("Tokyo");
        customerRepository.save(c2);
        List<Customer> all = customerRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void findAll_WhenNoCustomers_ShouldReturnEmptyList() {
        assertThat(customerRepository.findAll()).isEmpty();
    }

    // CRUD更新
    @Test
    void updateCustomer_ShouldReflectChanges() {
        Customer saved = customerRepository.save(customer);
        saved.setAddress("Nagoya");
        customerRepository.save(saved);
        Customer found = customerRepository.findById(saved.getCustomerId()).get();
        assertThat(found.getAddress()).isEqualTo("Nagoya");
    }

    // CRUD削除
    @Test
    void deleteCustomer_ShouldRemoveFromDatabase() {
        Customer saved = customerRepository.save(customer);
        customerRepository.deleteById(saved.getCustomerId());
        assertThat(customerRepository.findById(saved.getCustomerId())).isEmpty();
    }

    // 制約違反（メール重複)
    @Test
    void saveCustomer_WithDuplicateEmail_ShouldThrowException() {
        customerRepository.save(customer);
        Customer dup = new Customer();
        dup.setLastName("Yamada"); dup.setFirstName("Hanako");
        dup.setEmail("test@example.com");
        dup.setPassword("hoge1234");
        dup.setPhoneNumber("08012345679");
        dup.setAddress("Tokyo");
        assertThatThrownBy(() -> {
            customerRepository.saveAndFlush(dup);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // 制約違反（必須）
    @Test
    void saveCustomer_WithNullRequiredField_ShouldThrowException() {
        Customer ng = new Customer();
        ng.setLastName(null); 
        ng.setFirstName("Taro");
        ng.setEmail("taro2@mail.com");
        ng.setPassword("pw");
        ng.setAddress("Nagoya");
        ng.setPhoneNumber("09022223333");
        assertThatThrownBy(() -> {
            customerRepository.saveAndFlush(ng);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ID+メール複合検索 正常・異常
    @Test
    void findByCustomerIdAndEmail_Valid_ShouldReturn() {
        Customer saved = customerRepository.save(customer);
        Optional<Customer> found = customerRepository.findByCustomerIdAndEmail(saved.getCustomerId(), saved.getEmail());
        assertThat(found).isPresent();
    }

    @Test
    void findByCustomerIdAndEmail_Invalid_ShouldReturnEmpty() {
        customerRepository.save(customer);
        assertThat(customerRepository.findByCustomerIdAndEmail(999, "wrong@none.com")).isEmpty();
    }
}


