package com.example.simplezakka.controller;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.dto.customer.CustomerInfo;
import com.example.simplezakka.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CustomerService customerService;

    @Autowired
    ObjectMapper objectMapper;

    CustomerResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new CustomerResponse(
            1, "山田 太郎", "test@example.com", "東京都", "09011112222",
            LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void register_ValidInput_ShouldCreateCustomer() throws Exception {
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        CustomerInfo info = new CustomerInfo();
        info.setName("山田 太郎");
        info.setEmail("test@example.com");
        info.setAddress("東京都");
        info.setPhoneNumber("09011112222");
        req.setCustomerInfo(info); req.setPassword("securepw");

        when(customerService.createCustomer((CustomerRegisterRequest)any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("山田 太郎"));
    }

    @Test
    void register_DuplicateEmail_ShouldReturnConflict() throws Exception {
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        CustomerInfo info = new CustomerInfo();
        info.setName("重複 ユーザー");
        info.setEmail("dup@example.com");
        info.setAddress("東京都");
        info.setPhoneNumber("09012345678");
        req.setCustomerInfo(info); req.setPassword("dup1234");

        when(customerService.createCustomer((CustomerRegisterRequest)any()))
            .thenThrow(new IllegalArgumentException("メールアドレスが既に登録されています"));

        mockMvc.perform(post("/api/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode", is("REGISTER_ERROR")))
            .andExpect(jsonPath("$.message", containsString("メールアドレスが既に登録されています")));
    }

    @Test
    void register_InvalidInput_Should400ValidationError() throws Exception {
        String invalidJson = "{\"customerInfo\":{\"name\":\"\",\"email\":\"notmail\",\"address\":\"\",\"phoneNumber\":\"123\"},\"password\":\"\"}";

        mockMvc.perform(post("/api/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
            // 通常はfieldErrorsなどバリデーション結果の検証だが、@Validがないためエラーbody検証は標準仕様になる
    }

    @Test
    void login_ValidCredential_ShouldSetSession() throws Exception {
        CustomerLoginRequest req = new CustomerLoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("securepw");

        when(customerService.login("test@example.com", "securepw"))
            .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/customers/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("山田 太郎"));
    }

    @Test
    void login_EmailNotFound_ShouldReturn401() throws Exception {
        CustomerLoginRequest req = new CustomerLoginRequest();
        req.setEmail("notfound@example.com");
        req.setPassword("pw");
        when(customerService.login(eq("notfound@example.com"), anyString()))
            .thenThrow(new IllegalArgumentException("メールアドレスが見つかりません"));

        mockMvc.perform(post("/api/customers/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode", is("LOGIN_ERROR")))
            .andExpect(jsonPath("$.message", containsString("メールアドレスが見つかりません")));
    }

    @Test
    void login_InvalidPassword_ShouldReturn401() throws Exception {
        CustomerLoginRequest req = new CustomerLoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrongpw");
        when(customerService.login(eq("test@example.com"), eq("wrongpw")))
            .thenThrow(new IllegalArgumentException("パスワードが正しくありません"));

        mockMvc.perform(post("/api/customers/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode", is("LOGIN_ERROR")))
            .andExpect(jsonPath("$.message", containsString("パスワードが正しくありません")));
    }

    @Test
    void login_MissingField_Should400ValidationError() throws Exception {
        String invalidJson = "{\"email\":\"\", \"password\":\"\"}";
        mockMvc.perform(post("/api/customers/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void status_LoggedIn_ShouldReturnUserInfo() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInCustomerId", 1);
        session.setAttribute("loggedInCustomerName", "山田 太郎");

        mockMvc.perform(get("/api/customers/status").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedIn").value(true))
            .andExpect(jsonPath("$.customerId").value(1))
            .andExpect(jsonPath("$.customerName").value("山田 太郎"));
    }

    @Test
    void status_NotLoggedIn_ShouldReturnFalse() throws Exception {
        mockMvc.perform(get("/api/customers/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedIn").value(false));
    }

    @Test
    void logout_ShouldInvalidateSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInCustomerId", 1);

        mockMvc.perform(post("/api/customers/logout").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", containsString("ログアウトしました")));
    }

    @Test
    void updateCustomer_ValidInput_ShouldSucceed() throws Exception {
        CustomerUpdateRequest req = new CustomerUpdateRequest();
        CustomerInfo info = new CustomerInfo();
        info.setName("佐藤 花子");
        info.setEmail("hanako@sato.com");
        info.setAddress("神奈川県");
        info.setPhoneNumber("08023456789");
        req.setCustomerInfo(info); req.setCurrentPassword("oldpw"); req.setNewPassword("newpw");

        CustomerResponse updated = new CustomerResponse(
            2, "佐藤 花子", "hanako@sato.com", "神奈川県", "08023456789", LocalDateTime.now(), LocalDateTime.now()
        );
        when(customerService.updateCustomer(eq(2), (CustomerUpdateRequest)any())).thenReturn(updated);

        mockMvc.perform(put("/api/customers/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("佐藤 花子"));
    }

    @Test
    void updateCustomer_WrongPassword_Should400() throws Exception {
        CustomerUpdateRequest req = new CustomerUpdateRequest();
        CustomerInfo info = new CustomerInfo();
        info.setName("佐藤 花子");
        info.setEmail("hanako@sato.com");
        info.setAddress("神奈川県");
        info.setPhoneNumber("08023456789");
        req.setCustomerInfo(info); req.setCurrentPassword("wrongpw"); req.setNewPassword("newpw");

        when(customerService.updateCustomer(eq(2), (CustomerUpdateRequest)any()))
            .thenThrow(new IllegalArgumentException("パスワードが正しくありません"));

        mockMvc.perform(put("/api/customers/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode", is("UPDATE_ERROR")))
            .andExpect(jsonPath("$.message", containsString("パスワードが正しくありません")));
    }

    @Test
    void updateCustomer_MissingField_Should400ValidationError() throws Exception {
        String invalidJson = "{\"customerInfo\":{\"name\":\"\",\"email\":\"invalid\",\"address\":\"\",\"phoneNumber\":\"123\"},\"currentPassword\":\"\",\"newPassword\":\"\"}";
        mockMvc.perform(put("/api/customers/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchCustomers_ShouldReturnList() throws Exception {
        when(customerService.searchByNameOrPhone("山田"))
            .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/customers/search?keyword=山田"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("山田 太郎"));
    }

    @Test
    void getCustomer_Success() throws Exception {
        when(customerService.getCustomerById(1)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/customers/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("山田 太郎"));
    }

    @Test
    void getCustomer_NotFound_ShouldReturn404() throws Exception {
        when(customerService.getCustomerById(1234))
            .thenThrow(new IllegalArgumentException("該当会員が見つかりません"));

        mockMvc.perform(get("/api/customers/1234"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getCustomerProfile_Success() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInCustomerId", 1);
        when(customerService.getCustomerById(1)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/customers/profile").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("山田 太郎"));
    }

    @Test
    void getCustomerProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/customers/profile"))
            .andExpect(status().isUnauthorized());
    }
}
