package com.tericcabrel.authorization.controllers;

import com.tericcabrel.authorization.exceptions.ResourceNotFoundException;
import com.tericcabrel.authorization.models.dtos.CreateUserDto;
import com.tericcabrel.authorization.models.entities.Role;
import com.tericcabrel.authorization.models.entities.User;
import com.tericcabrel.authorization.models.response.BadRequestResponse;
import com.tericcabrel.authorization.models.response.InvalidDataResponse;
import com.tericcabrel.authorization.models.response.SuccessResponse;
import com.tericcabrel.authorization.models.response.UserResponse;
import com.tericcabrel.authorization.services.interfaces.RoleService;
import com.tericcabrel.authorization.services.interfaces.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.tericcabrel.authorization.utils.Constants.ROLE_ADMIN;

@Tag(name = "Admin Management", description = "Endpoints for managing admin users")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/admins")
public class AdminController {
  private final RoleService roleService;
  private final UserService userService;

  public AdminController(RoleService roleService, UserService userService) {
    this.roleService = roleService;
    this.userService = userService;
  }

  @Operation(
          summary = "Create a new admin",
          responses = {
                  @ApiResponse(responseCode = "200", description = "Admin user created successfully",
                          content = {@io.swagger.v3.oas.annotations.media.Content(
                                  mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponse.class))}),
                  @ApiResponse(responseCode = "400", description = "Invalid request data",
                          content = {@io.swagger.v3.oas.annotations.media.Content(
                                  mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = BadRequestResponse.class))}),
                  @ApiResponse(responseCode = "422", description = "Validation error",
                          content = {@io.swagger.v3.oas.annotations.media.Content(
                                  mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = InvalidDataResponse.class))})
          }
  )
  @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
  @PostMapping
  public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserDto createUserDto) throws ResourceNotFoundException {
    Role roleAdmin = roleService.findByName(ROLE_ADMIN);

    if (roleAdmin == null) {
      throw new ResourceNotFoundException("Role not found: " + ROLE_ADMIN);
    }

    createUserDto.setRole(roleAdmin)
            .setConfirmed(true)
            .setEnabled(true);

    User user = userService.save(createUserDto);

    return ResponseEntity.ok(new UserResponse(user));
  }

  @Operation(
          summary = "Delete an admin",
          responses = {
                  @ApiResponse(responseCode = "204", description = "Admin user deleted successfully"),
                  @ApiResponse(responseCode = "401", description = "Unauthorized access",
                          content = {@io.swagger.v3.oas.annotations.media.Content(
                                  mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = BadRequestResponse.class))}),
                  @ApiResponse(responseCode = "403", description = "Access forbidden",
                          content = {@io.swagger.v3.oas.annotations.media.Content(
                                  mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = BadRequestResponse.class))})
          }
  )
  @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
