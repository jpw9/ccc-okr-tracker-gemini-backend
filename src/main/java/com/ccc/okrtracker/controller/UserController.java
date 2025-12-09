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

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Endpoint to fetch the application's User entity for the currently authenticated Keycloak user.
     * This is used by the frontend for roles/permissions mapping.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // Only requires successful JWT authentication
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        // The Authentication object principal should be the JWT
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");

            // Look up user by email in the application database
            Optional<User> user = userRepository.findByEmail(email);

            return user.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        return ResponseEntity.badRequest().build();
    }
}