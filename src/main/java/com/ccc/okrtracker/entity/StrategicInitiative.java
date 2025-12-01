package com.ccc.okrtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StrategicInitiative extends BaseEntity {
    private String title;

    @Column(length = 1000) // FIXED: Increase size to accommodate long descriptions from CSV
    private String description;

    private Integer progress = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonIgnore // Prevent infinite recursion in JSON
    private Project project;

    @OneToMany(mappedBy = "initiative", cascade = CascadeType.ALL)
    private List<Goal> goals = new ArrayList<>();
}