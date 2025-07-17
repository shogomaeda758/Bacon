package com.example.simplezakka.repository;

import com.example.simplezakka.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class CustomerRepositoryTest {
    @Autowired
    private CustomerRepository customerRepository;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setLastName("田中");
        customer.setFirstName("真紀");
        customer.setEmail("tanaka@makisan.com");
        customer.setPassword("pw");
        customer.setAddress("大分県");
        customer.setPhoneNumber("080-1234-5555");
    }

    @Nested
    @DisplayName("メール検索API")
    class FindByEmailTests {
        @Test
        @DisplayName("メール一致で会員取得")
        void findByEmail_Success() {
            customerRepository.save(customer);
            Optional<Customer> found = customerRepository.findByEmail("tanaka@makisan.com");
            assertThat(found).isPresent();
            assertThat(found.get().getLastName()).isEqualTo("田中");
        }
        @Test
        @DisplayName("メール不一致で空Optional")
        void findByEmail_Fail() {
            assertThat(customerRepository.findByEmail("noone@none.co")).isEmpty();
        }
    }

    @Nested
    @DisplayName("メール重複チェックAPI")
    class ExistsTests {
        @Test
        @DisplayName("メール重複ありでtrue")
        void existsByEmail_True() {
            customerRepository.save(customer);
            assertThat(customerRepository.existsByEmail("tanaka@makisan.com")).isTrue();
        }
        @Test
        @DisplayName("メール登録なしでfalse")
        void existsByEmail_False() {
            assertThat(customerRepository.existsByEmail("a@b.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("名前or電話番号検索API")
    class NameOrPhoneTests {
        @Test
        @DisplayName("名前または電話番号検索で会員リスト返却")
        void nameOrPhone_Match() {
            customerRepository.save(customer);
            var list = customerRepository
                    .findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining(
                            "田中", "真紀", "5555");
            assertThat(list).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("ID+メール複合検索API")
    class ByCustomerIdAndEmailTests {
        @Test
        @DisplayName("findByCustomerIdAndEmail 成功")
        void findByCustomerIdAndEmail_Success() {
            Customer saved = customerRepository.save(customer);
            Optional<Customer> found = customerRepository.findByCustomerIdAndEmail(saved.getCustomerId(), "tanaka@makisan.com");
            assertThat(found).isPresent();
            assertThat(found.get().getLastName()).isEqualTo("田中");
        }
        @Test
        @DisplayName("findByCustomerIdAndEmail 失敗")
        void findByCustomerIdAndEmail_Fail() {
            assertThat(customerRepository.findByCustomerIdAndEmail(999, "xxx@zzz.com")).isEmpty();
        }
    }
}


