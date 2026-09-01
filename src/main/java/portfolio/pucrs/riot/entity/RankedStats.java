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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import portfolio.pucrs.season.entity.Season;

import java.time.LocalDateTime;

@Entity
@Table(name = "ranked_stats")
@Getter
@Setter
@NoArgsConstructor
public class RankedStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Tier tier;

    @Column(length = 5)
    private String division;

    @Column(name = "league_points")
    private Integer leaguePoints;

    private Integer wins;

    private Integer losses;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
