package com.project.usermanagement.repository;

import com.project.usermanagement.entity.Role;
import com.project.usermanagement.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role entity
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find role by name
     * 
     * @param name Role name
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(RoleName name);

    /**
     * Check if role exists by name
     * 
     * @param name Role name
     * @return true if exists, false otherwise
     */
    boolean existsByName(RoleName name);
}