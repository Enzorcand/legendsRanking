package portfolio.pucrs.riot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portfolio.pucrs.riot.entity.RiotAccount;

import java.util.Optional;

public interface RiotAccountRepository extends JpaRepository<RiotAccount, Long> {

    Optional<RiotAccount> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByPuuid(String puuid);
}
