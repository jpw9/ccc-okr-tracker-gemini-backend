package com.ccc.okrtracker.controller;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.service.HierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hierarchy")
@RequiredArgsConstructor
public class HierarchyController {

    private final HierarchyService hierarchyService;

    // --- GET ---

    @GetMapping("/projects")
    @PreAuthorize("hasAnyAuthority('VIEW_STRATEGY', 'MANAGE_STRATEGY')")
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(hierarchyService.getAllProjects());
    }

    // --- POST (Create) ---

    @PostMapping("/projects")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        return ResponseEntity.ok(hierarchyService.createProject(project));
    }

    @PostMapping("/projects/{projectId}/initiatives")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<StrategicInitiative> addInitiative(@PathVariable Long projectId, @RequestBody StrategicInitiative init) {
        return ResponseEntity.ok(hierarchyService.addInitiative(projectId, init));
    }

    @PostMapping("/initiatives/{initId}/goals")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<Goal> addGoal(@PathVariable Long initId, @RequestBody Goal goal) {
        return ResponseEntity.ok(hierarchyService.addGoal(initId, goal));
    }

    @PostMapping("/goals/{goalId}/objectives")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<Objective> addObjective(@PathVariable Long goalId, @RequestBody Objective objective) {
        return ResponseEntity.ok(hierarchyService.addObjective(goalId, objective));
    }

    @PostMapping("/objectives/{objId}/key-results")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<KeyResult> addKeyResult(@PathVariable Long objId, @RequestBody KeyResult kr) {
        return ResponseEntity.ok(hierarchyService.addKeyResult(objId, kr));
    }

    @PostMapping("/key-results/{krId}/action-items")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<ActionItem> addActionItem(@PathVariable Long krId, @RequestBody ActionItem ai) {
        return ResponseEntity.ok(hierarchyService.addActionItem(krId, ai));
    }

    // --- PUT (Update) ---

    @PutMapping("/projects/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<Project> updateProject(@PathVariable Long id, @RequestBody Project project) {
        return ResponseEntity.ok(hierarchyService.updateProject(id, project));
    }

    @PutMapping("/initiatives/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<StrategicInitiative> updateStrategicInitiative(@PathVariable Long id, @RequestBody StrategicInitiative init) {
        return ResponseEntity.ok(hierarchyService.updateStrategicInitiative(id, init));
    }

    @PutMapping("/goals/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<Goal> updateGoal(@PathVariable Long id, @RequestBody Goal goal) {
        return ResponseEntity.ok(hierarchyService.updateGoal(id, goal));
    }

    @PutMapping("/objectives/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<Objective> updateObjective(@PathVariable Long id, @RequestBody Objective objective) {
        return ResponseEntity.ok(hierarchyService.updateObjective(id, objective));
    }

    @PutMapping("/key-results/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<KeyResult> updateKeyResult(@PathVariable Long id, @RequestBody KeyResult kr) {
        return ResponseEntity.ok(hierarchyService.updateKeyResult(id, kr));
    }

    // THIS IS THE CRITICAL ENDPOINT: PUT /api/hierarchy/action-items/{id}
    @PutMapping("/action-items/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public ResponseEntity<ActionItem> updateActionItem(@PathVariable Long id, @RequestBody ActionItem ai) {
        return ResponseEntity.ok(hierarchyService.updateActionItem(id, ai));
    }
}