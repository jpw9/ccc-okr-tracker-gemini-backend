package com.ccc.okrtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_users") // 'user' is a reserved keyword in Postgres
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User extends BaseEntity {
    private String firstName;
    private String lastName;
    private String groupNo;
    private String email;

    @Column(unique = true)
    private String login;

    private String avatar; // Initials usually

    private Long primaryProjectId; // Kept for backward compatibility

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // NOTE: assignedProjects is managed via JdbcTemplate (UserProjectRepository)
    // because the join table has extra columns (access_level, assigned_date, assigned_by)
    // that cannot be managed by JPA's @ManyToMany mapping
    // Use ProjectAccessService to manage user-project assignments

    // Transient field to hold project IDs for API responses
    @Transient
    private Set<Long> assignedProjectIds;

    // Helper method to get assigned project IDs (loaded via UserProjectRepository)
    public Set<Long> getAssignedProjectIds() {
        return assignedProjectIds;
    }

    // Setter for assigned project IDs
    public void setAssignedProjectIds(Set<Long> assignedProjectIds) {
        this.assignedProjectIds = assignedProjectIds;
    }
}