package com.project.usermanagement.controller;

import com.project.usermanagement.dto.request.AdminRegistrationRequest;
import com.project.usermanagement.dto.request.LoginRequest;
import com.project.usermanagement.dto.request.RegisterRequest;
import com.project.usermanagement.dto.response.ApiResponse;
import com.project.usermanagement.dto.response.AuthResponse;
import com.project.usermanagement.dto.response.UserResponse;
import com.project.usermanagement.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * User login endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login request received for username: {}", loginRequest.getUsername());

        AuthResponse authResponse = authService.login(loginRequest);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authResponse));
    }

    /**
     * User registration endpoint
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("Registration request received for username: {}", registerRequest.getUsername());

        UserResponse userResponse = authService.register(registerRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("User registered successfully", userResponse));
    }

    /**
     * Admin registration endpoint
     * POST /api/auth/register-admin
     * 
     * Security:
     * - First admin: Can be created without authentication (no admins exist)
     * - Subsequent admins: Requires either:
     * 1. Valid admin secret key in request body, OR
     * 2. Must be called by an existing admin (authenticated)
     */
    @PostMapping("/register-admin")
    public ResponseEntity<ApiResponse<UserResponse>> registerAdmin(
            @Valid @RequestBody AdminRegistrationRequest request) {

        logger.info("Admin registration request received for username: {}", request.getUsername());

        UserResponse userResponse = authService.registerAdmin(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Admin user registered successfully", userResponse));
    }

    /**
     * Create admin by existing admin (alternative endpoint)
     * POST /api/auth/create-admin
     * Only accessible by existing admins
     */
    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createAdminByAdmin(
            @Valid @RequestBody AdminRegistrationRequest request) {

        logger.info("Admin creation request by admin for username: {}", request.getUsername());

        // No need for secret key when called by admin
        UserResponse userResponse = authService.registerAdmin(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Admin user created successfully", userResponse));
    }

    /**
     * Get current authenticated user
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        logger.debug("Fetching current authenticated user");

        UserResponse userResponse = authService.getCurrentUser();

        return ResponseEntity.ok(
                ApiResponse.success("Current user retrieved successfully", userResponse));
    }
}