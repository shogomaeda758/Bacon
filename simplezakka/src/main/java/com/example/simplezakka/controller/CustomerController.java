package com.example.simplezakka.controller;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    public static final String SESSION_CUSTOMER_ID = "loggedInCustomerId";
    public static final String SESSION_CUSTOMER_NAME = "loggedInCustomerName";


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CustomerRegisterRequest request) {
        try {
            CustomerResponse saved = customerService.createCustomer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException ex) {
            return buildError(HttpStatus.CONFLICT, "REGISTER_ERROR", ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CustomerLoginRequest request, HttpSession session) {
        try {
            CustomerResponse customer = customerService.login(request.getEmail(), request.getPassword());

            session.setAttribute(SESSION_CUSTOMER_ID, customer.getCustomerId());
            session.setAttribute(SESSION_CUSTOMER_NAME, customer.getName());

            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException ex) {
            return buildError(HttpStatus.UNAUTHORIZED, "LOGIN_ERROR", ex.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getLoginStatus(HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(SESSION_CUSTOMER_ID);
        String customerName = (String) session.getAttribute(SESSION_CUSTOMER_NAME);

        if (customerId != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("loggedIn", true);
            response.put("customerId", customerId);
            response.put("customerName", customerName);
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("loggedIn", false);
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        Map<String, String> response = new HashMap<>();
        response.put("message", "ログアウトしました。");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<?> update(@PathVariable Integer customerId,
                                    @RequestBody CustomerUpdateRequest request) {
        try {
            CustomerResponse updated = customerService.updateCustomer(customerId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return buildError(HttpStatus.BAD_REQUEST, "UPDATE_ERROR", ex.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(@RequestParam String keyword) {
        List<CustomerResponse> customers = customerService.searchByNameOrPhone(keyword);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Integer customerId) {
        try {
            CustomerResponse customer = customerService.getCustomerById(customerId);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
    @GetMapping("/profile")
    public ResponseEntity<CustomerResponse> getCustomerProfile(HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(SESSION_CUSTOMER_ID);
        if (customerId == null) {
            // ログインしていない場合は401 Unauthorizedを返す
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です。");
        }
        try {
            CustomerResponse customer = customerService.getCustomerById(customerId);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            // セッションに顧客IDはあるが、DBに存在しないなど（通常はありえないが念のため）
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "顧客情報が見つかりません。");
        }
    }


    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("errorCode", code);
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }
}