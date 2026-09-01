package portfolio.pucrs.match.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import portfolio.pucrs.match.entity.Match;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    boolean existsByRiotMatchId(String riotMatchId);

    List<Match> findAllByRiotAccountIdAndSeasonId(Long riotAccountId, Long seasonId);

    Page<Match> findAllByRiotAccountIdOrderByGameStartDesc(Long riotAccountId, Pageable pageable);
}
