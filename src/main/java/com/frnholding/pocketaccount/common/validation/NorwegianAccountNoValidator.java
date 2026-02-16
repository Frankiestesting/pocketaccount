package com.frnholding.pocketaccount.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NorwegianAccountNoValidator implements ConstraintValidator<NorwegianAccountNo, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String digits = value.trim();
        if (!digits.matches("\\d{11}")) {
            return false;
        }

        int[] weights = { 2, 3, 4, 5, 6, 7, 2, 3, 4, 5 };
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            int digit = digits.charAt(9 - i) - '0';
            sum += digit * weights[i];
        }

        int remainder = sum % 11;
        int control = 11 - remainder;
        if (control == 11) {
            control = 0;
        }

        if (control == 10) {
            return false;
        }

        int lastDigit = digits.charAt(10) - '0';
        return lastDigit == control;
    }
}
