package com.project.usermanagement.repository;

import com.project.usermanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     * 
     * @param username Username
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     * 
     * @param email Email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email
     * 
     * @param username Username
     * @param email    Email
     * @return Optional containing the user if found
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Check if username exists
     * 
     * @param username Username
     * @return true if exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     * 
     * @param email Email
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users
     * 
     * @param pageable Pagination information
     * @return Page of active users
     */
    Page<User> findByIsActiveTrue(Pageable pageable);

    /**
     * Search users by username, email, first name, or last name
     * 
     * @param searchTerm Search term
     * @param pageable   Pagination information
     * @return Page of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
}