package com.tericcabrel.authorization.controllers;

import com.tericcabrel.authorization.exceptions.ResourceNotFoundException;
import com.tericcabrel.authorization.models.dtos.RefreshTokenDto;
import com.tericcabrel.authorization.models.dtos.ValidateTokenDto;
import com.tericcabrel.authorization.models.entities.RefreshToken;
import com.tericcabrel.authorization.models.entities.User;
import com.tericcabrel.authorization.models.response.AuthTokenResponse;
import com.tericcabrel.authorization.models.response.BadRequestResponse;
import com.tericcabrel.authorization.models.response.InvalidDataResponse;
import com.tericcabrel.authorization.repositories.RefreshTokenRepository;
import com.tericcabrel.authorization.services.interfaces.UserService;
import com.tericcabrel.authorization.utils.JwtTokenUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.tericcabrel.authorization.utils.Constants.*;

@Tag(name = SWG_TOKEN_TAG_NAME, description = SWG_TOKEN_TAG_DESCRIPTION)
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/token")
public class TokenController {

  private final Log logger = LogFactory.getLog(this.getClass());
  private final JwtTokenUtil jwtTokenUtil;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserService userService;

  public TokenController(JwtTokenUtil jwtTokenUtil, RefreshTokenRepository refreshTokenRepository, UserService userService) {
    this.jwtTokenUtil = jwtTokenUtil;
    this.refreshTokenRepository = refreshTokenRepository;
    this.userService = userService;
  }

  @Operation(summary = "Validate a token")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = SWG_TOKEN_VALIDATE_MESSAGE),
          @ApiResponse(responseCode = "400", description = SWG_TOKEN_VALIDATE_ERROR),
          @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE),
  })
  @PostMapping(value = "/validate")
  public ResponseEntity<Map<String, String>> validate(@Valid @RequestBody ValidateTokenDto validateTokenDto) {
    String username = null;
    Map<String, String> result = new HashMap<>();

    try {
      username = jwtTokenUtil.getUsernameFromToken(validateTokenDto.getToken());
    } catch (IllegalArgumentException e) {
      logger.error(JWT_ILLEGAL_ARGUMENT_MESSAGE, e);
      result.put(MESSAGE_KEY, JWT_ILLEGAL_ARGUMENT_MESSAGE);
    } catch (ExpiredJwtException e) {
      logger.warn(JWT_EXPIRED_MESSAGE, e);
      result.put(MESSAGE_KEY, JWT_EXPIRED_MESSAGE);
    } catch (SignatureException e) {
      logger.error(JWT_SIGNATURE_MESSAGE);
      result.put(MESSAGE_KEY, JWT_SIGNATURE_MESSAGE);
    }

    if (username != null) {
      result.put(MESSAGE_KEY, VALIDATE_TOKEN_SUCCESS_MESSAGE);
      return ResponseEntity.ok(result);
    }

    return ResponseEntity.badRequest().body(result);
  }

  @Operation(summary = "Refresh a token")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = SWG_TOKEN_REFRESH_MESSAGE),
          @ApiResponse(responseCode = "400", description = SWG_TOKEN_REFRESH_ERROR),
          @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE),
  })
  @PostMapping(value = "/refresh")
  public ResponseEntity<Object> refresh(@Valid @RequestBody RefreshTokenDto refreshTokenDto)
          throws ResourceNotFoundException {
    RefreshToken refreshToken = refreshTokenRepository.findByValue(refreshTokenDto.getToken());
    Map<String, String> result = new HashMap<>();

    if (refreshToken == null) {
      result.put(MESSAGE_KEY, INVALID_TOKEN_MESSAGE);
      return ResponseEntity.badRequest().body(result);
    }

    User user = userService.findById(refreshToken.getUserId());
    if (user == null) {
      result.put(MESSAGE_KEY, TOKEN_NOT_FOUND_MESSAGE);
      return ResponseEntity.badRequest().body(result);
    }

    String token = jwtTokenUtil.createTokenFromUser(user);
    Date expirationDate = jwtTokenUtil.getExpirationDateFromToken(token);

    return ResponseEntity.ok(new AuthTokenResponse(token, refreshToken.getValue(), expirationDate.getTime()));
  }
}
