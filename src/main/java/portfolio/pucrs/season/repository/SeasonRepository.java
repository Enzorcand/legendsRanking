package portfolio.pucrs.season.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portfolio.pucrs.season.entity.Season;

import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    Optional<Season> findByActiveTrue();
}
