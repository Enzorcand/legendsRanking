package portfolio.pucrs.common.security;

import org.springframework.security.core.Authentication;

import java.util.Optional;

public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static Long id(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /**
     * Safe for endpoints where authentication is optional (e.g. public player profiles):
     * empty for anonymous requests instead of throwing a ClassCastException.
     */
    public static Optional<Long> optionalId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof Long userId ? Optional.of(userId) : Optional.empty();
    }
}
