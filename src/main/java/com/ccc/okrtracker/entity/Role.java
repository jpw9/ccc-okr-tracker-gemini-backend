package com.ccc.okrtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Role extends BaseEntity {
    private String name;
    private String description;
    private Boolean isSystem = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();

    // Inverse side of User.roles relationship
    @ManyToMany(mappedBy = "roles")
    @JsonIgnore // Prevent circular serialization
    private Set<User> users = new HashSet<>();

    // NOTE: scopedProjects is managed via JdbcTemplate (RoleProjectRepository)
    // because the join table cannot be managed by JPA (no @ManyToMany mapping)
    // Use ProjectAccessService to manage role-project relationships

    // Transient field to hold scoped project IDs for API responses
    @Transient
    private Set<Long> scopedProjectIds;

    // Helper method to check if this is a global role (not scoped to specific projects)
    public boolean isGlobalRole() {
        return scopedProjectIds == null || scopedProjectIds.isEmpty();
    }

    // Getter for scoped project IDs (loaded via RoleProjectRepository)
    public Set<Long> getScopedProjectIds() {
        return scopedProjectIds;
    }

    // Setter for scoped project IDs
    public void setScopedProjectIds(Set<Long> scopedProjectIds) {
        this.scopedProjectIds = scopedProjectIds;
    }
}