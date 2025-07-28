package com.example.simplezakka.validator;

import com.example.simplezakka.annotation.FullWidth;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FullWidthValidator implements ConstraintValidator<FullWidth, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // @NotBlank でnullや空文字は別途チェックされるため、ここではtrueを返す
        }
        return value.chars().allMatch(c -> Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN &&
                                         Character.UnicodeBlock.of(c) != Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
    }
}