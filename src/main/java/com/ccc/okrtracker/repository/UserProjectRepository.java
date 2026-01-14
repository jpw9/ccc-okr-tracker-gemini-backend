package com.ccc.okrtracker.repository;

import com.ccc.okrtracker.entity.AccessLevel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Repository for managing user-project assignments with access levels.
 * Uses JdbcTemplate for direct table manipulation since the join table
 * has additional columns (access_level, assigned_date, assigned_by).
 */
@Repository
@RequiredArgsConstructor
public class UserProjectRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Assign a user to a project with a specific access level.
     */
    public void assignUserToProject(Long userId, Long projectId, AccessLevel accessLevel, String assignedBy) {
        String sql = """
            INSERT INTO user_projects (user_id, project_id, access_level, assigned_by, assigned_date)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, project_id) DO UPDATE SET 
                access_level = EXCLUDED.access_level,
                assigned_by = EXCLUDED.assigned_by,
                assigned_date = CURRENT_TIMESTAMP
        """;
        jdbcTemplate.update(sql, userId, projectId, accessLevel.name(), assignedBy);
    }

    /**
     * Remove a user's assignment from a project.
     */
    public void removeUserFromProject(Long userId, Long projectId) {
        String sql = "DELETE FROM user_projects WHERE user_id = ? AND project_id = ?";
        jdbcTemplate.update(sql, userId, projectId);
    }

    /**
     * Get the access level for a user on a specific project.
     * Returns null if the user is not directly assigned.
     */
    public AccessLevel getAccessLevel(Long userId, Long projectId) {
        String sql = "SELECT access_level FROM user_projects WHERE user_id = ? AND project_id = ?";
        List<String> results = jdbcTemplate.queryForList(sql, String.class, userId, projectId);
        if (results.isEmpty()) {
            return null;
        }
        return AccessLevel.valueOf(results.get(0));
    }

    /**
     * Get all project assignments for a user with their access levels.
     */
    public List<Map<String, Object>> getUserProjectAssignments(Long userId) {
        String sql = """
            SELECT project_id, access_level, assigned_date, assigned_by 
            FROM user_projects 
            WHERE user_id = ?
        """;
        return jdbcTemplate.queryForList(sql, userId);
    }

    /**
     * Get all users assigned to a project with their access levels.
     */
    public List<Map<String, Object>> getProjectUserAssignments(Long projectId) {
        String sql = """
            SELECT user_id, access_level, assigned_date, assigned_by 
            FROM user_projects 
            WHERE project_id = ?
        """;
        return jdbcTemplate.queryForList(sql, projectId);
    }

    /**
     * Check if a user is directly assigned to a project.
     */
    public boolean isUserAssignedToProject(Long userId, Long projectId) {
        String sql = "SELECT COUNT(*) FROM user_projects WHERE user_id = ? AND project_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, projectId);
        return count != null && count > 0;
    }

    /**
     * Remove all assignments for a specific user.
     */
    public void removeAllUserAssignments(Long userId) {
        String sql = "DELETE FROM user_projects WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    /**
     * Remove all assignments for a specific project.
     */
    public void removeAllProjectAssignments(Long projectId) {
        String sql = "DELETE FROM user_projects WHERE project_id = ?";
        jdbcTemplate.update(sql, projectId);
    }

    /**
     * Get all project IDs assigned to a user.
     */
    public List<Long> getUserProjectIds(Long userId) {
        String sql = "SELECT project_id FROM user_projects WHERE user_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }
}
