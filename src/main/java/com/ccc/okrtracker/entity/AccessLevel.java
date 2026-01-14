package com.ccc.okrtracker.entity;

/**
 * Defines access levels for user-project assignments.
 * Used to control what actions a user can perform within a project.
 */
public enum AccessLevel {
    /**
     * Full control over the project, including deletion and member management.
     */
    OWNER,
    
    /**
     * Can manage hierarchy within the project (create, update, delete items).
     */
    MANAGER,
    
    /**
     * Can edit items assigned to them within the project.
     */
    MEMBER,
    
    /**
     * Read-only access to view project hierarchy.
     */
    VIEWER
}
