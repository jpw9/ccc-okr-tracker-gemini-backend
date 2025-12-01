package com.ccc.okrtracker.controller;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.service.HierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hierarchy")
@RequiredArgsConstructor
public class HierarchyController {

    private final HierarchyService hierarchyService;

    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(hierarchyService.getAllProjects());
    }

    @PostMapping("/projects")
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        return ResponseEntity.ok(hierarchyService.createProject(project));
    }

    @PostMapping("/projects/{projectId}/initiatives")
    public ResponseEntity<StrategicInitiative> addInitiative(@PathVariable Long projectId, @RequestBody StrategicInitiative init) {
        return ResponseEntity.ok(hierarchyService.addInitiative(projectId, init));
    }

    @PostMapping("/initiatives/{initId}/goals")
    public ResponseEntity<Goal> addGoal(@PathVariable Long initId, @RequestBody Goal goal) {
        return ResponseEntity.ok(hierarchyService.addGoal(initId, goal));
    }

    @PostMapping("/goals/{goalId}/objectives")
    public ResponseEntity<Objective> addObjective(@PathVariable Long goalId, @RequestBody Objective objective) {
        return ResponseEntity.ok(hierarchyService.addObjective(goalId, objective));
    }

    @PostMapping("/objectives/{objId}/key-results")
    public ResponseEntity<KeyResult> addKeyResult(@PathVariable Long objId, @RequestBody KeyResult kr) {
        return ResponseEntity.ok(hierarchyService.addKeyResult(objId, kr));
    }

    @PostMapping("/key-results/{krId}/action-items")
    public ResponseEntity<ActionItem> addActionItem(@PathVariable Long krId, @RequestBody ActionItem ai) {
        return ResponseEntity.ok(hierarchyService.addActionItem(krId, ai));
    }

    // Add PUT endpoints for updates and DELETE endpoints for soft delete
}