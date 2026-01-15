package com.ccc.okrtracker;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.repository.*;
import com.ccc.okrtracker.service.HierarchyService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for hierarchical progress roll-up logic.
 * Verifies that changes to Action Items and Key Results correctly propagate up the hierarchy.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@WithMockUser(username = "admin_user")
public class ProgressRollupIntegrationTest {

    @Autowired
    private HierarchyService hierarchyService;

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private StrategicInitiativeRepository initiativeRepo;

    @Autowired
    private GoalRepository goalRepo;

    @Autowired
    private ObjectiveRepository objectiveRepo;

    @Autowired
    private KeyResultRepository keyResultRepo;

    @Autowired
    private ActionItemRepository actionItemRepo;

    @Autowired
    private EntityManager entityManager;

    private Project project;
    private StrategicInitiative initiative;
    private Goal goal;
    private Objective objective;
    private KeyResult keyResult;

    @BeforeEach
    public void setUp() {
        // Clean up any existing test data
        actionItemRepo.deleteAll();
        keyResultRepo.deleteAll();
        objectiveRepo.deleteAll();
        goalRepo.deleteAll();
        initiativeRepo.deleteAll();
        projectRepo.deleteAll();
        
        // Flush to ensure cleanup is complete
        actionItemRepo.flush();

        // Create base hierarchy
        project = new Project();
        project.setTitle("Test Project");
        project.setDescription("Test project for progress rollup");
        project.setProgress(0);
        project = hierarchyService.createProject(project);

        initiative = new StrategicInitiative();
        initiative.setTitle("Test Initiative");
        initiative.setDescription("Test initiative");
        initiative.setProgress(0);
        initiative = hierarchyService.addInitiative(project.getId(), initiative);

        goal = new Goal();
        goal.setTitle("Test Goal");
        goal.setDescription("Test goal");
        goal.setProgress(0);
        goal = hierarchyService.addGoal(initiative.getId(), goal);

        objective = new Objective();
        objective.setTitle("Test Objective");
        objective.setDescription("Test objective");
        objective.setProgress(0);
        objective = hierarchyService.addObjective(goal.getId(), objective);

        keyResult = new KeyResult();
        keyResult.setTitle("Test Key Result");
        keyResult.setDescription("Test key result");
        keyResult.setProgress(0);
        keyResult = hierarchyService.addKeyResult(objective.getId(), keyResult);
    }

    /**
     * Test Scenario 1: Action Item Roll-up (Binary Average)
     * When 1 out of 2 action items is completed, KR progress should be 50%
     */
    @Test
    public void testActionItemBinaryRollup() {
        // Create two action items (both incomplete)
        ActionItem actionItem1 = new ActionItem();
        actionItem1.setTitle("Action Item 1");
        actionItem1.setDescription("First action item");
        actionItem1.setIsCompleted(false);
        actionItem1.setProgress(0);
        actionItem1 = hierarchyService.addActionItem(keyResult.getId(), actionItem1);

        ActionItem actionItem2 = new ActionItem();
        actionItem2.setTitle("Action Item 2");
        actionItem2.setDescription("Second action item");
        actionItem2.setIsCompleted(false);
        actionItem2.setProgress(0);
        actionItem2 = hierarchyService.addActionItem(keyResult.getId(), actionItem2);

        // Initial state check - all should be 0%
        keyResult = keyResultRepo.findById(keyResult.getId()).orElseThrow();
        assertEquals(0, keyResult.getProgress(), "Initial KR progress should be 0%");

        objective = objectiveRepo.findById(objective.getId()).orElseThrow();
        assertEquals(0, objective.getProgress(), "Initial Objective progress should be 0%");

        project = projectRepo.findById(project.getId()).orElseThrow();
        assertEquals(0, project.getProgress(), "Initial Project progress should be 0%");

        // Complete first action item
        actionItem1.setIsCompleted(true);
        actionItem1.setProgress(100); // Explicitly set progress
        hierarchyService.updateActionItem(actionItem1.getId(), actionItem1);

        // Flush and clear to ensure all changes are persisted and caches are cleared
        entityManager.flush();
        entityManager.clear();

        // Verify progress propagation
        actionItem1 = actionItemRepo.findById(actionItem1.getId()).orElseThrow();
        assertEquals(100, actionItem1.getProgress(), "Completed action item should be 100%");

        keyResult = keyResultRepo.findById(keyResult.getId()).orElseThrow();
        assertEquals(50, keyResult.getProgress(), "KR with 1/2 completed action items should be 50%");

        objective = objectiveRepo.findById(objective.getId()).orElseThrow();
        assertEquals(50, objective.getProgress(), "Objective should inherit 50% from KR");

        goal = goalRepo.findById(goal.getId()).orElseThrow();
        assertEquals(50, goal.getProgress(), "Goal should inherit 50% from Objective");

        initiative = initiativeRepo.findById(initiative.getId()).orElseThrow();
        assertEquals(50, initiative.getProgress(), "Initiative should inherit 50% from Goal");

        project = projectRepo.findById(project.getId()).orElseThrow();
        assertEquals(50, project.getProgress(), "Project should inherit 50% from Initiative");
    }

