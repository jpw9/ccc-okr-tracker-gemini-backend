package com.ccc.okrtracker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Project extends BaseEntity {
    private String title;

    @Column(length = 1000) // Confirmed long length for description
    private String description;

    private Integer progress = 0;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<StrategicInitiative> initiatives = new ArrayList<>();
}