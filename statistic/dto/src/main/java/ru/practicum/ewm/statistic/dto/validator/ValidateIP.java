package ru.practicum.ewm.statistic.dto.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ElementType.FIELD})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = IPValidator.class)
public @interface ValidateIP {
    String message() default "Wrong IP address format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
