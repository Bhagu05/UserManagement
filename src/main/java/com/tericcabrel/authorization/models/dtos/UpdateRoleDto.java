package com.tericcabrel.authorization.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Schema(name = "UpdateRoleParam", description = "Parameters required to update the roles of a user")
@Accessors(chain = true)
@Setter
@Getter
public class UpdateRoleDto {
    @Schema(description = "User identifier", required = true, example = "123456")
    @NotBlank(message = "The userId is required")
    private String userId;

    @Schema(description = "Array of roles to assign to a user", required = true, example = "[\"ADMIN\", \"USER\"]")
    @NotEmpty(message = "The field must have at least one item")
    private String[] roles;
}
