package portfolio.pucrs.riot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.riot.entity.Tier;

import java.util.List;
import java.util.Optional;

public interface RiotAccountRepository extends JpaRepository<RiotAccount, Long> {

    Optional<RiotAccount> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByPuuid(String puuid);

    @Query("""
            SELECT ra FROM RiotAccount ra
            JOIN FETCH ra.user u
            JOIN FETCH u.course c
            JOIN FETCH ra.rankedStats rs
            LEFT JOIN FETCH ra.playerStats ps
            WHERE u.rankingEligible = true
              AND u.active = true
              AND u.emailVerified = true
              AND LOWER(ra.gameName) LIKE LOWER(:searchPattern)
              AND (:courseId IS NULL OR c.id = :courseId)
              AND (:tier IS NULL OR rs.tier = :tier)
              AND (:role IS NULL OR ps.primaryRole = :role)
              AND (:championId IS NULL OR ps.mostPlayedChampionId = :championId)
            """)
    List<RiotAccount> findRankingCandidates(
            @Param("searchPattern") String searchPattern,
            @Param("courseId") Long courseId,
            @Param("tier") Tier tier,
            @Param("role") String role,
            @Param("championId") Integer championId);
}
