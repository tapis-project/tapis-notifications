-- Table for tracking sequence IDs associated with a series
CREATE TABLE IF NOT EXISTS series_seq_count
(
    id TEXT PRIMARY KEY,
    seq_count INTEGER NOT NULL DEFAULT 0
);
ALTER TABLE series_seq_count OWNER TO tapis_ntf;