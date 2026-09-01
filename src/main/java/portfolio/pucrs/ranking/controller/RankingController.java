package portfolio.pucrs.ranking.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import portfolio.pucrs.ranking.dto.RankingEntryResponse;
import portfolio.pucrs.ranking.dto.RankingFilter;
import portfolio.pucrs.ranking.service.RankingService;
import portfolio.pucrs.riot.entity.Tier;

@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public Page<RankingEntryResponse> getRanking(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Tier tier,
            @RequestParam(required = false) Integer championId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RankingFilter filter = new RankingFilter(search, courseId, role, tier, championId);
        Pageable pageable = PageRequest.of(page, size);
        return rankingService.getRanking(filter, pageable);
    }
}
