package com.ccc.okrtracker.controller;

import com.ccc.okrtracker.entity.Role;
import com.ccc.okrtracker.entity.User;
import com.ccc.okrtracker.repository.RoleRepository;
import com.ccc.okrtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public List<User> getUsers() {
        return userRepo.findAll();
    }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return userRepo.save(user);
    }

    // --- Roles ---
    @GetMapping("/roles")
    public List<Role> getRoles() {
        return roleRepo.findAll();
    }

    @PostMapping("/roles")
    public Role createRole(@RequestBody Role role) {
        return roleRepo.save(role);
    }
}