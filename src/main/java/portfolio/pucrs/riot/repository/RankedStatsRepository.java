package portfolio.pucrs.riot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portfolio.pucrs.riot.entity.RankedStats;

public interface RankedStatsRepository extends JpaRepository<RankedStats, Long> {
}
