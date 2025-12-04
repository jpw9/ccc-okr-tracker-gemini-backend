// File: jpw9/ccc-okr-tracker-gemini-backend/ccc-okr-tracker-gemini-backend-2133aa9e882f23bca9d8c21e07a747afb8685989/src/main/java/com/ccc/okrtracker/config/AuditorAwareImpl.java

package com.ccc.okrtracker.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        // Fetch the currently authenticated principal (username from JWT token)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            // Return a default or empty optional if no user is authenticated (e.g., during startup/unsecured calls)
            return Optional.of("system");
        }

        // For JWT tokens, the principal name is typically the user's login/preferred_username.
        return Optional.of(authentication.getName());
    }
}