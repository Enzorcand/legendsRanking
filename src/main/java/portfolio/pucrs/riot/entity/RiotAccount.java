package portfolio.pucrs.riot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import portfolio.pucrs.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "riot_accounts")
@Getter
@Setter
@NoArgsConstructor
public class RiotAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne
    @JoinColumn(name = "ranked_stats_id")
    private RankedStats rankedStats;

    @OneToOne(mappedBy = "riotAccount", fetch = FetchType.LAZY)
    private PlayerStats playerStats;

    @Column(unique = true, length = 100)
    private String puuid;

    @Column(name = "game_name", length = 100)
    private String gameName;

    @Column(name = "tag_line", length = 10)
    private String tagLine;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Region region;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
