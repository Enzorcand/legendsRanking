package portfolio.pucrs.riot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portfolio.pucrs.common.security.AuthenticatedUser;
import portfolio.pucrs.riot.dto.LinkRiotAccountRequest;
import portfolio.pucrs.riot.dto.RiotAccountResponse;
import portfolio.pucrs.riot.service.RiotAccountService;

@RestController
@RequestMapping("/api/me/riot-account")
public class RiotAccountController {

    private final RiotAccountService riotAccountService;

    public RiotAccountController(RiotAccountService riotAccountService) {
        this.riotAccountService = riotAccountService;
    }

    @PostMapping("/link")
    public ResponseEntity<RiotAccountResponse> link(Authentication authentication, @RequestBody LinkRiotAccountRequest request) {
        RiotAccountResponse response = riotAccountService.linkAccount(AuthenticatedUser.id(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/unlink")
    public ResponseEntity<Void> unlink(Authentication authentication) {
        riotAccountService.unlinkAccount(AuthenticatedUser.id(authentication));
        return ResponseEntity.noContent().build();
    }
}
