package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.exception.ResourceNotFoundException;
import com.ccc.okrtracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    // TODO: Replace hardcoded user with authenticated user from SecurityContext
    private static final String CURRENT_USER = "admin_user";

    public List<Project> getAllProjects() {
        return projectRepo.findAll(); // Using the eager fetch method
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

        // Ensure progress is set based on isCompleted status upon creation
        if (Optional.ofNullable(ai.getIsCompleted()).orElse(false)) {
            ai.setProgress(100);
        } else {
            ai.setProgress(0);
        }

        ActionItem saved = aiRepo.save(ai);
        calculationService.recalculateProject(kr.getObjective().getGoal().getInitiative().getProject().getId());
        return saved;
    }

    // --- Update Methods (PUT) ---

    @Transactional
    public Project updateProject(Long id, Project updates) {
        Project p = projectRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Apply updates
        Optional.ofNullable(updates.getTitle()).ifPresent(p::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(p::setDescription);
        // Progress can be set manually, but usually is recalculated
        Optional.ofNullable(updates.getProgress()).ifPresent(p::setProgress);

        // Handle Soft Delete/Restore Logic
        if (updates.getIsActive() != null && !updates.getIsActive()) {
            p.softDelete(CURRENT_USER);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            p.restore();
        }

        Project saved = projectRepo.save(p);
        calculationService.recalculateProject(id);
        return saved;
    }

    @Transactional
    public StrategicInitiative updateStrategicInitiative(Long id, StrategicInitiative updates) {
        StrategicInitiative init = initRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Initiative not found"));

        Optional.ofNullable(updates.getTitle()).ifPresent(init::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(init::setDescription);
        Optional.ofNullable(updates.getProgress()).ifPresent(init::setProgress);

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            init.softDelete(CURRENT_USER);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            init.restore();
        }

        StrategicInitiative saved = initRepo.save(init);
        calculationService.recalculateProject(saved.getProject().getId());
        return saved;
    }

    @Transactional
    public Goal updateGoal(Long id, Goal updates) {
        Goal g = goalRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        Optional.ofNullable(updates.getTitle()).ifPresent(g::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(g::setDescription);
        Optional.ofNullable(updates.getProgress()).ifPresent(g::setProgress);

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            g.softDelete(CURRENT_USER);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            g.restore();
        }

        Goal saved = goalRepo.save(g);
        calculationService.recalculateProject(saved.getInitiative().getProject().getId());
        return saved;
    }

    @Transactional
    public Objective updateObjective(Long id, Objective updates) {
        Objective obj = objectiveRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objective not found"));

        Optional.ofNullable(updates.getTitle()).ifPresent(obj::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(obj::setDescription);
        Optional.ofNullable(updates.getAssignee()).ifPresent(obj::setAssignee);
        Optional.ofNullable(updates.getYear()).ifPresent(obj::setYear);
        Optional.ofNullable(updates.getQuarter()).ifPresent(obj::setQuarter);
        Optional.ofNullable(updates.getDueDate()).ifPresent(obj::setDueDate);
        Optional.ofNullable(updates.getProgress()).ifPresent(obj::setProgress);

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            obj.softDelete(CURRENT_USER);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            obj.restore();
        }

        Objective saved = objectiveRepo.save(obj);
        calculationService.recalculateProject(saved.getGoal().getInitiative().getProject().getId());
        return saved;
    }

    @Transactional
    public KeyResult updateKeyResult(Long id, KeyResult updates) {
        KeyResult kr = krRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Key Result not found"));

        Optional.ofNullable(updates.getTitle()).ifPresent(kr::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(kr::setDescription);
        Optional.ofNullable(updates.getAssignee()).ifPresent(kr::setAssignee);
        Optional.ofNullable(updates.getMetricStart()).ifPresent(kr::setMetricStart);
        Optional.ofNullable(updates.getMetricTarget()).ifPresent(kr::setMetricTarget);
        Optional.ofNullable(updates.getMetricCurrent()).ifPresent(kr::setMetricCurrent);
        Optional.ofNullable(updates.getUnit()).ifPresent(kr::setUnit);
        Optional.ofNullable(updates.getProgress()).ifPresent(kr::setProgress);

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            kr.softDelete(CURRENT_USER);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            kr.restore();
        }

        KeyResult saved = krRepo.save(kr);
        calculationService.recalculateProject(saved.getObjective().getGoal().getInitiative().getProject().getId());
        return saved;
    }

    @Transactional
    public ActionItem updateActionItem(Long id, ActionItem updates) {
        ActionItem ai = aiRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Action Item not found"));

        Optional.ofNullable(updates.getTitle()).ifPresent(ai::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(ai::setDescription);
        Optional.ofNullable(updates.getDueDate()).ifPresent(ai::setDueDate);
        Optional.ofNullable(updates.getAssignee()).ifPresent(ai::setAssignee);

        // --- FIX: Apply manual progress first, so it overrides the 'isCompleted' derivation ---
        // If progress is manually sent from the UI (when editing), use it immediately.
        Optional.ofNullable(updates.getProgress()).ifPresent(ai::setProgress);

        // Handle isCompleted status: If set, update the status AND ONLY set progress
        // IF the updates DTO did NOT contain a manual progress value.
        if (updates.getIsCompleted() != null) {
            ai.setIsCompleted(updates.getIsCompleted());

            // Only set progress based on isCompleted if the update DTO DID NOT include a manual progress field.
            if (updates.getProgress() == null) {
                ai.setProgress(updates.getIsCompleted() ? 100 : 0);
            }
        }

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            ai.softDelete(CURRENT_USER);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            ai.restore();
        }

        ActionItem saved = aiRepo.save(ai);
        calculationService.recalculateProject(saved.getKeyResult().getObjective().getGoal().getInitiative().getProject().getId());
        return saved;
    }
}