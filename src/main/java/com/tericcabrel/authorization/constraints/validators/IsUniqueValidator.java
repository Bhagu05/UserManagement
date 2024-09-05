package com.tericcabrel.authorization.constraints.validators;

import com.tericcabrel.authorization.constraints.IsUnique;
import com.tericcabrel.authorization.utils.Helpers;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

@Component
public class IsUniqueValidator implements ConstraintValidator<IsUnique, Object> {
    private String propertyName;
    private String repositoryName;
    private UpdateAction action;

    private final ApplicationContext applicationContext;

    public IsUniqueValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void initialize(final IsUnique constraintAnnotation) {
        propertyName = constraintAnnotation.property();
        repositoryName = constraintAnnotation.repository();
        action = constraintAnnotation.action();
    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
        try {
            // Construct the repository bean name dynamically
            String fullRepositoryName = "com.tericcabrel.authorization.repositories." + repositoryName;
            Class<?> repositoryClass = Class.forName(fullRepositoryName);
            Object repositoryInstance = applicationContext.getBean(repositoryClass);

            // Get the property value to check for uniqueness
            Object propertyValue = BeanUtils.getPropertyDescriptor(value.getClass(), propertyName)
                    .getReadMethod().invoke(value);

            if (propertyValue == null) {
                return true; // Skip validation if the property is null
            }

            // Construct method name dynamically (e.g., "findByPropertyName")
            String methodName = "findBy" + Helpers.capitalize(propertyName);
            Object result = repositoryClass.getMethod(methodName, String.class).invoke(repositoryInstance, propertyValue.toString());

            if (action == UpdateAction.INSERT) {
                return result == null; // For inserts, return true if no record exists
            } else if (action == UpdateAction.UPDATE) {
                // For updates, ensure the IDs match
                Object currentId = BeanUtils.getPropertyDescriptor(value.getClass(), "id").getReadMethod().invoke(value);
                if (result != null) {
                    Object resultId = result.getClass().getMethod("getId").invoke(result);
                    return currentId != null && currentId.equals(resultId);
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return false; // Validation fails if an exception occurs
        }

        return false; // Default to false if no conditions are met
    }

    public enum UpdateAction {
        INSERT,
        UPDATE
    }
}
