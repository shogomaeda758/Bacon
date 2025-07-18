package com.example.simplezakka.service;

import com.example.simplezakka.dto.customer.*;
import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCryptPasswordEncoderをインポート

import java.util.List;
import java.util.stream.Collectors;

@Service
//@RequiredArgsConstructor // PasswordEncoderのインジェクション方法が変わるのでコメントアウトまたは削除
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BCryptPasswordEncoder passwordEncoder; // PasswordEncoderの代わりにBCryptPasswordEncoderを使う

    // コンストラクタを自分で定義し、BCryptPasswordEncoderを初期化する
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(); // ここでインスタンス化
    }

    /** 会員登録*/
    public CustomerResponse createCustomer(CustomerRegisterRequest request) {
        CustomerInfo info = request.getCustomerInfo();

        if (customerRepository.existsByEmail(info.getEmail())) {
            throw new IllegalArgumentException("既にこのメールアドレスは登録されています。");
        }

        Customer customer = new Customer();
        String[] names = splitName(info.getName());
        customer.setLastName(names[0]);
        customer.setFirstName(names[1]);
        customer.setEmail(info.getEmail());
        customer.setAddress(info.getAddress());
        customer.setPhoneNumber(info.getPhoneNumber());
        // BCryptPasswordEncoderを使ってハッシュ化
        customer.setPassword(passwordEncoder.encode(request.getPassword()));

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    /**ログイン*/
    public CustomerResponse login(String email, String password) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("メールアドレスが見つかりません。"));

        // BCryptPasswordEncoderを使ってパスワードを検証
        if (!passwordEncoder.matches(password, customer.getPassword())) {
            throw new IllegalArgumentException("パスワードが正しくありません。");
        }

        return toResponse(customer);
    }

    /**名前または電話番号で検索 */
    public List<CustomerResponse> searchByNameOrPhone(String keyword) {
        List<Customer> customers = customerRepository
                .findByLastNameContainingOrFirstNameContainingOrPhoneNumberContaining(
                        keyword, keyword, keyword);

        return customers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**会員情報取得 */
    public CustomerResponse getCustomerById(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません"));
        return toResponse(customer);
    }

    /** 会員情報更新*/
    public CustomerResponse updateCustomer(Integer customerId, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません"));

        // BCryptPasswordEncoderを使って現在のパスワードを検証
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
            // BCryptPasswordEncoderを使って新しいパスワードをハッシュ化
            customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        Customer updated = customerRepository.save(customer);
        return toResponse(updated);
    }

    /**nameを「姓」「名」に分割*/
    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split(" ", 2);
        String lastName = parts.length > 0 ? parts[0] : "";
        String firstName = parts.length > 1 ? parts[1] : "";
        return new String[]{lastName, firstName};
    }

    /**DTO変換*/
    private CustomerResponse toResponse(Customer customer) {
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