package com.project.usermanagement.service;

import com.project.usermanagement.dto.request.AdminRegistrationRequest;
import com.project.usermanagement.dto.request.LoginRequest;
import com.project.usermanagement.dto.request.RegisterRequest;
import com.project.usermanagement.dto.response.AuthResponse;
import com.project.usermanagement.dto.response.UserResponse;
import com.project.usermanagement.entity.Role;
import com.project.usermanagement.entity.User;
import com.project.usermanagement.enums.RoleName;
import com.project.usermanagement.exception.BadRequestException;
import com.project.usermanagement.repository.RoleRepository;
import com.project.usermanagement.repository.UserRepository;
import com.project.usermanagement.security.JwtTokenProvider;
import com.project.usermanagement.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Service for authentication operations
 */
@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.secret.key:CHANGE_THIS_SECRET_KEY_IN_PRODUCTION}")
    private String adminSecretKey;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            UserService userService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate user and generate JWT token
     */
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("Attempting login for user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        logger.info("User logged in successfully: {}", loginRequest.getUsername());

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .userId(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .roles(userPrincipal.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Register new user
     */
    public UserResponse register(RegisterRequest registerRequest) {
        logger.info("Registering new user: {}", registerRequest.getUsername());
        return userService.createUser(registerRequest);
    }

    /**
     * Register new admin user
     * This method has TWO modes:
     * 1. First admin creation (no admins exist) - no secret key required
     * 2. Subsequent admin creation - requires matching secret key OR must be called
     * by existing admin
     */
    public UserResponse registerAdmin(AdminRegistrationRequest request) {
        logger.info("Admin registration request for username: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.error("Username already exists: {}", request.getUsername());
            throw new BadRequestException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.error("Email already exists: {}", request.getEmail());
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        // Check if this is the first admin (no admins exist)
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new BadRequestException("Admin role not found in database"));

        long adminCount = userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(adminRole))
                .count();

        // If admins exist, validate authorization
        if (adminCount > 0) {
            // Check if request has valid secret key OR if current user is admin
            boolean hasValidSecretKey = request.getAdminSecretKey() != null &&
                    request.getAdminSecretKey().equals(adminSecretKey);
            boolean isCurrentUserAdmin = isCurrentUserAdmin();

            if (!hasValidSecretKey && !isCurrentUserAdmin) {
                logger.error("Unauthorized admin registration attempt");
                throw new BadRequestException("Unauthorized: Invalid admin secret key or insufficient permissions");
            }
        } else {
            logger.info("Creating first admin user - no authorization required");
        }

        // Create admin user
        User admin = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .isActive(true)
                .build();

        // Assign both USER and ADMIN roles
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new BadRequestException("User role not found in database"));

        admin.addRole(userRole);
        admin.addRole(adminRole);

        // Save admin user
        User savedAdmin = userRepository.save(admin);
        logger.info("Admin user created successfully with ID: {}", savedAdmin.getId());

        return mapToUserResponse(savedAdmin);
    }

    /**
     * Get current authenticated user
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        logger.debug("Fetching current user: {}", userPrincipal.getUsername());
        return userService.getUserById(userPrincipal.getId());
    }

    /**
     * Check if current user is admin
     */
    private boolean isCurrentUserAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }
            return authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Map User entity to UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}