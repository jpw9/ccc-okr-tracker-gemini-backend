// File: jpw9/ccc-okr-tracker-gemini-backend/ccc-okr-tracker-gemini-backend-2133aa9e882f23bca9d8c21e07a747afb8685989/src/main/java/com/ccc/okrtracker/service/CalculationService.java

package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.repository.*;
import lombok.RequiredArgsConstructor;
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

    // Helper to safely extract Integer progress, defaulting to 0 if null
    private int safeProgress(Integer progress) {
        return Optional.ofNullable(progress).orElse(0);
    }

    @Transactional
    public void recalculateProject(Long projectId) {
        logger.info("=== RECALCULATE PROJECT START: projectId={} ===", projectId);
        // Fetch fresh to ensure we have the latest data
        Project project = projectRepository.findById(projectId).orElseThrow();

        int projTotal = 0;
        int initCount = 0;

        for (StrategicInitiative init : project.getInitiatives()) {
            if (!init.getIsActive()) continue;

            int initTotal = 0;
            int goalCount = 0;

            for (Goal goal : init.getGoals()) {
                if (!goal.getIsActive()) continue;

                int goalTotal = 0;
                int objCount = 0;

                for (Objective obj : goal.getObjectives()) {
                    if (!obj.getIsActive()) continue;

                    int objTotal = 0;
                    int krCount = 0;

                    for (KeyResult kr : obj.getKeyResults()) {
                        if (!kr.getIsActive()) continue;

                        // KR Logic: Smart calculation based on manual lock flag
                        // 1. If KR was manually set (manualProgressSet=true), use direct value and ignore action items
                        // 2. Otherwise, calculate from action items or metrics
                        int krProgress = 0;
                        
                        boolean manuallySet = kr.getManualProgressSet() != null && kr.getManualProgressSet();
                        
                        if (manuallySet) {
                            // If manually set, we use the progress directly, but sync from metrics if they were updated
                            krProgress = safeProgress(kr.getProgress());
                            
                            if (kr.getMetricTarget() != null && kr.getMetricTarget() > 0 && kr.getMetricCurrent() != null) {
                                double target = kr.getMetricTarget();
                                double start = Optional.ofNullable(kr.getMetricStart()).orElse(0.0);
                                double current = kr.getMetricCurrent();
                                double range = target - start;
                                if (range != 0.0) {
                                    krProgress = (int) Math.min(100, Math.max(0, Math.round(((current - start) / range) * 100)));
                                }
                            }
                            logger.info("KR {} is MANUAL: using progress={}", kr.getId(), krProgress);
                        } else {
                            // KR was not manually set, calculate from action items or metrics
                            
                            // Check if action items exist and should be used
                            long activeAiCount = kr.getActionItems().stream()
                                    .filter(ai -> ai != null)
                                    .filter(BaseEntity::getIsActive)
                                    .count();

                            if (activeAiCount > 0) {
                                // Calculate KR progress from average of action items
                                double aiSum = kr.getActionItems().stream()
                                        .filter(ai -> ai != null)
                                        .filter(BaseEntity::getIsActive)
                                        .mapToInt(ai -> safeProgress(ai.getProgress()))
                                        .sum();
                                krProgress = (int) Math.min(100, Math.round(aiSum / activeAiCount));
                                
                                // Update metricCurrent to reflect the calculated progress
                                if (kr.getMetricTarget() != null && kr.getMetricTarget() > 0) {
                                    double start = Optional.ofNullable(kr.getMetricStart()).orElse(0.0);
                                    double target = kr.getMetricTarget();
                                    double range = target - start;
                                    double newCurrent = start + (range * krProgress / 100.0);
                                    kr.setMetricCurrent(newCurrent);
                                }
                            } else if (kr.getMetricTarget() != null && kr.getMetricTarget() > 0 && 
                                      kr.getMetricCurrent() != null) {
                                // No action items, calculate from metrics
                                double target = kr.getMetricTarget();
                                double start = Optional.ofNullable(kr.getMetricStart()).orElse(0.0);
                                double current = kr.getMetricCurrent();

                                double range = target - start;

                                if (range != 0.0) {
                                    double percentage = ((current - start) / range) * 100;
                                    krProgress = (int) Math.min(100, Math.max(0, Math.round(percentage)));
                                } else if (current == start) {
                                    krProgress = 0;
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
                    }
                }

                if (objCount > 0) {
                    int newGoalProgress = Math.round((float) goalTotal / objCount);
                    goal.setProgress(newGoalProgress);
                    goalRepository.save(goal);
                    initTotal += newGoalProgress;
                    goalCount++;
                }
            }

            if (goalCount > 0) {
                int newInitProgress = Math.round((float) initTotal / goalCount);
                init.setProgress(newInitProgress);
                initiativeRepository.save(init);
                projTotal += newInitProgress;
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