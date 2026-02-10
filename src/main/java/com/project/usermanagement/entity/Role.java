package com.project.usermanagement.entity;

import com.project.usermanagement.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing user roles (ADMIN, USER)
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;

    public Role(RoleName name) {
        this.name = name;
    }
}