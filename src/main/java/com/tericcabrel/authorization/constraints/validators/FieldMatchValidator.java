package com.tericcabrel.authorization.constraints.validators;

import com.tericcabrel.authorization.constraints.FieldMatch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
// import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.BeanUtils;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {
    private String firstFieldName;
    private String secondFieldName;

    @Override
    public void initialize(final FieldMatch constraintAnnotation) {
        firstFieldName = constraintAnnotation.first();
        secondFieldName = constraintAnnotation.second();
    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
        try {
            final Object firstObj = BeanUtils.getPropertyDescriptor(value.getClass(), firstFieldName);
            final Object secondObj = BeanUtils.getPropertyDescriptor(value.getClass(), secondFieldName);

            return firstObj == null && secondObj == null || (firstObj != null && firstObj.equals(secondObj));
        } catch (final Exception e) {
            return false; // Return false if any exception occurs during property comparison
        }
    }
}
