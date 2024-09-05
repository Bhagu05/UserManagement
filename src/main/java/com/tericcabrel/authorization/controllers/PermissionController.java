package com.tericcabrel.authorization.controllers;

import com.tericcabrel.authorization.exceptions.ResourceNotFoundException;
import com.tericcabrel.authorization.models.entities.Permission;
import com.tericcabrel.authorization.models.response.*;
import com.tericcabrel.authorization.services.interfaces.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.tericcabrel.authorization.utils.Constants.*;

@Tag(name = SWG_PERMISSION_TAG_NAME, description = SWG_PERMISSION_TAG_DESCRIPTION)
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping(value = "/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = SWG_PERMISSION_LIST_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_PERMISSION_LIST_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE)
    })
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<PermissionListResponse> getAllPermissions() {
        return ResponseEntity.ok(new PermissionListResponse(permissionService.findAll()));
    }

    @Operation(summary = SWG_PERMISSION_ITEM_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_PERMISSION_ITEM_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE),
            @ApiResponse(responseCode = "404", description = PERMISSION_NOT_FOUND_MESSAGE)
    })
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable String id) throws ResourceNotFoundException {
        Optional<Permission> permission = permissionService.findById(id);

        if (permission.isEmpty()) {
            throw new ResourceNotFoundException(PERMISSION_NOT_FOUND_MESSAGE);
        }

        return ResponseEntity.ok(new PermissionResponse(permission.get()));
    }
}
