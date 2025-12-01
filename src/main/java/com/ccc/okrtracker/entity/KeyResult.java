package com.ccc.okrtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class KeyResult extends BaseEntity {
    private String title;
    private String description;
    private Integer progress = 0;

    private String assignee;
    private Double metricStart;
    private Double metricTarget;
    private Double metricCurrent;
    private String unit; // "%", "$", etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objective_id")
    @JsonIgnore
    private Objective objective;

    @OneToMany(mappedBy = "keyResult", cascade = CascadeType.ALL)
    private List<ActionItem> actionItems = new ArrayList<>();
}