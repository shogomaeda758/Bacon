package com.example.simplezakka.dto.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRegisterRequest {

    @Valid
    private CustomerInfo customerInfo;

    @NotBlank(message = "パスワードは必須です")
    private String password;
}
