package com.socialvideo.video.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AllowedDurationValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedDuration {
    String message() default "La duración debe ser 4, 6 u 8 segundos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
