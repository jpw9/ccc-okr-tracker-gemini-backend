package com.ccc.okrtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ActionItem extends BaseEntity {
    private String title;
    private String description;
    private Integer progress = 0; // 0 or 100 usually

    private LocalDate dueDate;
    private String assignee;
    private Boolean isCompleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "key_result_id")
    @JsonIgnore
    private KeyResult keyResult;
}