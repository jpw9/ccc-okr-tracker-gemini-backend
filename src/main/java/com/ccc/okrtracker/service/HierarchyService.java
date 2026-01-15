package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.exception.ResourceNotFoundException;
import com.ccc.okrtracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(HierarchyService.class);

    private final ProjectRepository projectRepo;
    private final StrategicInitiativeRepository initRepo;
    private final GoalRepository goalRepo;
    private final ObjectiveRepository objectiveRepo;
    private final KeyResultRepository krRepo;
    private final ActionItemRepository aiRepo;
    private final CalculationService calculationService;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    /**
     * Retrieves the currently authenticated user from Spring Security context.
     * Extracts the email claim from JWT and looks up the User entity.
     * 
     * @return The authenticated User entity
     * @throws ResourceNotFoundException if user is not found in database
     */
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }

        // Extract email from JWT token
        final String userIdentifier;
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email != null) {
                userIdentifier = email;
            } else {
                String username = jwt.getClaimAsString("preferred_username");
                userIdentifier = username != null ? username : jwt.getSubject();
            }
        } else {
            userIdentifier = authentication.getName();
        }

        if (userIdentifier == null) {
            throw new ResourceNotFoundException("Could not extract user identifier from authentication");
        }

        // Look up user by email
        return userRepository.findByEmail(userIdentifier)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or inactive: " + userIdentifier));
    }

    /**
     * Helper method to get the current user's login string for audit fields.
     * Falls back to "system" if no authenticated user is found.
     * 
     * @return The user's login identifier or "system"
     */
    private String getCurrentUserLogin() {
        try {
            User user = getCurrentAuthenticatedUser();
            return user.getEmail();
        } catch (Exception e) {
            return "system"; // Fallback for system operations or unauthenticated contexts
        }
    }

    /**
     * Get all projects filtered by user's access permissions.
     * Uses ProjectAccessService to determine accessible project IDs.
     */
    public List<Project> getAllProjects() {
        User currentUser = getCurrentAuthenticatedUser();
        Set<Long> accessibleIds = projectAccessService.getAccessibleProjectIds(currentUser);
        
        if (accessibleIds.isEmpty()) {
            return List.of();
        }
        
        return projectRepo.findAll().stream()
            .filter(p -> p.getIsActive() != null && p.getIsActive() && accessibleIds.contains(p.getId()))
            .collect(Collectors.toList());
    }

    /**
     * Get all projects without access filtering (for admin purposes).
     */
    public List<Project> getAllProjectsUnfiltered() {
        return projectRepo.findByIsActiveTrue();
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
            p.softDelete(getCurrentUserLogin());
            // CRITICAL FIX: Cascade soft delete to all children
            cascadeSoftDelete(p, false);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            p.restore();
        }

        Project saved = projectRepo.save(p);
        projectRepo.flush();  // Ensure changes are persisted before recalculation
        calculationService.recalculateProject(id);
        return projectRepo.findById(id).orElseThrow();  // Re-fetch after recalculation
    }

    @Transactional
    public StrategicInitiative updateStrategicInitiative(Long id, StrategicInitiative updates) {
        StrategicInitiative init = initRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Initiative not found"));

        Optional.ofNullable(updates.getTitle()).ifPresent(init::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(init::setDescription);
        Optional.ofNullable(updates.getProgress()).ifPresent(init::setProgress);

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            init.softDelete(getCurrentUserLogin());
            // FIX: Cascade soft delete to children of Initiative
            cascadeSoftDelete(init, false);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            init.restore();
        }

        StrategicInitiative saved = initRepo.save(init);
        initRepo.flush();  // Ensure changes are persisted before recalculation
        Long projectId = saved.getProject().getId();
        calculationService.recalculateProject(projectId);
        return initRepo.findById(id).orElseThrow();  // Re-fetch after recalculation
    }

    @Transactional
    public Goal updateGoal(Long id, Goal updates) {
        Goal g = goalRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        logger.info("=== UPDATE GOAL START: id={}, isActive={} ===", id, updates.getIsActive());

        Optional.ofNullable(updates.getTitle()).ifPresent(g::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(g::setDescription);
        Optional.ofNullable(updates.getProgress()).ifPresent(g::setProgress);

        // Get projectId BEFORE soft delete to avoid detached entity issues
        Long projectId = g.getInitiative().getProject().getId();
        logger.info("Goal id={} belongs to project id={}", id, projectId);

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            logger.info("Soft-deleting Goal id={}", id);
            g.softDelete(getCurrentUserLogin());
            // FIX: Cascade soft delete to children of Goal
            cascadeSoftDelete(g, false);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            g.restore();
        }

        Goal saved = goalRepo.save(g);
        goalRepo.flush();  // Ensure changes are persisted before recalculation
        logger.info("Goal id={} saved and flushed, isActive={}", id, saved.getIsActive());
        logger.info("Triggering recalculation for project id={}", projectId);
        calculationService.recalculateProject(projectId);
        return goalRepo.findById(id).orElseThrow();  // Re-fetch after recalculation
    }

    @Transactional
    public Objective updateObjective(Long id, Objective updates) {
        Objective obj = objectiveRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objective not found"));

        logger.info("=== UPDATE OBJECTIVE START: id={}, isActive={} ===", id, updates.getIsActive());

        Optional.ofNullable(updates.getTitle()).ifPresent(obj::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(obj::setDescription);
        Optional.ofNullable(updates.getAssignee()).ifPresent(obj::setAssignee);
        Optional.ofNullable(updates.getYear()).ifPresent(obj::setYear);
        Optional.ofNullable(updates.getQuarter()).ifPresent(obj::setQuarter);
        Optional.ofNullable(updates.getDueDate()).ifPresent(obj::setDueDate);
        Optional.ofNullable(updates.getProgress()).ifPresent(obj::setProgress);

        // Get projectId BEFORE soft delete to avoid detached entity issues
        Long projectId = obj.getGoal().getInitiative().getProject().getId();
        logger.info("Objective id={} belongs to project id={}", id, projectId);

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            logger.info("Soft-deleting Objective id={}", id);
            obj.softDelete(getCurrentUserLogin());
            // FIX: Cascade soft delete to children of Objective
            cascadeSoftDelete(obj, false);
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            obj.restore();
        }

        Objective saved = objectiveRepo.save(obj);
        objectiveRepo.flush();  // Ensure changes are persisted before recalculation
        logger.info("Objective id={} saved and flushed, isActive={}", id, saved.getIsActive());
        logger.info("Triggering recalculation for project id={}", projectId);
        calculationService.recalculateProject(projectId);
        return objectiveRepo.findById(id).orElseThrow();  // Re-fetch after recalculation
    }

    @Transactional
    public KeyResult updateKeyResult(Long id, KeyResult updates) {
        KeyResult kr = krRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Key Result not found"));

        logger.info("=== UPDATE KEY RESULT START: id={}, isActive={} ===", id, updates.getIsActive());

        Optional.ofNullable(updates.getTitle()).ifPresent(kr::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(kr::setDescription);
        Optional.ofNullable(updates.getAssignee()).ifPresent(kr::setAssignee);
        Optional.ofNullable(updates.getMetricStart()).ifPresent(kr::setMetricStart);
        Optional.ofNullable(updates.getMetricTarget()).ifPresent(kr::setMetricTarget);
        Optional.ofNullable(updates.getMetricCurrent()).ifPresent(kr::setMetricCurrent);
        Optional.ofNullable(updates.getUnit()).ifPresent(kr::setUnit);
        Optional.ofNullable(updates.getProgress()).ifPresent(kr::setProgress);
        
        // Track if we need to recalculate
        boolean needsRecalculation = false;
        
        // When user manually updates KR progress or metrics, lock it and trigger recalculation
        // The calculation service will respect the manualProgressSet flag and use the value directly
        if (updates.getProgress() != null || updates.getMetricCurrent() != null) {
            kr.setManualProgressSet(true);
            needsRecalculation = true;
        }

        // Get projectId BEFORE soft delete to avoid detached entity issues
        Long projectId = kr.getObjective().getGoal().getInitiative().getProject().getId();
        Long krId = kr.getId();
        logger.info("KeyResult id={} belongs to project id={}", krId, projectId);

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            logger.info("Soft-deleting KeyResult id={}", krId);
            kr.softDelete(getCurrentUserLogin());
            // FIX: Cascade soft delete to children of Key Result
            cascadeSoftDelete(kr, false);
            needsRecalculation = true;
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            kr.restore();
            needsRecalculation = true;
        }

        KeyResult saved = krRepo.save(kr);
        krRepo.flush();  // Ensure KR update is persisted before recalculation
        logger.info("KeyResult id={} saved and flushed, isActive={}, needsRecalculation={}", krId, saved.getIsActive(), needsRecalculation);
        
        if (needsRecalculation) {
            logger.info("Triggering recalculation for project id={}", projectId);
            calculationService.recalculateProject(projectId);
        }
        
        // Re-fetch KR after recalculation to ensure it has latest calculated progress
        return krRepo.findById(krId).orElseThrow(() -> new ResourceNotFoundException("Key Result not found"));
    }

    @Transactional
    public ActionItem updateActionItem(Long id, ActionItem updates) {
        ActionItem ai = aiRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Action Item not found"));

        logger.info("Updating ActionItem id={}, incoming isCompleted={}, incoming progress={}", 
                    id, updates.getIsCompleted(), updates.getProgress());
        logger.info("Current state: isCompleted={}, progress={}", ai.getIsCompleted(), ai.getProgress());
        
        Optional.ofNullable(updates.getTitle()).ifPresent(ai::setTitle);
        Optional.ofNullable(updates.getDescription()).ifPresent(ai::setDescription);
        Optional.ofNullable(updates.getDueDate()).ifPresent(ai::setDueDate);
        Optional.ofNullable(updates.getAssignee()).ifPresent(ai::setAssignee);

        // Determine if this is a "Mark as Done" operation vs a full edit
        // Mark as Done: sends isCompleted change but progress is 0 (default)
        // Edit dialog: sends progress > 0 OR progress explicitly set to a value
        Integer incomingProgress = updates.getProgress();
        Boolean incomingIsCompleted = updates.getIsCompleted();
        
        // If progress is explicitly provided and non-zero, it's from the edit dialog
        if (incomingProgress != null && incomingProgress > 0) {
            // Edit dialog scenario: progress is source of truth
            ai.setProgress(incomingProgress);
            // Sync isCompleted based on progress (100 = completed)
            ai.setIsCompleted(incomingProgress >= 100);
            logger.info("Edit dialog mode: using explicit progress={}, setting isCompleted={}", incomingProgress, ai.getIsCompleted());
        } else if (incomingIsCompleted != null) {
            // Mark as Done scenario: isCompleted is source of truth, auto-set progress
            ai.setIsCompleted(incomingIsCompleted);
            ai.setProgress(incomingIsCompleted ? 100 : 0);
            logger.info("Mark as Done mode: setting isCompleted={}, progress={}", incomingIsCompleted, ai.getProgress());
        }
        
        logger.info("After update: isCompleted={}, progress={}", ai.getIsCompleted(), ai.getProgress());

        if (updates.getIsActive() != null && !updates.getIsActive()) {
            ai.softDelete(getCurrentUserLogin());
        } else if (updates.getIsActive() != null && updates.getIsActive()) {
            ai.restore();
        }

        // Save the KeyResult ID BEFORE saving the ActionItem (in case relationship gets detached)
        Long krId = ai.getKeyResult() != null ? ai.getKeyResult().getId() : null;
        logger.info("KeyResult ID before save: {}", krId);

        ActionItem savedAi = aiRepo.save(ai);
        aiRepo.flush();  // Ensure action item is persisted before recalculation
        logger.info("Saved ActionItem id={}, isCompleted={}, progress={}, isActive={}", 
                    savedAi.getId(), savedAi.getIsCompleted(), savedAi.getProgress(), savedAi.getIsActive());
        
        // When action item is updated (including soft-delete), recalculate the project
        if (krId != null) {
            // Re-fetch KR to ensure we have a fresh instance
            KeyResult kr = krRepo.findById(krId).orElse(null);
            logger.info("KeyResult after re-fetch: {}", kr != null ? "found kr.id=" + kr.getId() : "NULL");
            
            if (kr != null) {
                // UNLOCK the KR so it recalculates from action items
                kr.setManualProgressSet(false);
                krRepo.save(kr);
                krRepo.flush(); 
                
                // Navigate to Project ID
                Long projectId = kr.getObjective().getGoal().getInitiative().getProject().getId();
                logger.info("Triggering recalculation for project id={}", projectId);
                calculationService.recalculateProject(projectId);
                
                // Re-fetch action item after recalculation to ensure it's attached and has latest data
                savedAi = aiRepo.findById(savedAi.getId()).orElseThrow();
                logger.info("Final ActionItem after recalc: id={}, isCompleted={}, progress={}", savedAi.getId(), savedAi.getIsCompleted(), savedAi.getProgress());
            }
        }
        
        return savedAi;
    }

    // --- New Recursive Helper for Soft Delete/Restore ---

    /**
     * Recursively applies soft-delete (isActive=false) or restore (isActive=true)
     * to all hierarchical descendants of the given parent entity.
     * NOTE: This method only processes CHILDREN, not the parent itself (parent is handled by caller).
     * @param parent The parent entity (Project, Initiative, Goal, Objective, KeyResult)
     * @param restore If true, restores the entities (isActive=true); otherwise soft-deletes (isActive=false).
     */
    private void cascadeSoftDelete(BaseEntity parent, boolean restore) {
        String currentUser = getCurrentUserLogin();
        
        // Recursively apply to children (NOT to parent - parent is handled by caller)
        if (parent instanceof Project) {
            Project p = (Project) parent;
            // Initialize lazy collection before iterating
            Hibernate.initialize(p.getInitiatives());
            for (StrategicInitiative init : p.getInitiatives()) {
                if (!restore) init.softDelete(currentUser); else init.restore();
                initRepo.save(init);
                cascadeSoftDelete(init, restore);
            }
        } else if (parent instanceof StrategicInitiative) {
            StrategicInitiative init = (StrategicInitiative) parent;
            // Initialize lazy collection before iterating
            Hibernate.initialize(init.getGoals());
            for (Goal goal : init.getGoals()) {
                if (!restore) goal.softDelete(currentUser); else goal.restore();
                goalRepo.save(goal);
                cascadeSoftDelete(goal, restore);
            }
        } else if (parent instanceof Goal) {
            Goal g = (Goal) parent;
            // Initialize lazy collection before iterating
            Hibernate.initialize(g.getObjectives());
            for (Objective obj : g.getObjectives()) {
                if (!restore) obj.softDelete(currentUser); else obj.restore();
                objectiveRepo.save(obj);
                cascadeSoftDelete(obj, restore);
            }
        } else if (parent instanceof Objective) {
            Objective obj = (Objective) parent;
            // Initialize lazy collection before iterating
            Hibernate.initialize(obj.getKeyResults());
            for (KeyResult kr : obj.getKeyResults()) {
                if (!restore) kr.softDelete(currentUser); else kr.restore();
                krRepo.save(kr);
                cascadeSoftDelete(kr, restore);
            }
        } else if (parent instanceof KeyResult) {
            KeyResult kr = (KeyResult) parent;
            // Initialize lazy collection before iterating
            Hibernate.initialize(kr.getActionItems());
            for (ActionItem ai : kr.getActionItems()) {
                if (!restore) ai.softDelete(currentUser); else ai.restore();
                aiRepo.save(ai);
            }
        }
    }
}