package com.tericcabrel.authorization.models.dtos;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.tericcabrel.authorization.constraints.IsUnique;
import com.tericcabrel.authorization.constraints.FieldMatch;
import com.tericcabrel.authorization.models.entities.Role;
import com.tericcabrel.authorization.models.entities.Coordinates;

@Schema(name = "RegisterParam", description = "Parameters required to create or update a user")
@FieldMatch.List({
        @FieldMatch(first = "password", second = "confirmPassword", message = "The password fields must match")
})
// Uncomment when `@IsUnique` implementation is ready
/*
@IsUnique.List({
    @IsUnique(property = "email", repository = "UserRepository", message = "This email already exists!")
})
*/
@Accessors(chain = true)
@Setter
@Getter
public class CreateUserDto {
    @Schema(hidden = true)
    private String id;

    @Schema(description = "User first name", required = true, example = "John")
    @NotBlank(message = "The first name is required")
    private String firstName;

    @Schema(description = "User last name", required = true, example = "Doe")
    @NotBlank(message = "The last name is required")
    private String lastName;

    @Schema(description = "User email address", required = true, example = "user@example.com")
    @Email(message = "Email address is not valid")
    @NotBlank(message = "The email address is required")
    private String email;

    @Schema(description = "User's password (must be at least 6 characters)", required = true, example = "securePassword123")
    @Size(min = 6, message = "Must be at least 6 characters")
    private String password;

    @Schema(description = "User timezone", required = true, example = "America/New_York")
    @NotBlank(message = "The timezone is required")
    private String timezone;

    @Schema(description = "Password confirmation", required = true, example = "securePassword123")
    @NotBlank(message = "This field is required")
    private String confirmPassword;

    @Schema(description = "User gender", example = "Male")
    private String gender;

    @Schema(description = "User avatar", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "Indicates if the user will be enabled or not", example = "true")
    private boolean enabled;

    @Schema(description = "Indicates if the user has confirmed their account", example = "false")
    private boolean confirmed;

    @Schema(description = "Geographic location of the user")
    private Coordinates coordinates;

    @Schema(description = "User role")
    private Role role;

    public CreateUserDto() {
        enabled = true;
    }
}
