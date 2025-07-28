package com.example.simplezakka.validator;

import com.example.simplezakka.annotation.HalfWidth;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HalfWidthValidator implements ConstraintValidator<HalfWidth, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // @NotBlank でnullや空文字は別途チェックされるため、ここではtrueを返す
        }

        // ここを修正：BASIC_LATIN（半角英数字記号）のみを許可する
        // 全角文字や半角カタカナなどはすべて弾く
        return value.chars().allMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN);
    }
}