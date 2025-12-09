package com.ccc.okrtracker.controller;

import com.ccc.okrtracker.entity.Role;
import com.ccc.okrtracker.entity.User;
import com.ccc.okrtracker.repository.RoleRepository;
import com.ccc.okrtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // NEW IMPORT
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}