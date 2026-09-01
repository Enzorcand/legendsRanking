ALTER TABLE ranked_stats
    ADD COLUMN season_id BIGINT REFERENCES seasons (id),
    ADD COLUMN tier VARCHAR(20),
    ADD COLUMN division VARCHAR(5),
    ADD COLUMN league_points INT,
    ADD COLUMN wins INT,
    ADD COLUMN losses INT,
    ADD COLUMN updated_at TIMESTAMP;
