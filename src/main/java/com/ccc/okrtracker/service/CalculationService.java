// File: jpw9/ccc-okr-tracker-gemini-backend/ccc-okr-tracker-gemini-backend-2133aa9e882f23bca9d8c21e07a747afb8685989/src/main/java/com/ccc/okrtracker/service/CalculationService.java

package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private static final Logger logger = LoggerFactory.getLogger(CalculationService.class);

    private final ProjectRepository projectRepository;
    private final KeyResultRepository krRepository;
    private final ObjectiveRepository objectiveRepository;
    private final GoalRepository goalRepository;
    private final StrategicInitiativeRepository initiativeRepository;
    private final EntityManager entityManager;

    // Helper to safely extract Integer progress, defaulting to 0 if null
    private int safeProgress(Integer progress) {
        return Optional.ofNullable(progress).orElse(0);
    }

    @Transactional
    public void recalculateProject(Long projectId) {
        logger.info("=== RECALCULATE PROJECT START: projectId={} ===", projectId);
        
        // Synchronize any pending changes to database before clearing cache
        // This ensures all previous saves are committed before we fetch fresh data
        try {
            entityManager.flush();
        } catch (Exception e) {
            // If flush fails, log warning but continue - this might happen if entities are in invalid state
            logger.warn("Failed to flush before recalculation, continuing anyway: {}", e.getMessage());
        }
        
        // Clear persistence context to ensure we fetch fresh data from database
        // This prevents stale data issues in test environments and long-running transactions
        entityManager.clear();
        
        // Fetch project
        Project project = projectRepository.findById(projectId).orElseThrow();

        // Force initialization of lazy collections at all levels
        Hibernate.initialize(project.getInitiatives());
        
        for (StrategicInitiative init : project.getInitiatives()) {
            Hibernate.initialize(init.getGoals());
            for (Goal goal : init.getGoals()) {
                Hibernate.initialize(goal.getObjectives());
                for (Objective obj : goal.getObjectives()) {
                    Hibernate.initialize(obj.getKeyResults());
                    for (KeyResult kr : obj.getKeyResults()) {
                        Hibernate.initialize(kr.getActionItems());
                    }
                }
            }
        }
        
        int projTotal = 0;
        int initCount = 0;

        for (StrategicInitiative init : project.getInitiatives()) {
            logger.info("Initiative id={}, isActive={}", init.getId(), init.getIsActive());
            if (!init.getIsActive()) continue;

            int initTotal = 0;
            int goalCount = 0;

            for (Goal goal : init.getGoals()) {
                logger.info("  Goal id={}, isActive={}", goal.getId(), goal.getIsActive());
                if (!goal.getIsActive()) continue;

                int goalTotal = 0;
                int objCount = 0;

                for (Objective obj : goal.getObjectives()) {
                    logger.info("    Objective id={}, isActive={}", obj.getId(), obj.getIsActive());
                    if (!obj.getIsActive()) continue;

                    int objTotal = 0;
                    int krCount = 0;

                    for (KeyResult kr : obj.getKeyResults()) {
                        logger.info("      KeyResult id={}, isActive={}", kr.getId(), kr.getIsActive());
                        if (!kr.getIsActive()) continue;

                        // KR Logic: Smart calculation based on manual lock flag
                        // 1. If KR was manually set (manualProgressSet=true), use direct value and ignore action items
                        // 2. Otherwise, calculate from action items or metrics
                        int krProgress = 0;
                        
                        boolean manuallySet = kr.getManualProgressSet() != null && kr.getManualProgressSet();
                        
                        logger.info("KR {} calculation: manualProgressSet={}, hasActionItems={}, hasMetrics={}", 
                                kr.getId(), 
                                manuallySet, 
                                !kr.getActionItems().isEmpty(),
                                (kr.getMetricTarget() != null && kr.getMetricCurrent() != null));
                        
                        if (manuallySet) {
                            // If manually set, use the progress value directly - don't recalculate or sync from metrics
                            // This preserves the user's manual input while allowing rollup to parent entities
                            krProgress = safeProgress(kr.getProgress());
                            logger.info("KR {} is MANUAL: using progress={}", kr.getId(), krProgress);
                        } else {
                            // KR was not manually set, calculate from action items or metrics
                            
                            // Check if action items exist and should be used
                            long totalAiCount = kr.getActionItems().stream()
                                    .filter(ai -> ai != null)
                                    .count();
                            long activeAiCount = kr.getActionItems().stream()
                                    .filter(ai -> ai != null)
                                    .filter(BaseEntity::getIsActive)
                                    .count();
                            
                            logger.info("KR {} action items: total={}, active={}", kr.getId(), totalAiCount, activeAiCount);

                            if (activeAiCount > 0) {
                                // Calculate KR progress from average of action items
                                double aiSum = kr.getActionItems().stream()
                                        .filter(ai -> ai != null)
                                        .filter(BaseEntity::getIsActive)
                                        .mapToInt(ai -> safeProgress(ai.getProgress()))
                                        .sum();
                                krProgress = (int) Math.min(100, Math.round(aiSum / activeAiCount));
                                logger.info("KR {} calculated from {} active action items: progress={}", kr.getId(), activeAiCount, krProgress);
                                
                                // Update metricCurrent to reflect the calculated progress
                                if (kr.getMetricTarget() != null && kr.getMetricTarget() > 0) {
                                    double start = Optional.ofNullable(kr.getMetricStart()).orElse(0.0);
                                    double target = kr.getMetricTarget();
                                    double range = target - start;
                                    double newCurrent = start + (range * krProgress / 100.0);
                                    kr.setMetricCurrent(newCurrent);
                                }
                            } else if (totalAiCount > 0) {
                                // All action items were deleted - reset progress to 0
                                krProgress = 0;
                                logger.info("KR {} has {} deleted action items, resetting progress to 0", kr.getId(), totalAiCount);
                                // Also reset metricCurrent if metrics exist
                                if (kr.getMetricTarget() != null && kr.getMetricTarget() > 0) {
                                    double start = Optional.ofNullable(kr.getMetricStart()).orElse(0.0);
                                    kr.setMetricCurrent(start);
                                }
                            } else if (kr.getMetricTarget() != null && kr.getMetricTarget() > 0 && 
                                      kr.getMetricCurrent() != null) {
                                // No action items, calculate from metrics
                                double target = kr.getMetricTarget();
                                double start = Optional.ofNullable(kr.getMetricStart()).orElse(0.0);
                                double current = kr.getMetricCurrent();

                                double range = target - start;
                                
                                logger.info("KR {} calculating from metrics: start={}, current={}, target={}, range={}", 
                                        kr.getId(), start, current, target, range);

                                if (range != 0.0) {
                                    double percentage = ((current - start) / range) * 100;
                                    krProgress = (int) Math.min(100, Math.max(0, Math.round(percentage)));
                                    logger.info("KR {} metric calculation: percentage={}, krProgress={}", kr.getId(), percentage, krProgress);
                                } else if (current == start) {
                                    krProgress = 0;
                                    logger.info("KR {} metric calculation: range is 0 and current==start, setting progress=0", kr.getId());
                                }
                            } else {
                                // No action items and no metrics, use current progress value
                                krProgress = safeProgress(kr.getProgress());
                            }
                        }

                        kr.setProgress(krProgress);
                        krRepository.save(kr);  
                        objTotal += krProgress; 
                        krCount++;
                    }

                    if (krCount > 0) {
                        int newObjProgress = Math.round((float) objTotal / krCount);
                        obj.setProgress(newObjProgress);
                        objectiveRepository.save(obj);
                        goalTotal += newObjProgress;
                        objCount++;
                    } else {
                        // No active KRs - set objective progress to 0
                        obj.setProgress(0);
                        objectiveRepository.save(obj);
                        goalTotal += 0;
                        objCount++;
                    }
                }

                if (objCount > 0) {
                    int newGoalProgress = Math.round((float) goalTotal / objCount);
                    goal.setProgress(newGoalProgress);
                    goalRepository.save(goal);
                    initTotal += newGoalProgress;
                    goalCount++;
                } else {
                    // No active objectives - set goal progress to 0
                    goal.setProgress(0);
                    goalRepository.save(goal);
                    initTotal += 0;
                    goalCount++;
                }
            }

            if (goalCount > 0) {
                int newInitProgress = Math.round((float) initTotal / goalCount);
                init.setProgress(newInitProgress);
                initiativeRepository.save(init);
                projTotal += newInitProgress;
                initCount++;
            } else {
                // No active goals - set initiative progress to 0
                init.setProgress(0);
                initiativeRepository.save(init);
                projTotal += 0;
                initCount++;
            }
        }

        if (initCount > 0) {
            int newProjProgress = Math.round((float) projTotal / initCount);
            project.setProgress(newProjProgress);
            projectRepository.save(project);
        }
        
        logger.info("=== RECALCULATE PROJECT END: projectId={} ===", projectId);
    }
}