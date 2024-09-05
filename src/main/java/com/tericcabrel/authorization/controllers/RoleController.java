package com.tericcabrel.authorization.controllers;

import com.tericcabrel.authorization.exceptions.ResourceNotFoundException;
import com.tericcabrel.authorization.models.dtos.CreateRoleDto;
import com.tericcabrel.authorization.models.dtos.UpdateRolePermissionDto;
import com.tericcabrel.authorization.models.entities.Permission;
import com.tericcabrel.authorization.models.entities.Role;
import com.tericcabrel.authorization.models.response.*;
import com.tericcabrel.authorization.services.interfaces.PermissionService;
import com.tericcabrel.authorization.services.interfaces.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

import static com.tericcabrel.authorization.utils.Constants.*;

@Tag(name = SWG_ROLE_TAG_NAME, description = SWG_ROLE_TAG_DESCRIPTION)
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping(value = "/roles")
public class RoleController {
    private final RoleService roleService;
    private final PermissionService permissionService;

    public RoleController(PermissionService permissionService, RoleService roleService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @Operation(summary = SWG_ROLE_CREATE_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_ROLE_CREATE_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE),
            @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE)
    })
    @PreAuthorize("hasAuthority('create:role')")
    @PostMapping
    public ResponseEntity<Role> create(@Valid @RequestBody CreateRoleDto createRoleDto) {
        Role role = roleService.save(createRoleDto);
        return ResponseEntity.ok(role);
    }

    @Operation(summary = SWG_ROLE_LIST_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_ROLE_LIST_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE)
    })
    @PreAuthorize("hasAuthority('read:roles')")
    @GetMapping
    public ResponseEntity<RoleListResponse> all() {
        return ResponseEntity.ok(new RoleListResponse(roleService.findAll()));
    }

    @Operation(summary = SWG_ROLE_ITEM_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_ROLE_ITEM_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE)
    })
    @PreAuthorize("hasAuthority('read:role')")
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> one(@PathVariable String id) throws ResourceNotFoundException {
        Role role = roleService.findById(id);
        return ResponseEntity.ok(new RoleResponse(role));
    }

    @Operation(summary = SWG_ROLE_UPDATE_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_ROLE_UPDATE_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE),
            @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE)
    })
    @PreAuthorize("hasAuthority('update:role')")
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateRoleDto createRoleDto
    ) throws ResourceNotFoundException {
        return ResponseEntity.ok(new RoleResponse(roleService.update(id, createRoleDto)));
    }

    @Operation(summary = SWG_ROLE_DELETE_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = SWG_ROLE_DELETE_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE)
    })
    @PreAuthorize("hasAuthority('delete:role')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = SWG_ROLE_ASSIGN_PERMISSION_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_ROLE_ASSIGN_PERMISSION_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE),
            @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE)
    })
    @PreAuthorize("hasAuthority('add:permission')")
    @PutMapping("/{id}/permissions")
    public ResponseEntity<RoleResponse> addPermissions(
            @PathVariable String id,
            @Valid @RequestBody UpdateRolePermissionDto updateRolePermissionDto
    ) throws ResourceNotFoundException {
        Role role = roleService.findById(id);
        Arrays.stream(updateRolePermissionDto.getPermissions()).forEach(permissionName -> {
            Optional<Permission> permission = permissionService.findByName(permissionName);
            permission.ifPresent(role::addPermission);
        });
        Role roleUpdated = roleService.update(role);
        return ResponseEntity.ok().body(new RoleResponse(roleUpdated));
    }

    @Operation(summary = SWG_ROLE_REMOVE_PERMISSION_OPERATION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SWG_ROLE_REMOVE_PERMISSION_MESSAGE),
            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_MESSAGE),
            @ApiResponse(responseCode = "403", description = FORBIDDEN_MESSAGE),
            @ApiResponse(responseCode = "422", description = INVALID_DATA_MESSAGE)
    })
    @PreAuthorize("hasAuthority('remove:permission')")
    @DeleteMapping("/{id}/permissions")
    public ResponseEntity<RoleResponse> removePermissions(
            @PathVariable String id,
            @Valid @RequestBody UpdateRolePermissionDto updateRolePermissionDto
    ) throws ResourceNotFoundException {
        Role role = roleService.findById(id);
        Arrays.stream(updateRolePermissionDto.getPermissions()).forEach(permissionName -> {
            Optional<Permission> permission = permissionService.findByName(permissionName);
            permission.ifPresent(role::removePermission);
        });
        Role roleUpdated = roleService.update(role);
        return ResponseEntity.ok().body(new RoleResponse(roleUpdated));
    }
}
