package com.tericcabrel.authorization.controllers;

import com.tericcabrel.authorization.exceptions.ResourceNotFoundException;
import com.tericcabrel.authorization.models.dtos.ForgotPasswordDto;
import com.tericcabrel.authorization.models.dtos.ResetPasswordDto;
import com.tericcabrel.authorization.models.entities.User;
import com.tericcabrel.authorization.models.entities.UserAccount;
import com.tericcabrel.authorization.models.response.BadRequestResponse;
import com.tericcabrel.authorization.models.response.InvalidDataResponse;
import com.tericcabrel.authorization.models.response.SuccessResponse;
import com.tericcabrel.authorization.services.interfaces.UserAccountService;
import com.tericcabrel.authorization.services.interfaces.UserService;
import com.tericcabrel.authorization.events.OnResetPasswordEvent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.tericcabrel.authorization.utils.Constants.*;

@Tag(name = SWG_RESPWD_TAG_NAME, description = SWG_RESPWD_TAG_DESCRIPTION)
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class ResetPasswordController {

    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserAccountService userAccountService;

    public ResetPasswordController(
            UserService userService,
            ApplicationEventPublisher eventPublisher,
            UserAccountService userAccountService
    ) {
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.userAccountService = userAccountService;
    }

    @Operation(summary = SWG_RESPWD_FORGOT_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_RESPWD_FORGOT_MESSAGE),
            @ApiResponse(responseCode = "400", description = SWG_RESPWD_FORGOT_ERROR),
            @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE)
    })
    @PostMapping(value = "/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordDto forgotPasswordDto) {
        Map<String, String> result = new HashMap<>();

        try {
            User user = userService.findByEmail(forgotPasswordDto.getEmail());
            if (user == null) {
                result.put(MESSAGE_KEY, NO_USER_FOUND_WITH_EMAIL_MESSAGE);
                return ResponseEntity.badRequest().body(result);
            }

            eventPublisher.publishEvent(new OnResetPasswordEvent(user));
            result.put(MESSAGE_KEY, PASSWORD_LINK_SENT_MESSAGE);
            return ResponseEntity.ok(result);

        } catch (ResourceNotFoundException e) {
            result.put(MESSAGE_KEY, NO_USER_FOUND_WITH_EMAIL_MESSAGE);
            return ResponseEntity.badRequest().body(result);
        }
    }

    @Operation(summary = SWG_RESPWD_RESET_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_RESPWD_RESET_MESSAGE),
            @ApiResponse(responseCode = "400", description = SWG_RESPWD_RESET_ERROR),
            @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE)
    })
    @PostMapping(value = "/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordDto passwordResetDto) {
        Map<String, String> result = new HashMap<>();

        try {
            UserAccount userAccount = userAccountService.findByToken(passwordResetDto.getToken());
            if (userAccount.isExpired()) {
                result.put(MESSAGE_KEY, TOKEN_EXPIRED_MESSAGE);
                userAccountService.delete(userAccount.getId());
                return ResponseEntity.badRequest().body(result);
            }

            userService.updatePassword(userAccount.getUser().getId(), passwordResetDto.getPassword());
            result.put(MESSAGE_KEY, RESET_PASSWORD_SUCCESS_MESSAGE);

            userAccountService.delete(userAccount.getId());
            return ResponseEntity.ok(result);

        } catch (ResourceNotFoundException e) {
            result.put(MESSAGE_KEY, TOKEN_NOT_FOUND_MESSAGE);
            return ResponseEntity.badRequest().body(result);
        }
    }
}
