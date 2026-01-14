package com.ccc.okrtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter @Setter @NoArgsConstructor
public class Project extends BaseEntity {
    private String title;

    @Column(length = 1000) // Confirmed long length for description
    private String description;

    private Integer progress = 0;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<StrategicInitiative> initiatives = new ArrayList<>();

    // NOTE: assignedUsers and scopedRoles are managed via JdbcTemplate
    // (UserProjectRepository and RoleProjectRepository)
    // No JPA mappings because join tables have extra columns or need manual control
    // Use ProjectAccessService to query users/roles for a project

    // Constructor for backward compatibility with ImportService
    public Project(String title, String description, Integer progress, List<StrategicInitiative> initiatives) {
        this.title = title;
        this.description = description;
        this.progress = progress != null ? progress : 0;
        this.initiatives = initiatives != null ? initiatives : new ArrayList<>();
    }
}