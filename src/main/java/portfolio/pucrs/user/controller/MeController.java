package portfolio.pucrs.user.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portfolio.pucrs.common.security.AuthenticatedUser;
import portfolio.pucrs.user.dto.MeResponse;
import portfolio.pucrs.user.dto.UpdateMeRequest;
import portfolio.pucrs.user.service.UserService;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public MeResponse me(Authentication authentication) {
        return userService.getMe(AuthenticatedUser.id(authentication));
    }

    @PatchMapping
    public MeResponse updateMe(Authentication authentication, @RequestBody UpdateMeRequest request) {
        return userService.updateMe(AuthenticatedUser.id(authentication), request);
    }

    @PatchMapping("/ranking")
    public MeResponse toggleRankingParticipation(Authentication authentication) {
        return userService.toggleRankingParticipation(AuthenticatedUser.id(authentication));
    }
}
