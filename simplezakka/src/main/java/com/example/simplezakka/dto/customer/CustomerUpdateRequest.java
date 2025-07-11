package com.example.simplezakka.dto.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerUpdateRequest {

    @Valid
    private CustomerInfo customerInfo;

    @NotBlank(message = "現在のパスワードは必須です")
    private String currentPassword;

    private String newPassword;
}
