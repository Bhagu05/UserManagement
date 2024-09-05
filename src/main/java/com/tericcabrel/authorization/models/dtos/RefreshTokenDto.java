package com.tericcabrel.authorization.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Schema(name = "RefreshTokenParam", description = "Parameters required to create or update a user")
@Accessors(chain = true)
@Setter
@Getter
public class RefreshTokenDto {
    @Schema(description = "Refresh token used to validate the user and generate a new token", required = true, example = "dXNlcjEyMy5yZWZyZXNoLnRva2Vu")
    @NotBlank(message = "The token is required")
    private String token;
}
