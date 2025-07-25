package com.example.simplezakka.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import com.example.simplezakka.validator.FullWidthValidator;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FullWidthValidator.class)
@Documented
public @interface FullWidth {
    String message() default "全角文字で入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}