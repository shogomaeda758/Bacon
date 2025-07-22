package com.example.simplezakka.service;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.repository.CustomerRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @InjectMocks
    CustomerService customerService;
    @Mock
    CustomerRepository customerRepository;

    Customer customerEntity;
    BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        customerEntity = new Customer();
        customerEntity.setCustomerId(1);
        customerEntity.setLastName("Harada");
        customerEntity.setFirstName("Taro");
        customerEntity.setEmail("test@example.com");
        customerEntity.setPhoneNumber("09011112222");
        customerEntity.setAddress("Tokyo");
        customerEntity.setPassword(encoder.encode("securepw"));
    }

    // 会員作成（成功）
    @Test
    void createCustomer_ValidRequest_ShouldSucceed() {
        CustomerInfo info = new CustomerInfo();
        info.setName("Harada Taro"); info.setEmail("test@example.com");
        info.setPhoneNumber("09011112222"); info.setAddress("Tokyo");
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setCustomerInfo(info); req.setPassword("securepw");

        when(customerRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer arg = inv.getArgument(0, Customer.class);
            arg.setCustomerId(1);
            return arg;
        });

        CustomerResponse res = customerService.createCustomer(req);
        assertThat(res.getName()).isEqualTo("Harada Taro");
        assertThat(res.getEmail()).isEqualTo("test@example.com");
    }

    // 会員作成（メール重複）
    @Test
    void createCustomer_EmailExists_ShouldThrow() {
        CustomerInfo info = new CustomerInfo();
        info.setName("Yamada Hanako"); info.setEmail("test@example.com");
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setCustomerInfo(info); req.setPassword("hogepw");
        when(customerRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(req))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // 顧客ログイン成功
    @Test
    void login_CorrectCredentials_ShouldReturnDTO() {
        Customer localCustomer = new Customer();
        localCustomer.setCustomerId(1);
        localCustomer.setLastName("Harada");
        localCustomer.setFirstName("Taro");
        localCustomer.setEmail("test@example.com");
        localCustomer.setPhoneNumber("09011112222");
        localCustomer.setAddress("Tokyo");
        localCustomer.setPassword(encoder.encode("securepw"));

        when(customerRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(localCustomer));
        CustomerResponse res = customerService.login("test@example.com", "securepw");
        assertThat(res.getEmail()).isEqualTo("test@example.com");
    }

    // 顧客ログイン失敗（メールNG）
    @Test
    void login_EmailNotFound_ShouldThrow() {
        when(customerRepository.findByEmail("no@none.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> customerService.login("no@none.com", "pw"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // 顧客ログイン失敗（パスNG）
    @Test
    void login_InvalidPassword_ShouldThrow() {
        Customer localCustomer = new Customer();
        localCustomer.setCustomerId(1);
        localCustomer.setLastName("Harada");
        localCustomer.setFirstName("Taro");
        localCustomer.setEmail("test@example.com");
        localCustomer.setPhoneNumber("09011112222");
        localCustomer.setAddress("Tokyo");
        localCustomer.setPassword(encoder.encode("securepw"));

        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(localCustomer));
        assertThatThrownBy(() -> customerService.login("test@example.com", "wrong"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // 名前 or 電話検索（ヒットあり）
    @Test
    void searchBynameorPhone_Match_ShouldReturnList() {
        when(customerRepository.findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining(
                "Harada", "Harada", "Harada"))
            .thenReturn(List.of(customerEntity));
        List<CustomerResponse> res = customerService.searchByNameOrPhone("Harada");
        assertThat(res).hasSize(1);
    }

    // 名前 or 電話検索（ヒットなし）
    @Test
    void searchBynameorPhone_NoMatch_ShouldReturnEmpty() {
        when(customerRepository.findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining(
                "XYZ", "XYZ", "XYZ")).thenReturn(Collections.emptyList());
        List<CustomerResponse> res = customerService.searchByNameOrPhone("XYZ");
        assertThat(res).isEmpty();
    }

    // 顧客取得成功
    @Test
    void getCustomerById_ExistingId_ShouldReturnData() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customerEntity));
        CustomerResponse res = customerService.getCustomerById(1);
        assertThat(res.getEmail()).isEqualTo("test@example.com");
    }

    // 顧客取得失敗
    @Test
    void getCustomerById_NotFound_ShouldThrow() {
        when(customerRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> customerService.getCustomerById(999))
            .isInstanceOf(IllegalArgumentException.class);
    }


    // createdAt, updatedAt 自動設定
    @Test
    void createCustomer_ShouldSetCreatedAtAndUpdatedAt() {
        CustomerInfo info = new CustomerInfo();
        info.setName("Harada Taro"); info.setEmail("test2@example.com");
        info.setPhoneNumber("09032111222"); info.setAddress("Kyoto");
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setCustomerInfo(info); req.setPassword("testpw2");
        Customer ent = new Customer();
        ent.setCustomerId(2); ent.setLastName("Harada"); ent.setFirstName("Taro"); ent.setEmail("test2@example.com");
        ent.setAddress("Kyoto"); ent.setPassword(encoder.encode("testpw2"));
        ent.setPhoneNumber("09032111222");
        ent.setCreatedAt(java.time.LocalDateTime.now());
        ent.setUpdatedAt(java.time.LocalDateTime.now());
        when(customerRepository.existsByEmail("test2@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(ent);

        CustomerResponse res = customerService.createCustomer(req);
        assertThat(res.getCreatedAt()).isNotNull();
        assertThat(res.getUpdatedAt()).isNotNull();
    }
}