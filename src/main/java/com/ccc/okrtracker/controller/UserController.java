package com.ccc.okrtracker.controller;

import com.ccc.okrtracker.entity.User;
import com.ccc.okrtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Endpoint to fetch the application's User entity for the currently authenticated Keycloak user.
     * Used by the frontend for roles/permissions mapping.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // Only requires successful JWT authentication
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");

            Optional<User> user = userRepository.findByEmail(email);

            return user.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        return ResponseEntity.badRequest().build();
    }

    /**
     * Endpoint to fetch all active users. Used for assignment dropdowns in hierarchy views.
     * Requires only basic authentication, not MANAGE_USERS permission.
     */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public List<User> getAllUsersForAssignment() {
        return userRepository.findAll().stream()
                .filter(User::getIsActive)
                .collect(Collectors.toList());
    }
}