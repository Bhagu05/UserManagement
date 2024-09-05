package com.tericcabrel.authorization.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Schema(name = "ValidateTokenParam", description = "Parameters required to perform a token validation")
@Accessors(chain = true)
@Setter
@Getter
public class ValidateTokenDto {
    @Schema(description = "Token to validate", required = true, example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "The token is required")
    private String token;
}
