package com.ccc.okrtracker.service;

import com.ccc.okrtracker.dto.HierarchyImportRow;
import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList; // Added for explicit List initialization
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ProjectRepository projectRepo;
    private final StrategicInitiativeRepository initRepo;
    private final GoalRepository goalRepo;
    private final ObjectiveRepository objectiveRepo;
    private final KeyResultRepository krRepo;
    private final ActionItemRepository aiRepo;
    private final CalculationService calculationService;

    // Placeholder cache to hold entities during batch processing and avoid redundant DB lookups.
    private Project currentProject;
    private StrategicInitiative currentInitiative;
    private Goal currentGoal;
    private Objective currentObjective;
    private KeyResult currentKeyResult;

    @Transactional
    public void importHierarchy(List<HierarchyImportRow> rows) {
        currentProject = null;
        currentInitiative = null;
        currentGoal = null;
        currentObjective = null;
        currentKeyResult = null;

        Project topLevelProject = null;

        for (HierarchyImportRow row : rows) {

            // 1. PROJECT
            if (row.getProjectTitle() != null && !row.getProjectTitle().isEmpty()) {
                if (currentProject == null || !currentProject.getTitle().equals(row.getProjectTitle())) {
                    // Try to find existing Project (Assuming Project titles are unique for simplicity)
                    // NOTE: Real implementation should use a dedicated repository method to find by title.

                    // FIX: Ensure Project entities returned by findAll() have initialized collections.
                    // However, since findAll() loads the Project entity, and the collections are @OneToMany
                    // they are likely lazy-loaded or proxied. The safest way is to fix the access.
                    Optional<Project> existingProject = projectRepo.findAll().stream()
                            .filter(p -> p.getTitle().equals(row.getProjectTitle()))
                            .findFirst();

                    currentProject = existingProject.orElseGet(() -> {
                        // When creating a new project, the list is initialized, so it's safe.
                        Project newProject = new Project(row.getProjectTitle(), row.getProjectDescription(), 0, null);
                        return projectRepo.save(newProject);
                    });

                    // Reset lower levels
                    currentInitiative = null;
                    currentGoal = null;
                    currentObjective = null;
                    currentKeyResult = null;
                }
                topLevelProject = currentProject;
            }

            // Must have a project to continue
            if (currentProject == null) continue;


            // 2. STRATEGIC INITIATIVE
            if (row.getInitiativeTitle() != null && !row.getInitiativeTitle().isEmpty()) {
                if (currentInitiative == null || !currentInitiative.getTitle().equals(row.getInitiativeTitle())) {
                    // FIX: Defensive null check for currentProject.getInitiatives() (Line 75)
                    currentInitiative = Optional.ofNullable(currentProject.getInitiatives())
                            .orElse(Collections.emptyList()).stream()
                            .filter(init -> init.getTitle().equals(row.getInitiativeTitle()))
                            .findFirst()
                            .orElseGet(() -> {
                                StrategicInitiative newInit = new StrategicInitiative(row.getInitiativeTitle(), row.getInitiativeDescription(), 0, currentProject, null);

                                // CRITICAL FIX: Ensure parent list is non-null before adding (Line 88)
                                List<StrategicInitiative> initiatives = currentProject.getInitiatives();
                                if (initiatives == null) {
                                    initiatives = new ArrayList<>();
                                    currentProject.setInitiatives(initiatives);
                                }
                                initiatives.add(newInit);

                                return initRepo.save(newInit);
                            });

                    // Reset lower levels
                    currentGoal = null;
                    currentObjective = null;
                    currentKeyResult = null;
                }
            }

            // Must have an initiative to continue
            if (currentInitiative == null) continue;


            // 3. GOAL
            if (row.getGoalTitle() != null && !row.getGoalTitle().isEmpty()) {
                if (currentGoal == null || !currentGoal.getTitle().equals(row.getGoalTitle())) {
                    // FIX: Defensive null check for currentInitiative.getGoals()
                    currentGoal = Optional.ofNullable(currentInitiative.getGoals())
                            .orElse(Collections.emptyList()).stream()
                            .filter(g -> g.getTitle().equals(row.getGoalTitle()))
                            .findFirst()
                            .orElseGet(() -> {
                                Goal newGoal = new Goal(row.getGoalTitle(), row.getGoalDescription(), 0, currentInitiative, null);

                                // CRITICAL FIX: Ensure parent list is non-null before adding
                                List<Goal> goals = currentInitiative.getGoals();
                                if (goals == null) {
                                    goals = new ArrayList<>();
                                    currentInitiative.setGoals(goals);
                                }
                                goals.add(newGoal);

                                return goalRepo.save(newGoal);
                            });

                    // Reset lower levels
                    currentObjective = null;
                    currentKeyResult = null;
                }
            }

            // Must have a goal to continue
            if (currentGoal == null) continue;


            // 4. OBJECTIVE
            if (row.getObjectiveTitle() != null && !row.getObjectiveTitle().isEmpty()) {
                if (currentObjective == null || !currentObjective.getTitle().equals(row.getObjectiveTitle())) {
                    // FIX: Defensive null check for currentGoal.getObjectives()
                    currentObjective = Optional.ofNullable(currentGoal.getObjectives())
                            .orElse(Collections.emptyList()).stream()
                            .filter(o -> o.getTitle().equals(row.getObjectiveTitle()))
                            .findFirst()
                            .orElseGet(() -> {
                                Objective newObj = new Objective();
                                newObj.setTitle(row.getObjectiveTitle());
                                newObj.setDescription(row.getObjectiveDescription());
                                newObj.setAssignee(row.getObjectiveAssignee());
                                newObj.setYear(row.getObjectiveYear());
                                newObj.setQuarter(row.getObjectiveQuarter());
                                newObj.setDueDate(row.getObjectiveDueDate());
                                newObj.setGoal(currentGoal);
                                newObj.setProgress(0);

                                // CRITICAL FIX: Ensure parent list is non-null before adding
                                List<Objective> objectives = currentGoal.getObjectives();
                                if (objectives == null) {
                                    objectives = new ArrayList<>();
                                    currentGoal.setObjectives(objectives);
                                }
                                objectives.add(newObj);

                                return objectiveRepo.save(newObj);
                            });

                    // Reset lower level
                    currentKeyResult = null;
                }
            }

            // Must have an objective to continue
            if (currentObjective == null) continue;


            // 5. KEY RESULT
            if (row.getKrTitle() != null && !row.getKrTitle().isEmpty()) {
                if (currentKeyResult == null || !currentKeyResult.getTitle().equals(row.getKrTitle())) {
                    // FIX: Defensive null check for currentObjective.getKeyResults()
                    currentKeyResult = Optional.ofNullable(currentObjective.getKeyResults())
                            .orElse(Collections.emptyList()).stream()
                            .filter(kr -> kr.getTitle().equals(row.getKrTitle()))
                            .findFirst()
                            .orElseGet(() -> {
                                KeyResult newKr = new KeyResult();
                                newKr.setTitle(row.getKrTitle());
                                newKr.setDescription(row.getKrDescription());
                                newKr.setAssignee(row.getKrAssignee());
                                newKr.setMetricStart(Optional.ofNullable(row.getKrMetricStart()).orElse(0.0));
                                newKr.setMetricTarget(Optional.ofNullable(row.getKrMetricTarget()).orElse(0.0));
                                newKr.setMetricCurrent(Optional.ofNullable(row.getKrMetricCurrent()).orElse(0.0));
                                newKr.setUnit(row.getKrUnit());
                                newKr.setObjective(currentObjective);
                                newKr.setProgress(0);

                                // CRITICAL FIX: Ensure parent list is non-null before adding
                                List<KeyResult> keyResults = currentObjective.getKeyResults();
                                if (keyResults == null) {
                                    keyResults = new ArrayList<>();
                                    currentObjective.setKeyResults(keyResults);
                                }
                                keyResults.add(newKr);

                                return krRepo.save(newKr);
                            });
                }
            }

            // Must have a KR to continue
            if (currentKeyResult == null) continue;


            // 6. ACTION ITEM
            if (row.getActionItemTitle() != null && !row.getActionItemTitle().isEmpty()) {
                // Action Items don't cascade, so we always create a new one if it's specified in the row.
                ActionItem newAi = new ActionItem();
                newAi.setTitle(row.getActionItemTitle());
                newAi.setDescription(row.getActionItemDescription());
                newAi.setAssignee(row.getActionItemAssignee());
                newAi.setDueDate(row.getActionItemDueDate());
                newAi.setIsCompleted(Optional.ofNullable(row.getActionItemIsCompleted()).orElse(false));
                newAi.setKeyResult(currentKeyResult);

                // Set initial progress based on completion
                newAi.setProgress(newAi.getIsCompleted() ? 100 : 0);

                // FIX: Defensive null check for currentKeyResult.getActionItems()
                List<ActionItem> currentAiList = Optional.ofNullable(currentKeyResult.getActionItems())
                        .orElseGet(ArrayList::new);

                // If the list was null, set the initialized list back to the parent KR
                if (currentKeyResult.getActionItems() == null) {
                    currentKeyResult.setActionItems(currentAiList);
                }

                currentAiList.add(newAi);

                aiRepo.save(newAi);
            }
        }

        // After processing, recalculate the top-level project's progress
        if (topLevelProject != null) {
            calculationService.recalculateProject(topLevelProject.getId());
        }
    }
}