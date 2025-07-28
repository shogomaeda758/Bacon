package com.example.simplezakka.dto.customer;

import lombok.Getter;
import lombok.Setter;

import com.example.simplezakka.annotation.HalfWidth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class CustomerLoginRequest {

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    @HalfWidth(message = "メールアドレスは半角数字で入力してください。")
    private String email;

    @NotBlank(message = "パスワードは必須です")
    @HalfWidth(message = "パスワードは半角文字で入力してください。")
    private String password;
}