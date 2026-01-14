package com.ccc.okrtracker.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Repository for managing role-project scoping.
 * Uses JdbcTemplate for direct join table manipulation.
 */
@Repository
@RequiredArgsConstructor
public class RoleProjectRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Add a project to a role's scope.
     */
    public void addProjectToRole(Long roleId, Long projectId) {
        String sql = """
            INSERT INTO role_projects (role_id, project_id)
            VALUES (?, ?)
            ON CONFLICT (role_id, project_id) DO NOTHING
        """;
        jdbcTemplate.update(sql, roleId, projectId);
    }

    /**
     * Remove a project from a role's scope.
     */
    public void removeProjectFromRole(Long roleId, Long projectId) {
        String sql = "DELETE FROM role_projects WHERE role_id = ? AND project_id = ?";
        jdbcTemplate.update(sql, roleId, projectId);
    }

    /**
     * Get all project IDs scoped to a role.
     */
    public List<Long> getRoleScopedProjectIds(Long roleId) {
        String sql = "SELECT project_id FROM role_projects WHERE role_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, roleId);
    }

    /**
     * Get all role IDs that have a specific project in their scope.
     */
    public List<Long> getRolesForProject(Long projectId) {
        String sql = "SELECT role_id FROM role_projects WHERE project_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, projectId);
    }

    /**
     * Check if a role has a specific project in its scope.
     */
    public boolean isProjectInRoleScope(Long roleId, Long projectId) {
        String sql = "SELECT COUNT(*) FROM role_projects WHERE role_id = ? AND project_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, roleId, projectId);
        return count != null && count > 0;
    }

    /**
     * Remove all project scoping for a specific role.
     */
    public void removeAllRoleProjects(Long roleId) {
        String sql = "DELETE FROM role_projects WHERE role_id = ?";
        jdbcTemplate.update(sql, roleId);
    }

    /**
     * Remove a project from all role scopes.
     */
    public void removeProjectFromAllRoles(Long projectId) {
        String sql = "DELETE FROM role_projects WHERE project_id = ?";
        jdbcTemplate.update(sql, projectId);
    }
}
