package portfolio.pucrs.match.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portfolio.pucrs.match.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {

    boolean existsByRiotMatchId(String riotMatchId);
}
