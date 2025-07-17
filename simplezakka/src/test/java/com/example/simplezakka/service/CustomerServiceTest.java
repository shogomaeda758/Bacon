package com.example.simplezakka.service;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @InjectMocks
    private CustomerService customerService;
    @Mock
    private CustomerRepository customerRepository;

    private Customer customerEntity;

    @BeforeEach
    void setUp() {
        customerEntity = new Customer();
        customerEntity.setCustomerId(1);
        customerEntity.setLastName("山田");
        customerEntity.setFirstName("太郎");
        customerEntity.setEmail("yamada@example.com");
        customerEntity.setAddress("東京都");
        customerEntity.setPhoneNumber("090-1111-2222");
        customerEntity.setPassword("plainpass");
        customerEntity.setCreatedAt(LocalDateTime.now());
        customerEntity.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("顧客登録API")
    class RegisterTests {
        @Test
        @DisplayName("新規登録成功")
        void register_Success() {
            CustomerInfo info = new CustomerInfo();
            info.setName("山田 太郎");
            info.setEmail("yamada@example.com");
            info.setAddress("東京都");
            info.setPhoneNumber("090-1111-2222");
            CustomerRegisterRequest req = new CustomerRegisterRequest();
            req.setCustomerInfo(info);
            req.setPassword("plainpass");

            when(customerRepository.existsByEmail("yamada@example.com")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenReturn(customerEntity);

            CustomerResponse res = customerService.createCustomer(req);
            assertThat(res.getName()).isEqualTo("山田 太郎");
            assertThat(res.getEmail()).isEqualTo("yamada@example.com");
        }

        @Test
        @DisplayName("新規登録失敗（メール重複）")
        void register_DuplicateEmail() {
            CustomerInfo info = new CustomerInfo();
            info.setName("山田 太郎");
            info.setEmail("yamada@example.com");
            CustomerRegisterRequest req = new CustomerRegisterRequest();
            req.setCustomerInfo(info);
            req.setPassword("plainpass");

            when(customerRepository.existsByEmail("yamada@example.com")).thenReturn(true);

            assertThatThrownBy(() -> customerService.createCustomer(req)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("ログインAPI")
    class LoginTests {
        @Test
        @DisplayName("ログイン成功")
        void login_Success() {
            when(customerRepository.findByEmail("yamada@example.com")).thenReturn(Optional.of(customerEntity));
            CustomerResponse res = customerService.login("yamada@example.com", "plainpass");
            assertThat(res.getName()).isEqualTo("山田 花子");
        }
        @Test
        @DisplayName("メール未登録")
        void login_EmailNotFound() {
            when(customerRepository.findByEmail("nope@no.co")).thenReturn(Optional.empty());
            assertThatThrownBy(() ->
                    customerService.login("nope@no.co", "hoge")).isInstanceOf(IllegalArgumentException.class);
        }
        @Test
        @DisplayName("パスワード不一致")
        void login_WrongPassword() {
            when(customerRepository.findByEmail("yamada@example.com")).thenReturn(Optional.of(customerEntity));
            assertThatThrownBy(() -> customerService.login("yamada@example.com", "wrongpw"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("会員情報更新API")
    class UpdateTests {
        @Test
        @DisplayName("会員情報更新成功")
        void update_Success() {
            CustomerInfo info = new CustomerInfo();
            info.setName("山田 花子");
            info.setEmail("hanako@yamada.com");
            info.setAddress("愛知県");
            info.setPhoneNumber("080-2222-3333");
            CustomerUpdateRequest req = new CustomerUpdateRequest();
            req.setCustomerInfo(info);
            req.setCurrentPassword("plainpass");
            req.setNewPassword("newpass");

            when(customerRepository.findById(1)).thenReturn(Optional.of(customerEntity));
            when(customerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            CustomerResponse result = customerService.updateCustomer(1, req);
            assertThat(result.getName()).isEqualTo("山田 花子");
            assertThat(customerEntity.getPassword()).isEqualTo("newpass");
        }
        @Test
        @DisplayName("会員情報更新失敗（認証NG）")
        void update_Fail_WrongPassword() {
            CustomerUpdateRequest req = new CustomerUpdateRequest();
            CustomerInfo info = new CustomerInfo();
            info.setName("山田 太郎");
            info.setEmail("yamada@example.com");
            req.setCustomerInfo(info);
            req.setCurrentPassword("wrongpass");
            req.setNewPassword("newpass");
            when(customerRepository.findById(1)).thenReturn(Optional.of(customerEntity));
            assertThatThrownBy(() -> customerService.updateCustomer(1, req))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("会員詳細取得API")
    class GetTests {
        @Test
        @DisplayName("取得成功")
        void getCustomerById_Success() {
            when(customerRepository.findById(1)).thenReturn(Optional.of(customerEntity));
            CustomerResponse res = customerService.getCustomerById(1);
            assertThat(res.getName()).isEqualTo("山田 太郎");
        }
        @Test
        @DisplayName("取得失敗")
        void getCustomerById_Fail() {
            when(customerRepository.findById(99)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> customerService.getCustomerById(99)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("名前or電話検索API")
    class SearchTests {
        @Test
        @DisplayName("名前または電話番号一致で会員リスト返却")
        void searchsearchByNameOrPhone_Match() {
            Customer target = new Customer();
            target.setFirstName("花子");
            target.setLastName("佐藤");
            target.setPhoneNumber("090-7777-9999");
            target.setEmail("sato@hanako.com");
            when(customerRepository.findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining(
                    "花子", "花子", "花子"))
                    .thenReturn(List.of(target));
            List<CustomerResponse> result = customerService.searchByNameOrPhone("花子");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("佐藤 花子");
        }

        @Test
        @DisplayName("名前or電話一致なしで空リスト返却")
        void searchsearchByNameOrPhone_NoMatch() {
            when(customerRepository.findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining(
                    "none", "none", "none"))
                    .thenReturn(Collections.emptyList());
            List<CustomerResponse> result = customerService.searchByNameOrPhone("none");
            assertThat(result).isEmpty();
        }
    }
}

