package com.socialvideo.video.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class AllowedDurationValidator implements ConstraintValidator<AllowedDuration, Short> {

    private static final Set<Short> ALLOWED = Set.of((short) 4, (short) 6, (short) 8);

    @Override
    public boolean isValid(Short value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotNull handles null
        return ALLOWED.contains(value);
    }
}
