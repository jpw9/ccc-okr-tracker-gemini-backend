package com.ccc.okrtracker.controller;

import com.ccc.okrtracker.entity.Project;
import com.ccc.okrtracker.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ProjectRepository projectRepo;
    // Inject other repos...

    @GetMapping
    @PreAuthorize("hasAnyAuthority('VIEW_STRATEGY', 'MANAGE_STRATEGY')")
    public List<Object> getArchivedItems() {
        // In a real scenario, you might create a specific DTO View or a Custom Query
        // For simplicity, we filter in memory here, but ideally use JPQL "WHERE isActive = false"
        List<Project> archivedProjects = projectRepo.findAll().stream()
                .filter(p -> !p.getIsActive()).collect(Collectors.toList());

        // Combine with other entities...
        return new java.util.ArrayList<>(archivedProjects);
    }

    @PostMapping("/restore/{type}/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STRATEGY')")
    public void restoreItem(@PathVariable String type, @PathVariable Long id) {
        if(type.equals("Project")) {
            Project p = projectRepo.findById(id).orElseThrow();
            p.restore();
            projectRepo.save(p);
        }
        // Handle other types
    }
}