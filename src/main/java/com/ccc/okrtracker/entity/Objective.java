package com.ccc.okrtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Objective extends BaseEntity {
    private String title;

    @Column(length = 1000) // FIXED: Increase size to accommodate long descriptions from CSV
    private String description;

    private Integer progress = 0;

    private String assignee; // User Login
    private Integer year;
    private String quarter; // "Q1", "Q2"...
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    @JsonIgnore
    private Goal goal;

    @OneToMany(mappedBy = "objective", cascade = CascadeType.ALL)
    private List<KeyResult> keyResults = new ArrayList<>();
}