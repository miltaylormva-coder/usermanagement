package com.project.usermanagement.service;

import com.project.usermanagement.dto.request.RegisterRequest;
import com.project.usermanagement.dto.response.UserResponse;
import com.project.usermanagement.entity.Role;
import com.project.usermanagement.entity.User;
import com.project.usermanagement.enums.RoleName;
import com.project.usermanagement.exception.BadRequestException;
import com.project.usermanagement.exception.ResourceNotFoundException;
import com.project.usermanagement.repository.RoleRepository;
import com.project.usermanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Service class for User management operations
 */
@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new user (Registration)
     */
    public UserResponse createUser(RegisterRequest request) {
        logger.info("Creating new user with username: {}", request.getUsername());

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

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .isActive(true)
                .build();

        // Assign default USER role
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.ROLE_USER));
        user.addRole(userRole);

        // Save user
        User savedUser = userRepository.save(user);
        logger.info("User created successfully with ID: {}", savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        logger.debug("Fetching user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToUserResponse(user);
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        logger.debug("Fetching user with username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return mapToUserResponse(user);
    }

    /**
     * Get all users (paginated)
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        logger.debug("Fetching all users with pagination");
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    /**
     * Get all active users (paginated)
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getActiveUsers(Pageable pageable) {
        logger.debug("Fetching active users with pagination");
        return userRepository.findByIsActiveTrue(pageable)
                .map(this::mapToUserResponse);
    }

    /**
     * Search users by keyword
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String searchTerm, Pageable pageable) {
        logger.debug("Searching users with term: {}", searchTerm);
        return userRepository.searchUsers(searchTerm, pageable)
                .map(this::mapToUserResponse);
    }

    /**
     * Update user
     */
    public UserResponse updateUser(Long id, RegisterRequest request) {
        logger.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Check if new username is taken by another user
        if (!user.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists: " + request.getUsername());
        }

        // Check if new email is taken by another user
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        // Update fields
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());

        // Update password only if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully with ID: {}", updatedUser.getId());

        return mapToUserResponse(updatedUser);
    }

    /**
     * Delete user (soft delete - deactivate)
     */
    public void deleteUser(Long id) {
        logger.info("Deactivating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setIsActive(false);
        userRepository.save(user);

        logger.info("User deactivated successfully with ID: {}", id);
    }

    /**
     * Permanently delete user
     */
    public void permanentlyDeleteUser(Long id) {
        logger.warn("Permanently deleting user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }

        userRepository.deleteById(id);
        logger.warn("User permanently deleted with ID: {}", id);
    }

    /**
     * Activate user
     */
    public void activateUser(Long id) {
        logger.info("Activating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setIsActive(true);
        userRepository.save(user);

        logger.info("User activated successfully with ID: {}", id);
    }

    /**
     * Assign role to user
     */
    public UserResponse assignRole(Long userId, RoleName roleName) {
        logger.info("Assigning role {} to user ID: {}", roleName, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        user.addRole(role);
        User updatedUser = userRepository.save(user);

        logger.info("Role assigned successfully to user ID: {}", userId);
        return mapToUserResponse(updatedUser);
    }

    /**
     * Remove role from user
     */
    public UserResponse removeRole(Long userId, RoleName roleName) {
        logger.info("Removing role {} from user ID: {}", roleName, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        user.removeRole(role);
        User updatedUser = userRepository.save(user);

        logger.info("Role removed successfully from user ID: {}", userId);
        return mapToUserResponse(updatedUser);
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