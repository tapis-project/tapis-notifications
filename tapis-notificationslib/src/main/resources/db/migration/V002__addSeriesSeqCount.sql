-- Table for tracking sequence IDs associated with a series
CREATE TABLE IF NOT EXISTS notifications_series
(
    tenant TEXT NOT NULL,
    source TEXT NOT NULL,
    subject TEXT NOT NULL,
    series_id TEXT NOT NULL,
    seq_count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant, source, subject, series_id)
);
ALTER TABLE notifications_series OWNER TO tapis_ntf;