package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private final ProjectRepository projectRepository;

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

                        // KR Logic: If Action Items exist, avg them. Else use Metric.
                        int krProgress = 0;
                        long activeAiCount = kr.getActionItems().stream().filter(BaseEntity::getIsActive).count();

                        if (activeAiCount > 0) {
                            double aiSum = kr.getActionItems().stream()
                                    .filter(BaseEntity::getIsActive)
                                    .mapToInt(ActionItem::getProgress).sum();
                            krProgress = (int) Math.round(aiSum / activeAiCount);
                        } else if (kr.getMetricTarget() != null && kr.getMetricTarget() > 0) {
                            double percentage = ((kr.getMetricCurrent() - kr.getMetricStart())
                                    / (kr.getMetricTarget() - kr.getMetricStart())) * 100;
                            krProgress = (int) Math.min(100, Math.max(0, Math.round(percentage)));
                        }

                        kr.setProgress(krProgress);
                        objTotal += krProgress;
                        krCount++;
                    }

                    int newObjProgress = (krCount > 0) ? Math.round((float) objTotal / krCount) : obj.getProgress();
                    obj.setProgress(newObjProgress);
                    goalTotal += newObjProgress;
                    objCount++;
                }

                int newGoalProgress = (objCount > 0) ? Math.round((float) goalTotal / objCount) : goal.getProgress();
                goal.setProgress(newGoalProgress);
                initTotal += newGoalProgress;
                goalCount++;
            }

            int newInitProgress = (goalCount > 0) ? Math.round((float) initTotal / goalCount) : init.getProgress();
            init.setProgress(newInitProgress);
            projTotal += newInitProgress;
            initCount++;
        }

        project.setProgress((initCount > 0) ? Math.round((float) projTotal / initCount) : project.getProgress());
        projectRepository.save(project);
    }
}