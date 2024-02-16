-- Table for tracking sequence IDs associated with a series
CREATE TABLE IF NOT EXISTS series_seq_id
(
    series_id TEXT PRIMARY KEY,
    series_seq_id INTEGER NOT NULL DEFAULT 0
);
ALTER TABLE series_seq_id OWNER TO tapis_ntf;