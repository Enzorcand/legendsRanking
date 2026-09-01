package portfolio.pucrs.user.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import portfolio.pucrs.common.security.AuthenticatedUser;
import portfolio.pucrs.match.dto.MatchHistoryEntryResponse;
import portfolio.pucrs.match.service.MatchHistoryService;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.user.service.PlayerProfileService;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerProfileService playerProfileService;
    private final MatchHistoryService matchHistoryService;

    public PlayerController(PlayerProfileService playerProfileService, MatchHistoryService matchHistoryService) {
        this.playerProfileService = playerProfileService;
        this.matchHistoryService = matchHistoryService;
    }

    @GetMapping("/{id}")
    public Object getPlayer(@PathVariable Long id, Authentication authentication) {
        return AuthenticatedUser.optionalId(authentication)
                .<Object>map(viewerId -> playerProfileService.getAuthenticatedProfile(id))
                .orElseGet(() -> playerProfileService.getPublicProfile(id));
    }

    @GetMapping("/{id}/matches")
    public Page<MatchHistoryEntryResponse> getMatchHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RiotAccount riotAccount = playerProfileService.getViewableRiotAccount(id);
        return matchHistoryService.getHistory(riotAccount.getId(), PageRequest.of(page, size));
    }
}
