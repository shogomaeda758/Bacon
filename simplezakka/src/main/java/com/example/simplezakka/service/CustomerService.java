package com.example.simplezakka.service;

import com.example.simplezakka.dto.customer.CustomerRegisterRequest;
import com.example.simplezakka.dto.customer.CustomerResponse;
import com.example.simplezakka.dto.customer.CustomerUpdateRequest;
import com.example.simplezakka.entity.CustomerEntity;
import com.example.simplezakka.repository.CustomerRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CustomerService {

    private static final String SESSION_CUSTOMER_KEY = "customerId";

    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public CustomerService(CustomerRepository customerRepository, 
                           EmailService emailService) {
        this.customerRepository = customerRepository;
        this.emailService = emailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public CustomerResponse registerCustomer(CustomerRegisterRequest request) {
        if (customerRepository.existsByEmail(request.getCustomerInfo().getEmail())) {
            throw new IllegalArgumentException("既に登録されているメールアドレスです。");
        }

        CustomerEntity customer = new CustomerEntity();
        customer.setFirstName(request.getCustomerInfo().getName());
        customer.setEmail(request.getCustomerInfo().getEmail());
        customer.setAddress(request.getCustomerInfo().getAddress());
        customer.setPhoneNumber(request.getCustomerInfo().getPhoneNumber());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        CustomerEntity saved = customerRepository.save(customer);

        // Amazon SES 経由で登録完了メール送信（仮）
        emailService.sendRegistrationComplete(saved.getEmail(), saved.getFirstName(), saved.getLastName());

        return toResponse(saved);
    }

    public boolean authenticate(String email, String rawPassword, HttpSession session) {
        Optional<CustomerEntity> optCustomer = customerRepository.findByEmail(email);

        if (optCustomer.isPresent() &&
            passwordEncoder.matches(rawPassword, optCustomer.get().getPasswordHash())) {

            session.setAttribute(SESSION_CUSTOMER_KEY, optCustomer.get().getCustomerId());
            return true;
        }
        return false;
    }

    public CustomerResponse getCustomerById(Integer customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("顧客が見つかりません。"));

        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Integer customerId, CustomerUpdateRequest request) {
        CustomerEntity customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("顧客が見つかりません。"));

        // パスワードチェック
        if (!passwordEncoder.matches(request.getCurrentPassword(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("現在のパスワードが一致しません。");
        }

        // 更新
        customer.setFirstName(request.getCustomerInfo().getName());
        customer.setLastName(request.getCustomerInfo().getName());
        customer.setEmail(request.getCustomerInfo().getEmail());
        customer.setAddress(request.getCustomerInfo().getAddress());
        customer.setPhoneNumber(request.getCustomerInfo().getPhoneNumber());

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        customer.setUpdatedAt(LocalDateTime.now());
        CustomerEntity updated = customerRepository.save(customer);

        // Amazon SES 経由で更新完了メール送信（仮）
        emailService.sendProfileUpdateComplete(updated.getEmail(), updated.getFirstName(), updated.getLastName());

        return toResponse(updated);
    }

    private CustomerResponse toResponse(CustomerEntity customer) {
    String fullName = customer.getLastName() + " " + customer.getFirstName();
    return new CustomerResponse(
            customer.getCustomerId(),
            fullName,
            customer.getEmail(),
            customer.getAddress(),
            customer.getPhoneNumber(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}

