package com.exercise.contactBook.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if(phone == null || phone.isBlank()) return true;
        try{
            var parsed = PHONE_UTIL.parse(phone,"IN");
            return PHONE_UTIL.isValidNumber(parsed);
        }catch (NumberParseException e) {
            return false;
        }
    }
}
