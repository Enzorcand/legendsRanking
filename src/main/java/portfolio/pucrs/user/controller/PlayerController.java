package portfolio.pucrs.user.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portfolio.pucrs.common.security.AuthenticatedUser;
import portfolio.pucrs.user.service.PlayerProfileService;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerProfileService playerProfileService;

    public PlayerController(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    @GetMapping("/{id}")
    public Object getPlayer(@PathVariable Long id, Authentication authentication) {
        return AuthenticatedUser.optionalId(authentication)
                .<Object>map(viewerId -> playerProfileService.getAuthenticatedProfile(id))
                .orElseGet(() -> playerProfileService.getPublicProfile(id));
    }
}
