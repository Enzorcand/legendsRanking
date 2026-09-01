package portfolio.pucrs.common.security;

import org.springframework.security.core.Authentication;

public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static Long id(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
