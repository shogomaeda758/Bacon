package com.example.simplezakka.service;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @InjectMocks
    CustomerService customerService;

    @Mock
    CustomerRepository customerRepository;

    // --- register 正常 ---
    @Test
    void createCustomer_ValidInput_ShouldCreateCustomer() {
        // DTO作成
        CustomerInfo info = new CustomerInfo();
        info.setName("原田 太郎");
        info.setEmail("taro@harada.com");
        info.setAddress("神奈川県横浜市1-2-3");
        info.setPhoneNumber("090-1234-5678");

        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setCustomerInfo(info);
        req.setPassword("testpass123");

        // Entity作成: nameを分割
        Customer entity = new Customer();
        entity.setCustomerId(1);
        entity.setLastName("原田");
        entity.setFirstName("太郎");
        entity.setEmail("taro@harada.com");
        entity.setAddress("神奈川県横浜市1-2-3");
        entity.setPhoneNumber("090-1234-5678");
        entity.setPassword("testpass123");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        when(customerRepository.existsByEmail("taro@harada.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(entity);

        CustomerResponse res = customerService.createCustomer(req);

        assertEquals("原田 太郎", res.getName());
        assertEquals("taro@harada.com", res.getEmail());
        assertEquals("神奈川県横浜市1-2-3", res.getAddress());
        assertEquals("090-1234-5678", res.getPhoneNumber());
        assertNotNull(res.getCreatedAt());
    }

    // --- register（重複メール）---
    @Test
    void createCustomer_DuplicateEmail_ShouldThrow() {
        CustomerInfo info = new CustomerInfo();
        info.setName("原田 太郎");
        info.setEmail("taro@harada.com");
        info.setAddress("神奈川県横浜市1-2-3");
        info.setPhoneNumber("090-1234-5678");

        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setCustomerInfo(info);
        req.setPassword("testpass123");

        when(customerRepository.existsByEmail("taro@harada.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> customerService.createCustomer(req));
    }

    // --- login 正常 ---
    @Test
    void login_ValidCredentials_ReturnsResponse() {
        Customer entity = new Customer();
        entity.setCustomerId(10);
        entity.setLastName("奈良田");
        entity.setFirstName("花子");
        entity.setEmail("hanako@narata.com");
        entity.setPassword("abcd1234");
        entity.setAddress("東京都品川区1-2-3");
        entity.setPhoneNumber("090-5678-0001");

        when(customerRepository.findByEmail("hanako@narata.com")).thenReturn(Optional.of(entity));

        CustomerResponse res = customerService.login("hanako@narata.com", "abcd1234");

        assertEquals("奈良田 花子", res.getName());
        assertEquals("090-5678-0001", res.getPhoneNumber());
    }

    // --- login メール未登録 ---
    @Test
    void login_EmailNotFound_ThrowsException() {
        when(customerRepository.findByEmail("not@found.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> customerService.login("not@found.com", "foo"));
    }

    // --- login パスワード不一致 ---
    @Test
    void login_WrongPassword_ThrowsException() {
        Customer entity = new Customer();
        entity.setCustomerId(9);
        entity.setLastName("鈴木");
        entity.setFirstName("次郎");
        entity.setEmail("jiro@suzuki.com");
        entity.setPassword("rightpw");

        when(customerRepository.findByEmail("jiro@suzuki.com")).thenReturn(Optional.of(entity));
        assertThrows(IllegalArgumentException.class, () -> customerService.login("jiro@suzuki.com", "wrongpw"));
    }

    // --- update 正常 ---
    @Test
    void updateCustomer_ValidInput_ShouldUpdate() {
        CustomerInfo info = new CustomerInfo();
        info.setName("鈴木 三郎");
        info.setEmail("saburo@suzuki.com");
        info.setAddress("愛知県名古屋市xx-yy-zz");
        info.setPhoneNumber("090-2222-1111");

        CustomerUpdateRequest req = new CustomerUpdateRequest();
        req.setCustomerInfo(info);
        req.setCurrentPassword("RIGHTPASS");
        req.setNewPassword("NEWPASS");

        Customer old = new Customer();
        old.setCustomerId(2);
        old.setLastName("鈴木");
        old.setFirstName("三郎");
        old.setEmail("saburo@suzuki.com");
        old.setPassword("RIGHTPASS");
        old.setAddress("愛知県名古屋市xx-yy-zz");
        old.setPhoneNumber("090-2222-1111");
        old.setCreatedAt(LocalDateTime.now().minusDays(10));

        when(customerRepository.findById(2)).thenReturn(Optional.of(old));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        CustomerResponse res = customerService.updateCustomer(2, req);
        assertEquals("鈴木 三郎", res.getName());
        assertEquals("saburo@suzuki.com", res.getEmail());
        assertEquals("NEWPASS", old.getPassword());
    }

    // --- update パスワード一致しない ---
    @Test
    void updateCustomer_WrongPassword_ThrowsException() {
        CustomerInfo info = new CustomerInfo();
        info.setName("名字 名前");
        info.setEmail("aaa@bbb.com");
        info.setAddress("どこかの町");
        info.setPhoneNumber("000-0000-0000");

        CustomerUpdateRequest req = new CustomerUpdateRequest();
        req.setCustomerInfo(info);
        req.setCurrentPassword("WRONGPASS");
        req.setNewPassword("NEWW");

        Customer old = new Customer();
        old.setCustomerId(10);
        old.setLastName("名字");
        old.setFirstName("名前");
        old.setEmail("aaa@bbb.com");
        old.setPassword("CORRECT");

        when(customerRepository.findById(10)).thenReturn(Optional.of(old));
        assertThrows(IllegalArgumentException.class, () -> customerService.updateCustomer(10, req));
    }

    // --- getCustomerById 正常 ---
    @Test
    void getCustomerById_Exists_ReturnsResponse() {
        Customer c = new Customer();
        c.setCustomerId(3);
        c.setLastName("高橋");
        c.setFirstName("花子");
        c.setEmail("hanako@takahashi.com");
        c.setPhoneNumber("090-7777-8888");
        c.setAddress("北海道札幌市xxx");
        c.setCreatedAt(LocalDateTime.now().minusYears(1));
        when(customerRepository.findById(3)).thenReturn(Optional.of(c));
        CustomerResponse result = customerService.getCustomerById(3);
        assertEquals("高橋 花子", result.getName());
        assertEquals("hanako@takahashi.com", result.getEmail());
    }

    // --- getCustomerById 不正 ---
    @Test
    void getCustomerById_NotFound_ThrowsException() {
        when(customerRepository.findById(-1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> customerService.getCustomerById(-1));
    }
}
