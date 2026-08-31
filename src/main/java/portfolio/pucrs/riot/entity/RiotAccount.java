package portfolio.pucrs.riot.entity;


import jakarta.persistence.*;
import lombok.Data;
import portfolio.pucrs.user.entity.User;

@Entity
@Table(name = "riot_accounts")
@Data
public class RiotAccount {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne
    @JoinColumn(name = "ranked_stats_id")
    private RankedStats ranked_stats;

    private String puuid;

    private String game_name;

    private String tag_line;

    @Enumerated(EnumType.STRING)
    private Region region;
}
