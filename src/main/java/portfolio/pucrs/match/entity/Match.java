package portfolio.pucrs.match.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.season.entity.Season;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "riot_match_id", nullable = false, unique = true, length = 30)
    private String riotMatchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "riot_account_id", nullable = false)
    private RiotAccount riotAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "game_start", nullable = false)
    private Instant gameStart;

    @Column(name = "game_duration", nullable = false)
    private int gameDuration;

    @Column(name = "queue_type", nullable = false, length = 30)
    private String queueType;

    @Column(nullable = false)
    private boolean win;

    @Column(name = "champion_id", nullable = false)
    private int championId;

    @Column(length = 20)
    private String role;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int deaths;

    @Column(nullable = false)
    private int assists;

    @Column(nullable = false)
    private int cs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
