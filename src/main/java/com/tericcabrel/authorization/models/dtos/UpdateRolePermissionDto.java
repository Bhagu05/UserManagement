package com.tericcabrel.authorization.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Schema(name = "UpdateRolePermissionParam", description = "Parameters required to update role permissions")
@Accessors(chain = true)
@Setter
@Getter
public class UpdateRolePermissionDto {
    @Schema(description = "Array of permissions to assign or remove from a role", required = true, example = "[\"READ\", \"WRITE\"]")
    @NotEmpty(message = "The field must have at least one item")
    private String[] permissions;
}
