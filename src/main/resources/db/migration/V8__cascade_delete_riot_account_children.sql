ALTER TABLE matches
    DROP CONSTRAINT matches_riot_account_id_fkey,
    ADD CONSTRAINT matches_riot_account_id_fkey
        FOREIGN KEY (riot_account_id) REFERENCES riot_accounts (id) ON DELETE CASCADE;

ALTER TABLE player_stats
    DROP CONSTRAINT player_stats_riot_account_id_fkey,
    ADD CONSTRAINT player_stats_riot_account_id_fkey
        FOREIGN KEY (riot_account_id) REFERENCES riot_accounts (id) ON DELETE CASCADE;
