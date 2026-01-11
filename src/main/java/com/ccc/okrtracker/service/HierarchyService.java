package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.exception.ResourceNotFoundException;
import com.ccc.okrtracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            // CRITICAL FIX: Cascade soft delete to all children
            cascadeSoftDelete(p, false);
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
            // FIX: Cascade soft delete to children of Initiative
            cascadeSoftDelete(init, false);
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
            // FIX: Cascade soft delete to children of Goal
            cascadeSoftDelete(g, false);
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
            // FIX: Cascade soft delete to children of Objective
            cascadeSoftDelete(obj, false);
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
        
        // When user manually updates KR progress or metrics, lock it (ignore action items)
        if (updates.getProgress() != null || updates.getMetricCurrent() != null) {
            kr.setManualProgressSet(true);
        }

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            kr.softDelete(CURRENT_USER);
            // FIX: Cascade soft delete to children of Key Result
            cascadeSoftDelete(kr, false);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            kr.restore();
        }

        KeyResult saved = krRepo.save(kr);
        krRepo.flush();  // Ensure KR update is persisted before recalculation
        
        // Reload KR to ensure relationships are initialized for navigation to Project
        Long krId = saved.getId();
        KeyResult reloadedKr = krRepo.findById(krId).orElseThrow(() -> new ResourceNotFoundException("Key Result not found"));
        
        Long projectId = reloadedKr.getObjective().getGoal().getInitiative().getProject().getId();
        calculationService.recalculateProject(projectId);
        return reloadedKr;  // Return reloaded KR with all updates and relationships intact
    }

    @Transactional
    public ActionItem updateActionItem(Long id, ActionItem updates) {
        ActionItem ai = aiRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Action Item not found"));

        Optional.ofNullable(updates.getTitle()).ifPresent(ai::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(ai::setDescription);
        Optional.ofNullable(updates.getDueDate()).ifPresent(ai::setDueDate);
        Optional.ofNullable(updates.getAssignee()).ifPresent(ai::setAssignee);

        // --- Apply manual progress first ---
        Optional.ofNullable(updates.getProgress()).ifPresent(ai::setProgress);

        if (updates.getIsCompleted() != null) {
            ai.setIsCompleted(updates.getIsCompleted());
            if (updates.getProgress() == null) {
                ai.setProgress(updates.getIsCompleted() ? 100 : 0);
            }
        }

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            ai.softDelete(CURRENT_USER);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            ai.restore();
        }

        ActionItem savedAi = aiRepo.save(ai);
        
        // When action item is updated, UNLOCK the KR so it recalculates from action items
        KeyResult kr = savedAi.getKeyResult();
        if (kr != null) {
            kr.setManualProgressSet(false);
            krRepo.save(kr);
            krRepo.flush(); 
            
            // Re-fetch KR to safely navigate to Project ID
            KeyResult reloadedKr = krRepo.findById(kr.getId()).orElseThrow();
            Long projectId = reloadedKr.getObjective().getGoal().getInitiative().getProject().getId();
            calculationService.recalculateProject(projectId);
        }
        
        return savedAi;
    }

    // --- New Recursive Helper for Soft Delete/Restore ---

    /**
     * Recursively applies soft-delete (isActive=false) or restore (isActive=true)
     * to all hierarchical descendants of the given parent entity.
     * * @param parent The parent entity (Project, Initiative, Goal, Objective, KeyResult)
     * @param restore If true, restores the entities (isActive=true); otherwise soft-deletes (isActive=false).
     */
    private void cascadeSoftDelete(BaseEntity parent, boolean restore) {
        // Determine the action based on the 'restore' flag
        if (!restore) {
            // Soft delete: set isActive to false and update audit fields
            parent.softDelete(CURRENT_USER);
        } else {
            // Restore: set isActive to true and clear closed audit fields
            parent.restore();
        }

        // Recursively apply to children
        if (parent instanceof Project) {
            Project p = (Project) parent;
            p.getInitiatives().forEach(init -> cascadeSoftDelete(init, restore));
        } else if (parent instanceof StrategicInitiative) {
            StrategicInitiative init = (StrategicInitiative) parent;
            init.getGoals().forEach(goal -> cascadeSoftDelete(goal, restore));
        } else if (parent instanceof Goal) {
            Goal g = (Goal) parent;
            g.getObjectives().forEach(obj -> cascadeSoftDelete(obj, restore));
        } else if (parent instanceof Objective) {
            Objective obj = (Objective) parent;
            obj.getKeyResults().forEach(kr -> cascadeSoftDelete(kr, restore));
        } else if (parent instanceof KeyResult) {
            KeyResult kr = (KeyResult) parent;
            kr.getActionItems().forEach(ai -> cascadeSoftDelete(ai, restore));
        }
    }
}