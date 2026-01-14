package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.*;
import com.ccc.okrtracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing project access control.
 * Handles both direct user-project assignments and role-based project scoping.
 * Uses JdbcTemplate repositories for join table management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectAccessService {

    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final UserProjectRepository userProjectRepo;
    private final RoleProjectRepository roleProjectRepo;

    /**
     * Get all project IDs a user can access based on:
     * 1. Direct assignment (user_projects via JdbcTemplate)
     * 2. Role-based assignment (role_projects via user's roles)
     * 3. VIEW_ALL_PROJECTS permission (bypass)
     * 4. Primary project (backward compatibility)
     */
    public Set<Long> getAccessibleProjectIds(User user) {
        Set<Long> accessibleIds = new HashSet<>();
        
        log.debug("Getting accessible projects for user: {} (ID: {})", user.getEmail(), user.getId());

        // Check for global bypass permission
        boolean hasViewAll = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .anyMatch(p -> "VIEW_ALL_PROJECTS".equals(p));

        if (hasViewAll) {
            log.debug("User has VIEW_ALL_PROJECTS permission - granting access to all projects");
            return projectRepo.findByIsActiveTrue().stream()
                .map(Project::getId)
                .collect(Collectors.toSet());
        }

        // Direct assignments via JdbcTemplate
        List<Long> directAssignments = userProjectRepo.getUserProjectIds(user.getId());
        log.debug("Direct project assignments: {}", directAssignments);
        accessibleIds.addAll(directAssignments);

        // Role-based assignments via JdbcTemplate
        for (Role role : user.getRoles()) {
            List<Long> roleProjects = roleProjectRepo.getRoleScopedProjectIds(role.getId());
            log.debug("Role '{}' (ID: {}) scoped projects: {}", role.getName(), role.getId(), roleProjects);
            // Only add projects that are explicitly scoped to this role
            // Empty list means role has no project restrictions, but doesn't grant global access
            accessibleIds.addAll(roleProjects);
        }

        // Include primary project for backward compatibility
        if (user.getPrimaryProjectId() != null) {
            log.debug("Adding primary project: {}", user.getPrimaryProjectId());
            accessibleIds.add(user.getPrimaryProjectId());
        }

        log.debug("Final accessible project IDs: {}", accessibleIds);
        return accessibleIds;
    }

    /**
     * Check if user can access a specific project.
     */
    public boolean canAccessProject(User user, Long projectId) {
        return getAccessibleProjectIds(user).contains(projectId);
    }

    /**
     * Get user's access level for a specific project.
     * Checks direct assignment first, then role-based access.
     */
    public AccessLevel getAccessLevel(User user, Long projectId) {
        // Check direct assignment first (has explicit access level)
        AccessLevel directLevel = userProjectRepo.getAccessLevel(user.getId(), projectId);
        if (directLevel != null) {
            return directLevel;
        }

        // Check role-based access via JdbcTemplate
        for (Role role : user.getRoles()) {
            List<Long> roleProjects = roleProjectRepo.getRoleScopedProjectIds(role.getId());
            if (roleProjects.isEmpty()) {
                // Global role - viewer access to all
                return AccessLevel.VIEWER;
            }
            if (roleProjects.contains(projectId)) {
                return AccessLevel.MEMBER;
            }
        }

        // Check VIEW_ALL_PROJECTS permission
        boolean hasViewAll = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .anyMatch(p -> "VIEW_ALL_PROJECTS".equals(p));
        if (hasViewAll) {
            return AccessLevel.VIEWER;
        }

        return null; // No access
    }

    /**
     * Assign a user to a project with a specific access level.
     */
    @Transactional
    public void assignUserToProject(Long userId, Long projectId, AccessLevel accessLevel, String assignedBy) {
        // Verify user and project exist
        userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        projectRepo.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        userProjectRepo.assignUserToProject(userId, projectId, accessLevel, assignedBy);
    }

    /**
     * Remove a user's assignment from a project.
     */
    @Transactional
    public void removeUserFromProject(Long userId, Long projectId) {
        userProjectRepo.removeUserFromProject(userId, projectId);
    }

    /**
     * Get all projects assigned to a user (directly via JdbcTemplate).
     */
    public List<Project> getUserProjects(Long userId) {
        List<Long> projectIds = userProjectRepo.getUserProjectIds(userId);
        if (projectIds.isEmpty()) {
            return Collections.emptyList();
        }
        return projectRepo.findByIdInAndIsActiveTrue(projectIds);
    }

    /**
     * Add a project to a role's scope.
     */
    @Transactional
    public void addProjectToRole(Long roleId, Long projectId) {
        // Verify role and project exist
        roleRepo.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        projectRepo.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        roleProjectRepo.addProjectToRole(roleId, projectId);
    }

    /**
     * Remove a project from a role's scope.
     */
    @Transactional
    public void removeProjectFromRole(Long roleId, Long projectId) {
        roleProjectRepo.removeProjectFromRole(roleId, projectId);
    }

    /**
     * Get all projects scoped to a role via JdbcTemplate.
     */
    public List<Project> getRoleScopedProjects(Long roleId) {
        List<Long> projectIds = roleProjectRepo.getRoleScopedProjectIds(roleId);
        if (projectIds.isEmpty()) {
            return Collections.emptyList();
        }
        return projectRepo.findByIdInAndIsActiveTrue(projectIds);
    }

    /**
     * Update all project assignments for a user.
     * This replaces the current assignments with the new list.
     */
    @Transactional
    public void updateUserProjectAssignments(Long userId, Set<Long> projectIds, AccessLevel defaultLevel, String assignedBy) {
        // Verify user exists
        userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Remove all current assignments
        userProjectRepo.removeAllUserAssignments(userId);

        // Add new assignments
        for (Long projectId : projectIds) {
            projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
            userProjectRepo.assignUserToProject(userId, projectId, defaultLevel, assignedBy);
        }
    }

    /**
     * Update all project scoping for a role.
     * This replaces the current scoping with the new list.
     */
    @Transactional
    public void updateRoleProjectScoping(Long roleId, Set<Long> projectIds) {
        log.info("Updating role {} project scoping to: {}", roleId, projectIds);
        
        // Verify role exists
        Role role = roleRepo.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        log.debug("Found role: {}", role.getName());

        // Remove all current scoping
        roleProjectRepo.removeAllRoleProjects(roleId);
        log.debug("Removed all existing project scoping for role {}", roleId);

        // Add new scoping
        for (Long projectId : projectIds) {
            projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
            roleProjectRepo.addProjectToRole(roleId, projectId);
            log.debug("Added project {} to role {}", projectId, roleId);
        }
        
        log.info("Successfully updated role {} with {} scoped projects", roleId, projectIds.size());
    }
}
