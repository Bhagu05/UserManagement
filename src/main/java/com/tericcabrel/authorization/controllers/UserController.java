package com.tericcabrel.authorization.controllers;

import com.tericcabrel.authorization.exceptions.PasswordNotMatchException;
import com.tericcabrel.authorization.exceptions.ResourceNotFoundException;
import com.tericcabrel.authorization.models.dtos.UpdatePasswordDto;
import com.tericcabrel.authorization.models.dtos.UpdateUserDto;
import com.tericcabrel.authorization.models.dtos.UpdateUserPermissionDto;
import com.tericcabrel.authorization.models.entities.Permission;
import com.tericcabrel.authorization.models.entities.User;
import com.tericcabrel.authorization.models.response.*;
import com.tericcabrel.authorization.services.FileStorageServiceImpl;
import com.tericcabrel.authorization.services.interfaces.PermissionService;
import com.tericcabrel.authorization.services.interfaces.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static com.tericcabrel.authorization.utils.Constants.*;

@Tag(name = SWG_USER_TAG_NAME, description = SWG_USER_TAG_DESCRIPTION)
@RestController
@RequestMapping(value = "/users")
@Validated
public class UserController {
    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final PermissionService permissionService;
    private final FileStorageServiceImpl fileStorageServiceImpl;

    public UserController(UserService userService, PermissionService permissionService, FileStorageServiceImpl fileStorageServiceImpl) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.fileStorageServiceImpl = fileStorageServiceImpl;
    }

    @Operation(summary = SWG_USER_LIST_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_USER_LIST_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE),
    })
    @PreAuthorize("hasAuthority('read:users')")
    @GetMapping
    public ResponseEntity<UserListResponse> all() {
        return ResponseEntity.ok(new UserListResponse(userService.findAll()));
    }

    @Operation(summary = SWG_USER_LOGGED_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_USER_LOGGED_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser() throws ResourceNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(new UserResponse(userService.findByEmail(authentication.getName())));
    }

    @Operation(summary = SWG_USER_ITEM_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_USER_ITEM_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE),
    })
    @PreAuthorize("hasAuthority('read:user')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> one(@PathVariable String id) throws ResourceNotFoundException {
        return ResponseEntity.ok(new UserResponse(userService.findById(id)));
    }

    @Operation(summary = SWG_USER_UPDATE_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_USER_UPDATE_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE),
            @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE),
    })
    @PreAuthorize("hasAuthority('update:user')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable String id, @RequestBody UpdateUserDto updateUserDto)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(new UserResponse(userService.update(id, updateUserDto)));
    }

    @Operation(summary = SWG_USER_UPDATE_PWD_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_USER_UPDATE_PWD_MESSAGE),
            @ApiResponse(responseCode = "400", description = SWG_USER_UPDATE_PWD_ERROR),
    })
    @PreAuthorize("hasAuthority('change:password')")
    @PutMapping("/{id}/password")
    public ResponseEntity<UserResponse> updatePassword(@PathVariable String id, @Valid @RequestBody UpdatePasswordDto updatePasswordDto)
            throws PasswordNotMatchException, ResourceNotFoundException {
        User user = userService.updatePassword(id, updatePasswordDto);
        if (user == null) throw new PasswordNotMatchException(PASSWORD_NOT_MATCH_MESSAGE);
        return ResponseEntity.ok(new UserResponse(user));
    }

    @Operation(summary = SWG_USER_PICTURE_OPERATION)
    @PreAuthorize("hasAuthority('change:picture')")
    @PostMapping("/{id}/picture")
    public ResponseEntity<UserResponse> uploadPicture(
            @PathVariable String id,
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam("action")
            @Pattern(regexp = "[ud]", message = "The valid value can be \"u\" or \"d\"")
            @NotBlank String action
    ) throws IOException, ResourceNotFoundException {
        UpdateUserDto updateUserDto = new UpdateUserDto();
        User user;

        if ("u".equals(action)) {
            String fileName = fileStorageServiceImpl.storeFile(file);
            updateUserDto.setAvatar(fileName);
            user = userService.update(id, updateUserDto);
        } else if ("d".equals(action)) {
            user = userService.findById(id);
            if (user.getAvatar() != null && fileStorageServiceImpl.deleteFile(user.getAvatar())) {
                user.setAvatar(null);
                userService.update(user);
            }
        } else {
            throw new IllegalArgumentException(USER_PICTURE_NO_ACTION_MESSAGE);
        }

        return ResponseEntity.ok(new UserResponse(user));
    }

    @Operation(summary = SWG_USER_PERMISSION_ASSIGN_OPERATION)
    @PreAuthorize("hasAuthority('assign:permission')")
    @PutMapping("/{id}/permissions")
    public ResponseEntity<UserResponse> assignPermissions(@PathVariable String id, @Valid @RequestBody UpdateUserPermissionDto updateUserPermissionDto)
            throws ResourceNotFoundException {
        User user = userService.findById(id);

        Arrays.stream(updateUserPermissionDto.getPermissions()).forEach(permissionName -> {
            Optional<Permission> permission = permissionService.findByName(permissionName);
            permission.ifPresent(value -> {
                if (!user.hasPermission(permissionName)) user.addPermission(value);
            });
        });

        userService.update(user);
        return ResponseEntity.ok(new UserResponse(user));
    }

    @Operation(summary = SWG_USER_PERMISSION_REVOKE_OPERATION)
    @PreAuthorize("hasAuthority('revoke:permission')")
    @DeleteMapping("/{id}/permissions")
    public ResponseEntity<UserResponse> revokePermissions(@PathVariable String id, @Valid @RequestBody UpdateUserPermissionDto updateUserPermissionDto)
            throws ResourceNotFoundException {
        User user = userService.findById(id);

        Arrays.stream(updateUserPermissionDto.getPermissions()).forEach(permissionName -> {
            Optional<Permission> permission = permissionService.findByName(permissionName);
            permission.ifPresent(value -> {
                if (user.hasPermission(permissionName)) user.removePermission(value);
            });
        });

        userService.update(user);
        return ResponseEntity.ok(new UserResponse(user));
    }
}
