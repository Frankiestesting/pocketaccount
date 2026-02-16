package com.frnholding.pocketaccount.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = NorwegianAccountNoValidator.class)
@Target({ FIELD })
@Retention(RUNTIME)
public @interface NorwegianAccountNo {
    String message() default "Invalid Norwegian account number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
