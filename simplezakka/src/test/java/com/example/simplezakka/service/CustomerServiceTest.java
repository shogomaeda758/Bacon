package com.example.simplezakka.service;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.repository.CustomerRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    void setUp() {
        customerEntity = new Customer();
        customerEntity.setCustomerId(1);
        customerEntity.setLastName("Harada");
        customerEntity.setFirstName("Taro");
        customerEntity.setEmail("test@example.com");
        customerEntity.setPassword("securepw");
        customerEntity.setPhoneNumber("09011112222");
        customerEntity.setAddress("Tokyo");
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
        when(customerRepository.save(any(Customer.class))).thenReturn(customerEntity);

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
        when(customerRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(customerEntity));
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
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(customerEntity));
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

    // 顧客更新成功
    @Test
    void updateCustomer_CorrectPassword_ShouldUpdate() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customerEntity));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        CustomerUpdateRequest req = new CustomerUpdateRequest();
        CustomerInfo info = new CustomerInfo();
        info.setName("Harada Jiro"); info.setEmail("jiro@harada.com");
        info.setPhoneNumber("09012345678"); info.setAddress("Kyoto");
        req.setCustomerInfo(info);
        req.setCurrentPassword("securepw"); req.setNewPassword("drivepw");

        CustomerResponse res = customerService.updateCustomer(1, req);
        assertThat(res.getName()).isEqualTo("Harada Jiro");
        assertThat(customerEntity.getPassword()).isEqualTo("drivepw");
    }

    // 顧客更新失敗（パス誤り）
    @Test
    void updateCustomer_WrongPassword_ShouldThrow() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customerEntity));
        CustomerUpdateRequest req = new CustomerUpdateRequest();
        CustomerInfo info = new CustomerInfo();
        info.setName("Harada Jiro"); info.setEmail("jiro@harada.com");
        req.setCustomerInfo(info);
        req.setCurrentPassword("wrong"); req.setNewPassword("drivepw");
        assertThatThrownBy(() -> customerService.updateCustomer(1, req))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // 制約違反（氏名等null）
    @Test
    void saveCustomer_WithNullRequiredField_ShouldThrowException() {
        Customer c = new Customer();
        c.setLastName(null); // 必須
        c.setFirstName("Taro"); c.setEmail("test2@mail.com");
        c.setPassword("pw"); c.setAddress("Tokyo"); c.setPhoneNumber("08000000001");

        when(customerRepository.save(any(Customer.class)))
            .thenThrow(new RuntimeException("DB制約違反"));

        CustomerRegisterRequest req = new CustomerRegisterRequest();
        CustomerInfo info = new CustomerInfo();
        info.setName(null); info.setEmail("test2@mail.com"); info.setAddress("Tokyo");
        info.setPhoneNumber("08000000001"); req.setCustomerInfo(info); req.setPassword("pw");
        assertThatThrownBy(() -> customerService.createCustomer(req))
            .isInstanceOf(RuntimeException.class);
    }

    // 制約違反（メール重複）
    @Test
    void saveCustomer_WithDuplicateEmail_ShouldThrowException() {
        CustomerInfo info = new CustomerInfo();
        info.setName("Harada Taro"); info.setEmail("test@example.com"); info.setPhoneNumber("09011112222"); info.setAddress("Tokyo");
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setCustomerInfo(info); req.setPassword("securepw");
        when(customerRepository.existsByEmail("test@example.com")).thenReturn(true);
        assertThatThrownBy(() -> customerService.createCustomer(req))
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
        ent.setAddress("Kyoto"); ent.setPassword("testpw2");
        ent.setPhoneNumber("09032111222");
        ent.setCreatedAt(java.time.LocalDateTime.now());
        ent.setUpdatedAt(java.time.LocalDateTime.now());
        when(customerRepository.existsByEmail("test2@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(ent);

        CustomerResponse res = customerService.createCustomer(req);
        assertThat(res.getCreatedAt()).isNotNull();
        assertThat(res.getUpdatedAt()).isNotNull();
    }

    // updatedAt更新
    @Test
    void updateCustomer_ShouldSetUpdatedAt() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customerEntity));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> {
            Customer c = i.getArgument(0, Customer.class);
            c.setUpdatedAt(java.time.LocalDateTime.now());
            return c;
        });
        CustomerUpdateRequest req = new CustomerUpdateRequest();
        CustomerInfo info = new CustomerInfo();
        info.setName("Harada Jiro"); info.setEmail("jiro@harada.com"); info.setAddress("Kyoto");
        info.setPhoneNumber("09055555555");
        req.setCustomerInfo(info);
        req.setCurrentPassword("securepw"); req.setNewPassword("drivepw");
        CustomerResponse res = customerService.updateCustomer(1, req);
        assertThat(res.getUpdatedAt()).isNotNull();
    }

}
