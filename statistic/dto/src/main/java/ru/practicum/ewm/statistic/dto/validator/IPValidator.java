package ru.practicum.ewm.statistic.dto.validator;

import org.apache.commons.validator.routines.InetAddressValidator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IPValidator implements ConstraintValidator<ValidateIP, String> {
    public void initialize(IPValidator parameters) {
    }

    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValid(value);
    }
}
