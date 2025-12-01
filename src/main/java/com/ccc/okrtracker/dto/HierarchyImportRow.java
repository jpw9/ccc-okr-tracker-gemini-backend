package com.ccc.okrtracker.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Data Transfer Object representing a single row in the OKR import CSV.
 * This structure allows us to handle the entire hierarchy level by level.
 */
@Data
public class HierarchyImportRow {

    // Project (Top Level - must be specified for initial import)
    private String projectTitle;
    private String projectDescription;

    // Strategic Initiative (Level 2)
    private String initiativeTitle;
    private String initiativeDescription;

    // Goal (Level 3)
    private String goalTitle;
    private String goalDescription;

    // Objective (Level 4)
    private String objectiveTitle;
    private String objectiveDescription;
    private String objectiveAssignee;
    private Integer objectiveYear;
    private String objectiveQuarter; // "Q1", "Q2"
    private LocalDate objectiveDueDate;

    // Key Result (Level 5)
    private String krTitle;
    private String krDescription;
    private String krAssignee;
    private Double krMetricStart;
    private Double krMetricTarget;
    private Double krMetricCurrent;
    private String krUnit;

    // Action Item (Level 6)
    private String actionItemTitle;
    private String actionItemDescription;
    private String actionItemAssignee;
    private LocalDate actionItemDueDate;
    private Boolean actionItemIsCompleted;
}