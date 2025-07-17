package com.example.simplezakka.controller;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.service.CustomerService;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CustomerService customerService;

    ObjectMapper objectMapper = new ObjectMapper();

    // 新規登録
    @Test
    void register_ValidInput_ShouldCreateCustomer() throws Exception {
        CustomerInfo info = new CustomerInfo();
        info.setName("中村 美咲");
        info.setEmail("misaki@nkmr.com");
        info.setAddress("北九州市");
        info.setPhoneNumber("080-9999-0000");

        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setCustomerInfo(info);
        req.setPassword("secure");

        CustomerResponse resp =
            new CustomerResponse(1, "中村 美咲", "misaki@nkmr.com", "北九州市", "080-9999-0000", LocalDateTime.now(), LocalDateTime.now());

        when(customerService.createCustomer(any())).thenReturn(resp);

        mockMvc.perform(post("/api/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("中村 美咲"));
    }

    // 登録・重複メール
    @Test
    void register_DuplicateEmail_ShouldReturnConflict() throws Exception {
        CustomerInfo info = new CustomerInfo();
        info.setName("佐々木 誠");
        info.setEmail("sasaki@makoto.com");
        info.setAddress("神戸市");
        info.setPhoneNumber("080-5555-7777");

        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setCustomerInfo(info);
        req.setPassword("abc");

        when(customerService.createCustomer(any()))
            .thenThrow(new IllegalArgumentException("メールがすでに登録されています"));

        mockMvc.perform(post("/api/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(containsString("メールがすでに登録されています")));
    }

    // ログイン正常
    @Test
    void login_ValidCredentials_ShouldReturnCustomer() throws Exception {
        CustomerLoginRequest req = new CustomerLoginRequest();
        req.setEmail("tanaka@domain.com");
        req.setPassword("passw0rd");

        CustomerResponse resp =
            new CustomerResponse(22, "田中 真", "tanaka@domain.com", "横浜市", "090-1111-1111", LocalDateTime.now(), LocalDateTime.now());
        when(customerService.login(eq("tanaka@domain.com"), eq("passw0rd"))).thenReturn(resp);

        mockMvc.perform(post("/api/customers/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("田中 真"));
    }

    // ログイン失敗（パスワード不一致）
    @Test
    void login_WrongPassword_ShouldReturn401() throws Exception {
        CustomerLoginRequest req = new CustomerLoginRequest();
        req.setEmail("tanaka@domain.com");
        req.setPassword("wrongPassword");
        when(customerService.login(anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("パスワードが正しくありません"));

        mockMvc.perform(post("/api/customers/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(containsString("パスワードが正しくありません")));
    }

    // 顧客取得
    @Test
    void getCustomer_ExistingId_ShouldReturnData() throws Exception {
        CustomerResponse resp =
            new CustomerResponse(1, "佐藤 杏", "an.sato@mail.com", "大分県", "090-0000-9999", LocalDateTime.now(), LocalDateTime.now());

        when(customerService.getCustomerById(1)).thenReturn(resp);

        mockMvc.perform(get("/api/customers/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("佐藤 杏"));
    }
}

