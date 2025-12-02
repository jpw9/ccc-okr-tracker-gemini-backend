// File: jpw9/ccc-okr-tracker-gemini-backend/ccc-okr-tracker-gemini-backend-2133aa9e882f23bca9d8c21e07a747afb8685989/src/main/java/com/ccc/okrtracker/service/CalculationService.java

package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private final ProjectRepository projectRepository;

    // Helper to safely extract Integer progress, defaulting to 0 if null
    private int safeProgress(Integer progress) {
        return Optional.ofNullable(progress).orElse(0);
    }

    @Transactional
    public void recalculateProject(Long projectId) {
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

                        // KR Logic: If Action Items exist, average them. Else use Metric.
                        int krProgress = 0;

                        // FIX 1: Filter out null ActionItem elements from the set/stream first
                        long activeAiCount = kr.getActionItems().stream()
                                .filter(ai -> ai != null)
                                .filter(BaseEntity::getIsActive)
                                .count();

                        if (activeAiCount > 0) {
                            // Calculate KR progress based on average Action Item progress (0 or 100)
                            // FIX 2: Added filter(ai -> ai != null) here as well for safety
                            double aiSum = kr.getActionItems().stream()
                                    .filter(ai -> ai != null) // CRASH FIX HERE
                                    .filter(BaseEntity::getIsActive)
                                    .mapToInt(ai -> safeProgress(ai.getProgress()))
                                    .sum();
                            krProgress = (int) Math.min(100, Math.round(aiSum / activeAiCount));
                        } else if (kr.getMetricTarget() != null && kr.getMetricTarget() > 0) {
                            // Calculate KR progress based on metrics

                            // FIX: Provide default values for nullable Double fields before calculation
                            double target = kr.getMetricTarget();
                            double start = Optional.ofNullable(kr.getMetricStart()).orElse(0.0);
                            double current = Optional.ofNullable(kr.getMetricCurrent()).orElse(0.0);

                            double range = target - start;

                            if (range != 0.0) { // Safety check against division by zero
                                double percentage = ((current - start) / range) * 100;
                                krProgress = (int) Math.min(100, Math.max(0, Math.round(percentage)));
                            } else if (current == start) { // This covers the case where start=target, and current is also equal.
                                // Target equals start (e.g., Target=10, Start=10, Current=10). Treat as 0% as this might imply a binary goal that hasn't been met.
                                krProgress = 0;
                            }
                            // Note: If range=0 but current != start, progress should arguably be 100, but the original code didn't handle this fully. Sticking to fixing the NPE.
                        }

                        kr.setProgress(krProgress);
                        objTotal += safeProgress(kr.getProgress());
                        krCount++;
                    }

                    int newObjProgress = (krCount > 0) ? Math.round((float) objTotal / krCount) : safeProgress(obj.getProgress());
                    obj.setProgress(newObjProgress);
                    goalTotal += safeProgress(obj.getProgress());
                    objCount++;
                }

                int newGoalProgress = (objCount > 0) ? Math.round((float) goalTotal / objCount) : safeProgress(goal.getProgress());
                goal.setProgress(newGoalProgress);
                initTotal += safeProgress(goal.getProgress());
                goalCount++;
            }

            int newInitProgress = (goalCount > 0) ? Math.round((float) initTotal / goalCount) : safeProgress(init.getProgress());
            init.setProgress(newInitProgress);
            projTotal += safeProgress(init.getProgress());
            initCount++;
        }

        project.setProgress((initCount > 0) ? Math.round((float) projTotal / initCount) : safeProgress(project.getProgress()));
        projectRepository.save(project);
    }
}