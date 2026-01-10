package com.ccc.okrtracker.controller;

import com.ccc.okrtracker.dto.UserDTO;
import com.ccc.okrtracker.entity.Role;
import com.ccc.okrtracker.entity.User;
import com.ccc.okrtracker.repository.RoleRepository;
import com.ccc.okrtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // NEW IMPORT
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;

    // --- Users ---
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('MANAGE_USERS')") // ADDED AUTHORIZATION CHECK
    public List<User> getUsers() {
        return userRepo.findAll();
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('MANAGE_USERS')") // ADDED AUTHORIZATION CHECK
    public User createUser(@RequestBody User user) {
        return userRepo.save(user);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')") // ADDED AUTHORIZATION CHECK
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

                    User savedUser = userRepo.save(existingUser);
                    return ResponseEntity.ok(savedUser);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- Roles ---
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')") // ADDED AUTHORIZATION CHECK
    public List<Role> getRoles() {
        return roleRepo.findAll();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')") // ADDED AUTHORIZATION CHECK
    public Role createRole(@RequestBody Role role) {
        return roleRepo.save(role);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        // Ensure the ID from the path is set on the entity before saving/updating
        role.setId(id);
        return ResponseEntity.ok(roleRepo.save(role));
    }
}