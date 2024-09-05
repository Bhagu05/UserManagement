package com.tericcabrel.authorization.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

import com.tericcabrel.authorization.models.entities.Coordinates;
import com.tericcabrel.authorization.models.entities.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Schema(name = "UpdateUserParam", description = "Parameters required to update a user")
@Accessors(chain = true)
@Setter
@Getter
public class UpdateUserDto {
    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @Schema(description = "User timezone", example = "America/New_York")
    private String timezone;

    @Schema(description = "User gender", example = "Male")
    private String gender;

    @Schema(description = "User avatar URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "Indicates if the user will be enabled or not", example = "true")
    private boolean enabled;

    @Schema(description = "Indicates if the user has confirmed their account", example = "false")
    private boolean confirmed;

    @Schema(description = "Geographic location of the user")
    private Coordinates coordinates;

    @Schema(description = "Set of roles assigned to the user")
    private Set<Role> roles;
}
