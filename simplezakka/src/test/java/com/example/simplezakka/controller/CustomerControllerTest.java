package com.example.simplezakka.controller;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CustomerService customerService;
    private ObjectMapper objectMapper;
    private CustomerResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sampleResponse = new CustomerResponse(
                1, "山田 太郎", "yamada@example.com", "東京都", "090-1111-2222",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("会員登録API")
    class RegisterTests {
        @Test
        @DisplayName("会員登録成功")
        void register_Success() throws Exception {
            CustomerInfo info = new CustomerInfo();
            info.setName("鈴木 一郎");
            info.setEmail("ichiro@suzuki.net");
            info.setAddress("札幌市");
            info.setPhoneNumber("080-2222-2222");
            CustomerRegisterRequest req = new CustomerRegisterRequest();
            req.setCustomerInfo(info);
            req.setPassword("regpass123");

            CustomerResponse regResponse = new CustomerResponse(
                    2, "鈴木 一郎", "ichiro@suzuki.net", "札幌市", "080-2222-2222",
                    LocalDateTime.now(), LocalDateTime.now());

            when(customerService.createCustomer((CustomerRegisterRequest)any())).thenReturn(regResponse);

            mockMvc.perform(post("/api/customers/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("鈴木 一郎"))
                .andExpect(jsonPath("$.email").value("ichiro@suzuki.net"));
        }

        @Test
        @DisplayName("会員登録（メール重複）")
        void register_DuplicateEmail() throws Exception {
            CustomerInfo info = new CustomerInfo();
            info.setName("重複 太郎");
            info.setEmail("duplicate@mail.com");
            info.setAddress("東京都");
            info.setPhoneNumber("090-1234-5678");
            CustomerRegisterRequest req = new CustomerRegisterRequest();
            req.setCustomerInfo(info);
            req.setPassword("pwdup");

            when(customerService.createCustomer((CustomerRegisterRequest)any()))
                .thenThrow(new IllegalArgumentException("メールアドレスが既に登録されています"));

            mockMvc.perform(post("/api/customers/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("メールアドレスが既に登録されています")));
        }
    }

    @Nested
    @DisplayName("ログインAPI")
    class LoginTests {
        @Test
        @DisplayName("ログイン成功")
        void login_Success() throws Exception {
            CustomerLoginRequest req = new CustomerLoginRequest();
            req.setEmail("yamada@example.com");
            req.setPassword("passw0rd");
            when(customerService.login(eq("yamada@example.com"), eq("passw0rd")))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/customers/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("山田 太郎"));
        }

        @Test
        @DisplayName("ログイン失敗（メール未登録）")
        void login_Fail_EmailNotFound() throws Exception {
            CustomerLoginRequest req = new CustomerLoginRequest();
            req.setEmail("notfound@example.com");
            req.setPassword("abc");
            when(customerService.login(eq("notfound@example.com"), anyString()))
                    .thenThrow(new IllegalArgumentException("メールアドレスが見つかりません"));

            mockMvc.perform(post("/api/customers/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", containsString("メールアドレスが見つかりません")));
        }

        @Test
        @DisplayName("ログイン失敗（パスワード不一致）")
        void login_Fail_WrongPassword() throws Exception {
            CustomerLoginRequest req = new CustomerLoginRequest();
            req.setEmail("yamada@example.com");
            req.setPassword("wrongpw");
            when(customerService.login(anyString(), eq("wrongpw")))
                    .thenThrow(new IllegalArgumentException("パスワードが正しくありません"));

            mockMvc.perform(post("/api/customers/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", containsString("パスワードが正しくありません")));
        }
    }

    @Nested
    @DisplayName("ログイン状態確認API")
    class LoginStatusTests {
        @Test
        @DisplayName("ログイン状態確認（ログイン済）")
        void status_LoggedIn() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("loggedIncustomerId", 1);
            session.setAttribute("loggedIncustomerName", "山田 太郎");

            mockMvc.perform(get("/api/customers/status").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loggedIn", is(true)))
                    .andExpect(jsonPath("$.customerId").value(1));
        }

        @Test
        @DisplayName("ログイン状態確認（未ログイン）")
        void status_NotLoggedIn() throws Exception {
            mockMvc.perform(get("/api/customers/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loggedIn", is(false)));
        }
    }

    @Nested
    @DisplayName("ログアウトAPI")
    class LogoutTests {
        @Test
        @DisplayName("ログアウト成功")
        void logout_Success() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("customerId", 1);

            mockMvc.perform(post("/api/customers/logout").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("ログアウトしました")));
        }
    }

    @Nested
    @DisplayName("会員情報更新API")
    class UpdateCustomerTests {
        @Test
        @DisplayName("会員情報更新成功")
        void updateCustomer_Success() throws Exception {
            CustomerUpdateRequest req = new CustomerUpdateRequest();
            CustomerInfo info = new CustomerInfo();
            info.setName("佐藤 一郎");
            info.setEmail("ichiro@sato.net");
            info.setAddress("札幌市");
            info.setPhoneNumber("080-2222-2222");
            req.setCustomerInfo(info);
            req.setCurrentPassword("pass1");
            req.setNewPassword("pass2");

            CustomerResponse updated = new CustomerResponse(
                    2, "佐藤 一郎", "ichiro@sato.net", "札幌市", "080-2222-2222",
                    LocalDateTime.now(), LocalDateTime.now());

            when(customerService.updateCustomer(eq(2), (CustomerUpdateRequest)any())).thenReturn(updated);

            mockMvc.perform(put("/api/customers/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("佐藤 一郎"));
        }

        @Test
        @DisplayName("会員情報更新失敗（認証NG）")
        void updateCustomer_Fail_InvalidPassword() throws Exception {
            CustomerUpdateRequest req = new CustomerUpdateRequest();
            CustomerInfo info = new CustomerInfo();
            info.setName("佐藤 一郎");
            info.setEmail("ichiro@sato.net");
            info.setAddress("札幌市");
            info.setPhoneNumber("080-2222-2222");
            req.setCustomerInfo(info);
            req.setCurrentPassword("wrongpassword");
            req.setNewPassword("pass2");

            when(customerService.updateCustomer(eq(2),(CustomerUpdateRequest)any()))
                    .thenThrow(new IllegalArgumentException("パスワードが正しくありません"));

            mockMvc.perform(put("/api/customers/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("パスワードが正しくありません")));
        }
    }

}
