package com.tericcabrel.authorization.controllers;

import com.tericcabrel.authorization.exceptions.ResourceNotFoundException;
import com.tericcabrel.authorization.models.entities.Role;
import com.tericcabrel.authorization.models.entities.User;
import com.tericcabrel.authorization.models.entities.UserAccount;
import com.tericcabrel.authorization.models.entities.RefreshToken;
import com.tericcabrel.authorization.models.dtos.LoginUserDto;
import com.tericcabrel.authorization.models.dtos.CreateUserDto;
import com.tericcabrel.authorization.models.dtos.ValidateTokenDto;
import com.tericcabrel.authorization.models.response.AuthTokenResponse;
import com.tericcabrel.authorization.models.response.BadRequestResponse;
import com.tericcabrel.authorization.models.response.InvalidDataResponse;
import com.tericcabrel.authorization.services.interfaces.RoleService;
import com.tericcabrel.authorization.services.interfaces.UserService;
import com.tericcabrel.authorization.services.interfaces.UserAccountService;
import com.tericcabrel.authorization.utils.Helpers;
import com.tericcabrel.authorization.utils.JwtTokenUtil;
import com.tericcabrel.authorization.repositories.RefreshTokenRepository;
import com.tericcabrel.authorization.events.OnRegistrationCompleteEvent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.tericcabrel.authorization.utils.Constants.*;

@Tag(name = SWG_AUTH_TAG_NAME, description = SWG_AUTH_TAG_DESCRIPTION)
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserService userService;
    private final RoleService roleService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserAccountService userAccountService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenUtil jwtTokenUtil,
            UserService userService,
            RoleService roleService,
            RefreshTokenRepository refreshTokenRepository,
            ApplicationEventPublisher eventPublisher,
            UserAccountService userAccountService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
        this.roleService = roleService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.eventPublisher = eventPublisher;
        this.userAccountService = userAccountService;
    }

    @Operation(summary = SWG_AUTH_REGISTER_OPERATION, description = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_AUTH_REGISTER_MESSAGE),
            @ApiResponse(responseCode = "400", description = SWG_AUTH_REGISTER_ERROR, content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE, content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping(value = "/register")
    public ResponseEntity<Object> register(@Valid @RequestBody CreateUserDto createUserDto) {
        try {
            Role roleUser = roleService.findByName(ROLE_USER);

            createUserDto.setRole(roleUser);

            User user = userService.save(createUserDto);

            eventPublisher.publishEvent(new OnRegistrationCompleteEvent(user));

            return ResponseEntity.ok(user);
        } catch (ResourceNotFoundException e) {
            logger.error("Register User: " + ROLE_NOT_FOUND_MESSAGE);

            Map<String, String> result = new HashMap<>();
            result.put("message", SWG_AUTH_REGISTER_ERROR);

            return ResponseEntity.badRequest().body(result);
        }
    }

    @Operation(summary = SWG_AUTH_LOGIN_OPERATION, description = "Authenticate and login a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_AUTH_LOGIN_MESSAGE),
            @ApiResponse(responseCode = "400", description = SWG_AUTH_LOGIN_ERROR, content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE, content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping(value = "/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginUserDto loginUserDto) throws ResourceNotFoundException {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginUserDto.getEmail(),
                        loginUserDto.getPassword()
                )
        );

        User user = userService.findByEmail(loginUserDto.getEmail());
        Map<String, String> result = new HashMap<>();

        if (!user.isEnabled()) {
            result.put(DATA_KEY, ACCOUNT_DEACTIVATED_MESSAGE);
            return ResponseEntity.badRequest().body(result);
        }

        if (!user.isConfirmed()) {
            result.put(DATA_KEY, ACCOUNT_NOT_CONFIRMED_MESSAGE);
            return ResponseEntity.badRequest().body(result);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        final String token = jwtTokenUtil.createTokenFromAuth(authentication);
        Date expirationDate = jwtTokenUtil.getExpirationDateFromToken(token);
        String refreshToken = Helpers.generateRandomString(25);

        refreshTokenRepository.save(new RefreshToken(user.getId(), refreshToken));

        return ResponseEntity.ok(new AuthTokenResponse(token, refreshToken, expirationDate.getTime()));
    }

    @Operation(summary = SWG_AUTH_CONFIRM_ACCOUNT_OPERATION, description = "Confirm a user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_AUTH_CONFIRM_ACCOUNT_MESSAGE),
            @ApiResponse(responseCode = "400", description = SWG_AUTH_CONFIRM_ACCOUNT_ERROR, content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping(value = "/confirm-account")
    public ResponseEntity<Object> confirmAccount(@Valid @RequestBody ValidateTokenDto validateTokenDto) throws ResourceNotFoundException {
        UserAccount userAccount = userAccountService.findByToken(validateTokenDto.getToken());
        Map<String, String> result = new HashMap<>();

        if (userAccount.isExpired()) {
            result.put(MESSAGE_KEY, TOKEN_EXPIRED_MESSAGE);
            userAccountService.delete(userAccount.getId());
            return ResponseEntity.badRequest().body(result);
        }

        userService.confirm(userAccount.getUser().getId());

        result.put(MESSAGE_KEY, ACCOUNT_CONFIRMED_MESSAGE);

        return ResponseEntity.ok(result);
    }
}
