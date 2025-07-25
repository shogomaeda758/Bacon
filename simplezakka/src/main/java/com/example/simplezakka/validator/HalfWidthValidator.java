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
        // 半角英数字、記号を許可。全角文字が含まれていないかチェック
        return value.chars().allMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN ||
                                         Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
    }
}