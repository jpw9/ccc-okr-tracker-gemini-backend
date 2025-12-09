package com.ccc.okrtracker.service;

import com.ccc.okrtracker.entity.Role;
import com.ccc.okrtracker.entity.User;
import com.ccc.okrtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Looks up the application User by the 'email' claim from the JWT and extracts application permissions.
     * Permissions are formatted as "PERMISSION_NAME" (e.g., "MANAGE_USERS") for Spring Security.
     * * @param jwt The authenticated JWT.
     * @return A collection of Spring Security GrantedAuthorities derived from the application user's roles.
     */
    public Collection<GrantedAuthority> mapJwtToAuthorities(Jwt jwt) {
        String email = jwt.getClaimAsString("email");

        if (email == null) {
            return Collections.emptyList();
        }

        // 1. Find the active application user by email
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent() && userOptional.get().getIsActive()) {
            User user = userOptional.get();

            // 2. Extract all unique active permissions from all assigned roles
            Set<String> appPermissions = user.getRoles().stream()
                    .filter(Role::getIsActive)
                    .flatMap(role -> role.getPermissions().stream())
                    .collect(Collectors.toSet());

            // 3. Convert to Spring Security Authorities
            return appPermissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        }

        // If user is not found or is inactive, return no authorities
        return Collections.emptyList();
    }
}