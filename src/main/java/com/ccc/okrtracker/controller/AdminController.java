package com.ccc.okrtracker.controller;

import com.ccc.okrtracker.dto.UserDTO;
import com.ccc.okrtracker.entity.AccessLevel;
import com.ccc.okrtracker.entity.Project;
import com.ccc.okrtracker.entity.Role;
import com.ccc.okrtracker.entity.User;
import com.ccc.okrtracker.repository.RoleProjectRepository;
import com.ccc.okrtracker.repository.RoleRepository;
import com.ccc.okrtracker.repository.UserProjectRepository;
import com.ccc.okrtracker.repository.UserRepository;
import com.ccc.okrtracker.service.ProjectAccessService;
import com.ccc.okrtracker.service.HierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final RoleProjectRepository roleProjectRepo;
    private final UserProjectRepository userProjectRepo;
    private final ProjectAccessService projectAccessService;
    private final HierarchyService hierarchyService;

    // Helper to get current user's email for audit
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }
        return "system";
    }

    // --- Users ---
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public List<User> getUsers() {
        List<User> users = userRepo.findAll();
        // Populate assignedProjectIds for each user
        for (User user : users) {
            List<Long> projectIds = userProjectRepo.getUserProjectIds(user.getId());
            user.setAssignedProjectIds(new HashSet<>(projectIds));
        }
        return users;
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public User createUser(@RequestBody User user) {
        return userRepo.save(user);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        return userRepo.findById(id)
                .map(existingUser -> {
                    // Update only mutable fields
                    existingUser.setFirstName(userDTO.getFirstName());
                    existingUser.setLastName(userDTO.getLastName());
                    existingUser.setEmail(userDTO.getEmail());
                    existingUser.setLogin(userDTO.getLogin());
                    existingUser.setGroupNo(userDTO.getGroupNo());
                    existingUser.setPrimaryProjectId(userDTO.getPrimaryProjectId());
                    
                    // Handle roles update using roleIds from DTO
                    if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
                        Set<Role> updatedRoles = new HashSet<>(roleRepo.findAllById(userDTO.getRoleIds()));
                        existingUser.setRoles(updatedRoles);
                    }

                    // Handle assigned projects update
                    if (userDTO.getAssignedProjectIds() != null) {
                        projectAccessService.updateUserProjectAssignments(
                            id, 
                            userDTO.getAssignedProjectIds(), 
                            AccessLevel.MEMBER, 
                            getCurrentUserEmail()
                        );
                    }

                    User savedUser = userRepo.save(existingUser);
                    // Populate assignedProjectIds in the response
                    List<Long> projectIds = userProjectRepo.getUserProjectIds(savedUser.getId());
                    savedUser.setAssignedProjectIds(new HashSet<>(projectIds));
                    return ResponseEntity.ok(savedUser);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- User Project Assignment Endpoints ---

    @PostMapping("/users/{userId}/projects/{projectId}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Void> assignUserToProject(
            @PathVariable Long userId,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "MEMBER") String accessLevel
    ) {
        projectAccessService.assignUserToProject(
            userId, 
            projectId, 
            AccessLevel.valueOf(accessLevel), 
            getCurrentUserEmail()
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}/projects/{projectId}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Void> removeUserFromProject(
            @PathVariable Long userId,
            @PathVariable Long projectId
    ) {
        projectAccessService.removeUserFromProject(userId, projectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}/projects")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<List<Project>> getUserProjects(@PathVariable Long userId) {
        return ResponseEntity.ok(projectAccessService.getUserProjects(userId));
    }

    // --- Roles ---
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public List<Role> getRoles() {
        List<Role> roles = roleRepo.findAll();
        // Populate scopedProjectIds for each role
        for (Role role : roles) {
            List<Long> projectIds = roleProjectRepo.getRoleScopedProjectIds(role.getId());
            role.setScopedProjectIds(new HashSet<>(projectIds));
        }
        return roles;
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public Role createRole(@RequestBody Role role) {
        return roleRepo.save(role);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        // Ensure the ID from the path is set on the entity before saving/updating
        role.setId(id);
        
        // Handle scoped projects if provided
        if (role.getScopedProjectIds() != null) {
            projectAccessService.updateRoleProjectScoping(id, role.getScopedProjectIds());
        }
        
        Role savedRole = roleRepo.save(role);
        // Populate scopedProjectIds in the response
        List<Long> projectIds = roleProjectRepo.getRoleScopedProjectIds(savedRole.getId());
        savedRole.setScopedProjectIds(new HashSet<>(projectIds));
        
        return ResponseEntity.ok(savedRole);
    }

    // --- Role Project Scoping Endpoints ---

    @PostMapping("/roles/{roleId}/projects/{projectId}")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<Void> addProjectToRole(
            @PathVariable Long roleId,
            @PathVariable Long projectId
    ) {
        projectAccessService.addProjectToRole(roleId, projectId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/roles/{roleId}/projects/{projectId}")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<Void> removeProjectFromRole(
            @PathVariable Long roleId,
            @PathVariable Long projectId
    ) {
        projectAccessService.removeProjectFromRole(roleId, projectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/roles/{roleId}/projects")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<List<Project>> getRoleScopedProjects(@PathVariable Long roleId) {
        return ResponseEntity.ok(projectAccessService.getRoleScopedProjects(roleId));
    }

    // --- All Projects (for admin assignment UI) ---
    @GetMapping("/projects")
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'MANAGE_ROLES')")
    public ResponseEntity<List<Project>> getAllProjectsForAdmin() {
        return ResponseEntity.ok(hierarchyService.getAllProjectsUnfiltered());
    }
}