package com.tericcabrel.authorization.constraints.validators;

import com.tericcabrel.authorization.constraints.Exists;
import com.tericcabrel.authorization.utils.Helpers;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

@Component
public class ExistsValidator implements ConstraintValidator<Exists, Object> {
    private String propertyName;
    private String repositoryName;

    private final ApplicationContext applicationContext;

    public ExistsValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void initialize(final Exists constraintAnnotation) {
        propertyName = constraintAnnotation.property();
        repositoryName = constraintAnnotation.repository();
    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Skip validation if the value is null
        }

        try {
            // Resolve the repository bean dynamically
            String fullRepositoryName = "com.tericcabrel.authorization.repositories." + repositoryName;
            Class<?> repositoryClass = Class.forName(fullRepositoryName);
            Object repositoryInstance = applicationContext.getBean(repositoryClass);

            // Retrieve the property value
            Object propertyValue = BeanUtils.getPropertyDescriptor(value.getClass(), propertyName)
                    .getReadMethod().invoke(value);

            if (propertyValue == null) {
                return false; // If the property value is null, validation fails
            }

            // Build method name dynamically (e.g., "findByPropertyName")
            String methodName = "findBy" + Helpers.capitalize(propertyName);
            Object result = repositoryClass.getMethod(methodName, String.class)
                    .invoke(repositoryInstance, propertyValue.toString());

            // Return true if the result exists, false otherwise
            return result != null;

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false; // Validation fails if any exception occurs
        }
    }
}
