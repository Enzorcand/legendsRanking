package portfolio.pucrs.riot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portfolio.pucrs.riot.entity.PlayerStats;

import java.util.Optional;

public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {

    Optional<PlayerStats> findByRiotAccountId(Long riotAccountId);
}
