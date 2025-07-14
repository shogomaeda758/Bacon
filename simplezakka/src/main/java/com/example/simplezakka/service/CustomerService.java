package com.example.simplezakka.service;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.entity.CustomerEntity;
import com.example.simplezakka.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 会員登録
     */
    public CustomerResponse createCustomer(CustomerRegisterRequest request) {
        CustomerInfo info = request.getCustomerInfo();

        if (customerRepository.existsByEmail(info.getEmail())) {
            throw new IllegalArgumentException("既にこのメールアドレスは登録されています。");
        }

        CustomerEntity customer = new CustomerEntity();
        String[] names = splitName(info.getName());
        customer.setLastName(names[0]);
        customer.setFirstName(names[1]);
        customer.setEmail(info.getEmail());
        customer.setAddress(info.getAddress());
        customer.setPhoneNumber(info.getPhoneNumber());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));

        CustomerEntity saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    /**
     * ログイン
     */
    public CustomerResponse login(String email, String password) {
        CustomerEntity customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("メールアドレスが見つかりません。"));

        if (!passwordEncoder.matches(password, customer.getPassword())) {
            throw new IllegalArgumentException("パスワードが正しくありません。");
        }

        return toResponse(customer);
    }
    
/**
     * 名前または電話番号で検索
     */
    public List<CustomerResponse> searchByNameOrPhone(String keyword) {
    // 部分一致で名前・電話番号検索
    List<CustomerEntity> customers = customerRepository
            .findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining(
                    keyword, keyword, keyword);

        return customers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 会員情報取得
     */
    public CustomerResponse getCustomerById(Integer customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません"));
        return toResponse(customer);
    }

    /**
     * 会員情報更新
     */
    public CustomerResponse updateCustomer(Integer customerId, CustomerUpdateRequest request) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), customer.getPassword())) {
            throw new IllegalArgumentException("現在のパスワードが正しくありません");
        }

        CustomerInfo info = request.getCustomerInfo();
        String[] names = splitName(info.getName());
        customer.setLastName(names[0]);
        customer.setFirstName(names[1]);
        customer.setEmail(info.getEmail());
        customer.setAddress(info.getAddress());
        customer.setPhoneNumber(info.getPhoneNumber());

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        CustomerEntity updated = customerRepository.save(customer);
        return toResponse(updated);
    }

    

    /**
     * nameを「姓」「名」に分割
     */
    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split(" ", 2);
        String lastName = parts.length > 0 ? parts[0] : "";
        String firstName = parts.length > 1 ? parts[1] : "";
        return new String[]{lastName, firstName};
    }

    /**
     * DTO変換
     */
    private CustomerResponse toResponse(CustomerEntity customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getAddress(),
                customer.getPhoneNumber(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}