    /**
     * Test Scenario 2: Metric-Based Roll-up (Percentage of Range)
     * Progress = ((current - start) / (target - start)) * 100
     */
    @Test
    public void testMetricBasedRollup() {
        // Create a new objective and key result for metric-based testing
        Objective metricObjective = new Objective();
        metricObjective.setTitle("Metric Test Objective");
        metricObjective.setDescription("Objective for metric testing");
        metricObjective.setProgress(0);
        metricObjective = hierarchyService.addObjective(goal.getId(), metricObjective);

        KeyResult metricKR = new KeyResult();
        metricKR.setTitle("Metric Key Result");
        metricKR.setDescription("Key result with metrics");
        metricKR.setMetricStart(10.0);
        metricKR.setMetricTarget(110.0);
        metricKR.setMetricCurrent(10.0);
        metricKR.setProgress(0);
        metricKR = hierarchyService.addKeyResult(metricObjective.getId(), metricKR);

        // Initial state - should be 0%
        metricKR = keyResultRepo.findById(metricKR.getId()).orElseThrow();
        assertEquals(0, metricKR.getProgress(), "Initial metric KR progress should be 0%");

        // Update current to 35.0
        // Progress = (35-10)/(110-10) = 25/100 = 25%
        metricKR.setMetricCurrent(35.0);
        hierarchyService.updateKeyResult(metricKR.getId(), metricKR);

        // Verify metric calculation
        metricKR = keyResultRepo.findById(metricKR.getId()).orElseThrow();
        assertEquals(25, metricKR.getProgress(), "KR progress should be 25% based on metric calculation");

        // Verify propagation to parent
        metricObjective = objectiveRepo.findById(metricObjective.getId()).orElseThrow();
        assertEquals(25, metricObjective.getProgress(), "Objective should inherit 25% from metric KR");

        // Update current to 60.0
        // Progress = (60-10)/(110-10) = 50/100 = 50%
        metricKR.setMetricCurrent(60.0);
        hierarchyService.updateKeyResult(metricKR.getId(), metricKR);

        metricKR = keyResultRepo.findById(metricKR.getId()).orElseThrow();
        assertEquals(50, metricKR.getProgress(), "KR progress should be 50% after metric update");

        metricObjective = objectiveRepo.findById(metricObjective.getId()).orElseThrow();
        assertEquals(50, metricObjective.getProgress(), "Objective should update to 50%");
    }

    /**
     * Test Scenario 3A: Inactive Filter
     * Inactive children should not affect parent progress
     */
    @Test
    public void testInactiveFilterInRollup() {
        // Create a second initiative
        StrategicInitiative initiative2 = new StrategicInitiative();
        initiative2.setTitle("Second Initiative");
        initiative2.setDescription("Second initiative to test inactive filter");
        initiative2.setProgress(100); // Set to 100%
        initiative2 = hierarchyService.addInitiative(project.getId(), initiative2);

        // Project now has 2 initiatives: one at 0%, one at 100%
        // Average should be 50%
        project = projectRepo.findById(project.getId()).orElseThrow();
        assertEquals(50, project.getProgress(), "Project with 2 active initiatives (0% and 100%) should be 50%");

        // Mark second initiative as inactive
        initiative2.setIsActive(false);
        hierarchyService.updateStrategicInitiative(initiative2.getId(), initiative2);

        // Project should now only consider the first initiative (0%)
        project = projectRepo.findById(project.getId()).orElseThrow();
        assertEquals(0, project.getProgress(), "Project should ignore inactive initiative and show 0%");
    }

