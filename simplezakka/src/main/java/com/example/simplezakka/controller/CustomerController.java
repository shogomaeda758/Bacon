package com.example.simplezakka.controller;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CustomerRegisterRequest request) {
        try {
            CustomerResponse saved = customerService.createCustomer(request);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return buildError("REGISTER_ERROR", ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        try {
            CustomerResponse customer = customerService.login(email, password);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException ex) {
            return buildError("LOGIN_ERROR", ex.getMessage());
        }
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<?> update(@PathVariable Integer customerId,
                                    @RequestBody CustomerUpdateRequest request) {
        try {
            CustomerResponse updated = customerService.updateCustomer(customerId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return buildError("UPDATE_ERROR", ex.getMessage());
        }
    }

    
    private ResponseEntity<Map<String, Object>> buildError(String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("errorCode", code);
        error.put("message", message);
        return ResponseEntity.badRequest().body(error);
    }
}
