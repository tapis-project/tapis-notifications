-- Table for tracking sequence IDs associated with an event series
CREATE TABLE IF NOT EXISTS event_series
(
    tenant TEXT NOT NULL,
    source TEXT NOT NULL,
    subject TEXT NOT NULL,
    series_id TEXT NOT NULL,
    seq_count BIGINT NOT NULL DEFAULT 0,
    created    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    PRIMARY KEY (tenant, source, subject, series_id)
);
ALTER TABLE event_series OWNER TO tapis_ntf;