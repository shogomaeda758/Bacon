package com.example.simplezakka.controller;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus; // HttpStatusをインポート
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // ResponseStatusExceptionをインポート

import jakarta.servlet.http.HttpSession; // HttpSessionをインポート
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // 定数としてセッション属性名を定義
    public static final String SESSION_CUSTOMER_ID = "loggedInCustomerId";
    public static final String SESSION_CUSTOMER_NAME = "loggedInCustomerName";


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CustomerRegisterRequest request) {
        try {
            CustomerResponse saved = customerService.createCustomer(request);
            // 登録後、自動ログインさせる場合はここでセッションに保存
            // HttpSession session に保存する場合は、ここにセッション保存ロジックを追加
            return ResponseEntity.status(HttpStatus.CREATED).body(saved); // 201 Created を返す
        } catch (IllegalArgumentException ex) {
            return buildError(HttpStatus.CONFLICT, "REGISTER_ERROR", ex.getMessage()); // 409 Conflict
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CustomerLoginRequest request, HttpSession session) { // ★HttpSessionをインジェクション
        try {
            CustomerResponse customer = customerService.login(request.getEmail(), request.getPassword());
            
            // ログイン成功: セッションにユーザー情報を保存
            session.setAttribute(SESSION_CUSTOMER_ID, customer.getCustomerId());
            session.setAttribute(SESSION_CUSTOMER_NAME, customer.getName()); // フロントエンドで利用するため
            
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException ex) {
            // 認証失敗: 401 Unauthorized を返す
            return buildError(HttpStatus.UNAUTHORIZED, "LOGIN_ERROR", ex.getMessage());
        }
    }

    // ログイン状態確認エンドポイント
    @GetMapping("/status")
    public ResponseEntity<?> getLoginStatus(HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(SESSION_CUSTOMER_ID);
        String customerName = (String) session.getAttribute(SESSION_CUSTOMER_NAME);

        if (customerId != null) {
            // ログイン中の場合、ユーザー情報を返す
            Map<String, Object> response = new HashMap<>();
            response.put("loggedIn", true);
            response.put("customerId", customerId);
            response.put("customerName", customerName);
            return ResponseEntity.ok(response);
        } else {
            // ログインしていない場合
            Map<String, Object> response = new HashMap<>();
            response.put("loggedIn", false);
            return ResponseEntity.ok(response);
        }
    }

    // ログアウトエンドポイント
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate(); // セッションを無効化する
        Map<String, String> response = new HashMap<>();
        response.put("message", "ログアウトしました。");
        return ResponseEntity.ok(response);
    }


    // 会員情報更新、検索などは省略（変更なし）
    @PutMapping("/{customerId}")
    public ResponseEntity<?> update(@PathVariable Integer customerId,
                                    @RequestBody CustomerUpdateRequest request) {
        try {
            CustomerResponse updated = customerService.updateCustomer(customerId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return buildError(HttpStatus.BAD_REQUEST, "UPDATE_ERROR", ex.getMessage()); // 400 Bad Request
        }
    }

    // 検索エンドポイント
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(@RequestParam String keyword) {
        List<CustomerResponse> customers = customerService.searchByNameOrPhone(keyword);
        return ResponseEntity.ok(customers);
    }
    
    // 会員情報取得エンドポイント
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Integer customerId) {
        try {
            CustomerResponse customer = customerService.getCustomerById(customerId);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()); // 404 Not Found
        }
    }


    // エラーレスポンスのヘルパーメソッドにHttpStatusを追加
    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("errorCode", code);
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }
}