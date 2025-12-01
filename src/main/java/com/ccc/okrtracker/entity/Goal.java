package com.ccc.okrtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Goal extends BaseEntity {
    private String title;

    @Column(length = 1000) // FIXED: Increase size to accommodate long descriptions from CSV
    private String description;

    private Integer progress = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiative_id")
    @JsonIgnore
    private StrategicInitiative initiative;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL)
    private List<Objective> objectives = new ArrayList<>();
}