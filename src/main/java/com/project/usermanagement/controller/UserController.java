package com.project.usermanagement.controller;

import com.project.usermanagement.dto.request.RegisterRequest;
import com.project.usermanagement.dto.response.ApiResponse;
import com.project.usermanagement.dto.response.UserResponse;
import com.project.usermanagement.enums.RoleName;
import com.project.usermanagement.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user management
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get user by ID (Admin only)
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        logger.debug("Get user request for ID: {}", id);

        UserResponse userResponse = userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", userResponse));
    }

    /**
     * Get all users (Admin only)
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        logger.debug("Get all users request (Admin)");

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserResponse> users = userService.getAllUsers(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * Get active users (Admin only)
     * GET /api/users/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getActiveUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("Get active users request (Admin)");

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> users = userService.getActiveUsers(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Active users retrieved successfully", users));
    }

    /**
     * Search users (Admin only)
     * GET /api/users/search
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("Search users with query: {}", query);

        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<UserResponse> users = userService.searchUsers(query, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Search results retrieved successfully", users));
    }

    /**
     * Update user (Admin only)
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody RegisterRequest request) {

        logger.info("Update user request for ID: {}", id);
        UserResponse userResponse = userService.updateUser(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", userResponse));
    }

    /**
     * Deactivate user (Admin only)
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        logger.info("Deactivate user request for ID: {}", id);

        userService.deleteUser(id);

        return ResponseEntity.ok(
                ApiResponse.success("User deactivated successfully"));
    }

    /**
     * Activate user (Admin only)
     * PATCH /api/users/{id}/activate
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long id) {
        logger.info("Activate user request for ID: {}", id);

        userService.activateUser(id);

        return ResponseEntity.ok(
                ApiResponse.success("User activated successfully"));
    }

    /**
     * Assign role to user (Admin only)
     * POST /api/users/{id}/roles/{roleName}
     */
    @PostMapping("/{id}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable Long id,
            @PathVariable RoleName roleName) {

        logger.info("Assign role {} to user ID: {}", roleName, id);
        UserResponse userResponse = userService.assignRole(id, roleName);

        return ResponseEntity.ok(
                ApiResponse.success("Role assigned successfully", userResponse));
    }

    /**
     * Remove role from user (Admin only)
     * DELETE /api/users/{id}/roles/{roleName}
     */
    @DeleteMapping("/{id}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> removeRole(
            @PathVariable Long id,
            @PathVariable RoleName roleName) {

        logger.info("Remove role {} from user ID: {}", roleName, id);
        UserResponse userResponse = userService.removeRole(id, roleName);

        return ResponseEntity.ok(
                ApiResponse.success("Role removed successfully", userResponse));
    }
}