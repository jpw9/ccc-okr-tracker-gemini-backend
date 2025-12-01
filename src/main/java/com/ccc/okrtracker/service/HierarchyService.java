package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.exception.ResourceNotFoundException;
import com.ccc.okrtracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HierarchyService {

    private final ProjectRepository projectRepo;
    private final StrategicInitiativeRepository initRepo;
    private final GoalRepository goalRepo;
    private final ObjectiveRepository objectiveRepo;
    private final KeyResultRepository krRepo;
    private final ActionItemRepository aiRepo;
    private final CalculationService calculationService;

    public List<Project> getAllProjects() {
        return projectRepo.findAll();
    }

    // --- Add Methods ---

    @Transactional
    public Project createProject(Project project) {
        return projectRepo.save(project);
    }

    @Transactional
    public StrategicInitiative addInitiative(Long projectId, StrategicInitiative init) {
        Project p = projectRepo.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        init.setProject(p);
        StrategicInitiative saved = initRepo.save(init);
        calculationService.recalculateProject(projectId);
        return saved;
    }

    @Transactional
    public Goal addGoal(Long initId, Goal goal) {
        StrategicInitiative init = initRepo.findById(initId).orElseThrow(() -> new ResourceNotFoundException("Initiative not found"));
        goal.setInitiative(init);
        Goal saved = goalRepo.save(goal);
        calculationService.recalculateProject(init.getProject().getId());
        return saved;
    }

    @Transactional
    public Objective addObjective(Long goalId, Objective obj) {
        Goal g = goalRepo.findById(goalId).orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        obj.setGoal(g);
        Objective saved = objectiveRepo.save(obj);
        calculationService.recalculateProject(g.getInitiative().getProject().getId());
        return saved;
    }

    @Transactional
    public KeyResult addKeyResult(Long objId, KeyResult kr) {
        Objective obj = objectiveRepo.findById(objId).orElseThrow(() -> new ResourceNotFoundException("Objective not found"));
        kr.setObjective(obj);
        KeyResult saved = krRepo.save(kr);
        calculationService.recalculateProject(obj.getGoal().getInitiative().getProject().getId());
        return saved;
    }

    @Transactional
    public ActionItem addActionItem(Long krId, ActionItem ai) {
        KeyResult kr = krRepo.findById(krId).orElseThrow(() -> new ResourceNotFoundException("KR not found"));
        ai.setKeyResult(kr);
        ActionItem saved = aiRepo.save(ai);
        calculationService.recalculateProject(kr.getObjective().getGoal().getInitiative().getProject().getId());
        return saved;
    }

    // --- Update Methods (Generic) ---
    // You would implement update methods here that take an ID and DTO/Entity,
    // update fields, save, and then call calculationService.recalculateProject(...)

    // Example: Soft Delete
    @Transactional
    public void deleteEntity(String type, Long id) {
        // Logic to find repo by type, set isActive=false, save, then recalculate
        // For brevity, here is Project delete:
        if (type.equals("Project")) {
            Project p = projectRepo.findById(id).orElseThrow();
            p.softDelete("admin_user");
            projectRepo.save(p);
        }
        // Implement others...
    }
}