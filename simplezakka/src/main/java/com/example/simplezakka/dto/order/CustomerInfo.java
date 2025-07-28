package com.example.simplezakka.dto.order;

import com.example.simplezakka.annotation.FullWidth;
import com.example.simplezakka.annotation.HalfWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInfo {
    
    private Long customerId;

    @NotBlank(message = "氏名は必須です。")
    @Size(max = 100, message = "氏名は100文字以内で入力してください。")
    @FullWidth(message = "氏名は全角文字で入力してください。") // 氏名: 全角指定
    private String name; 

    @NotBlank(message = "メールアドレスは必須です。")
    @Email(message = "有効なメールアドレス形式で入力してください。")
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください。")
    @HalfWidth(message = "メールアドレスは半角文字で入力してください。") // メールアドレス: 半角指定
    private String email;

    @NotBlank(message = "住所は必須です。")
    @Size(max = 500, message = "住所は500文字以内で入力してください。")
    private String address;

    @NotBlank(message = "電話番号は必須です。")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "電話番号は10桁または11桁の数字で入力してください。")
    @HalfWidth(message = "電話番号は半角数字で入力してください。") // 電話番号: 半角指定
    private String phoneNumber;
}