    /**
     * Test Scenario 3B: Null Safety
     * Null metricCurrent should default to 0.0
     */
    @Test
    public void testNullMetricCurrentDefaultsToZero() {
        KeyResult nullMetricKR = new KeyResult();
        nullMetricKR.setTitle("Null Metric KR");
        nullMetricKR.setDescription("KR with null current");
        nullMetricKR.setMetricStart(0.0);
        nullMetricKR.setMetricTarget(100.0);
        nullMetricKR.setMetricCurrent(null); // Explicitly null
        nullMetricKR = hierarchyService.addKeyResult(objective.getId(), nullMetricKR);

        // Should not throw NPE
        nullMetricKR = keyResultRepo.findById(nullMetricKR.getId()).orElseThrow();
        assertNotNull(nullMetricKR, "KR with null metricCurrent should be created");
        assertEquals(0, nullMetricKR.getProgress(), "KR with null metricCurrent should have 0% progress");
    }

    /**
     * Test Scenario 3C: Division by Zero
     * When metricTarget equals metricStart, progress should be 0 without crash
     */
    @Test
    public void testDivisionByZeroHandling() {
        KeyResult divByZeroKR = new KeyResult();
        divByZeroKR.setTitle("Division by Zero KR");
        divByZeroKR.setDescription("KR with equal start and target");
        divByZeroKR.setMetricStart(50.0);
        divByZeroKR.setMetricTarget(50.0); // Same as start
        divByZeroKR.setMetricCurrent(50.0);
        divByZeroKR = hierarchyService.addKeyResult(objective.getId(), divByZeroKR);

        // Should not throw ArithmeticException
        divByZeroKR = keyResultRepo.findById(divByZeroKR.getId()).orElseThrow();
        assertNotNull(divByZeroKR, "KR with equal start/target should be created");
        assertEquals(0, divByZeroKR.getProgress(), "KR with equal start/target should have 0% progress");

        // Try updating current to a different value
        divByZeroKR.setMetricCurrent(75.0);
        hierarchyService.updateKeyResult(divByZeroKR.getId(), divByZeroKR);

        divByZeroKR = keyResultRepo.findById(divByZeroKR.getId()).orElseThrow();
        assertEquals(0, divByZeroKR.getProgress(), "KR progress should remain 0% when target equals start");
    }

    /**
     * Test Scenario 4: Complex hierarchy with mixed progress
     * Verify weighted averaging across multiple levels
     */
    @Test
    public void testComplexHierarchyRollup() {
        // Create a second goal under the initiative
        Goal goal2 = new Goal();
        goal2.setTitle("Second Goal");
        goal2.setDescription("Second goal for complex test");
        goal2.setProgress(0);
        goal2 = hierarchyService.addGoal(initiative.getId(), goal2);

        // Create objective under second goal
        Objective objective2 = new Objective();
        objective2.setTitle("Second Objective");
        objective2.setDescription("Objective under second goal");
        objective2.setProgress(0);
        objective2 = hierarchyService.addObjective(goal2.getId(), objective2);

        // Create key result under second objective with 100% progress
        KeyResult keyResult2 = new KeyResult();
        keyResult2.setTitle("Second Key Result");
        keyResult2.setDescription("KR with 100% progress");
        keyResult2.setMetricStart(0.0);
        keyResult2.setMetricTarget(100.0);
        keyResult2.setMetricCurrent(100.0);
        keyResult2 = hierarchyService.addKeyResult(objective2.getId(), keyResult2);

        // Now initiative has 2 goals:
        // - Goal 1 (with objective at 0%) -> 0%
        // - Goal 2 (with objective at 100%) -> 100%
        // Initiative average should be 50%
        initiative = initiativeRepo.findById(initiative.getId()).orElseThrow();
        assertEquals(50, initiative.getProgress(), "Initiative with 2 goals (0% and 100%) should be 50%");

        project = projectRepo.findById(project.getId()).orElseThrow();
        assertEquals(50, project.getProgress(), "Project should reflect 50% from initiative");
    }
}
