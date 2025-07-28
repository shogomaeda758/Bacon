package com.example.simplezakka.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import com.example.simplezakka.validator.HalfWidthValidator;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HalfWidthValidator.class)
@Documented
public @interface HalfWidth {
    String message() default "半角文字で入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}