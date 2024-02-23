-- Table for tracking sequence IDs associated with a series
CREATE TABLE IF NOT EXISTS series_seq_count
(
    tenant TEXT NOT NULL,
    source TEXT NOT NULL,
    subject TEXT NOT NULL,
    seriesId TEXT NOT NULL,
    seq_count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant, source, subject, seriesId)
);
ALTER TABLE series_seq_count OWNER TO tapis_ntf;