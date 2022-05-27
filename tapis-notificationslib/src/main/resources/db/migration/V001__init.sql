-- Initial DB schema creation for Tapis Notifications Service
-- postgres commands to create all tables, indices and other database artifacts required.
-- Prerequisites:
-- Create DB named tapisntfdb and user named tapis_ntf
--   CREATE DATABASE tapisntfdb ENCODING='UTF8' LC_COLLATE='en_US.utf8' LC_CTYPE='en_US.utf8';
--   CREATE USER tapis_ntf WITH ENCRYPTED PASSWORD '<password>';
--   GRANT ALL PRIVILEGES ON DATABASE tapisntfdb TO tapis_ntf;
-- Fast way to check for table:
--   SELECT to_regclass('tapis_ntf.subscriptions');
--
--
-- TIMEZONE Convention
----------------------
-- All tables conform to the same timezone usage rule:
--
--   All dates, times and timestamps are stored as UTC WITHOUT TIMEZONE information.
--
-- All temporal values written to the database are required to be UTC, all temporal
-- values read from the database can be assumed to be UTC.

-- NOTES for jOOQ
--   When a POJO has a default constructor (which is needed for jersey's SelectableEntityFilteringFeature)
--     then column names must match POJO attributes (with convention an_attr -> anAttr)
--     in order for jOOQ to set the attribute during Record.into()
--     Possibly another option would be to create a custom mapper to be used by Record.into()
--
-- Create the schema and set the search path
CREATE SCHEMA IF NOT EXISTS tapis_ntf AUTHORIZATION tapis_ntf;
ALTER ROLE tapis_ntf SET search_path = 'tapis_ntf';
SET search_path TO tapis_ntf;

-- Set permissions
-- GRANT CONNECT ON DATABASE tapisntfdb TO tapis_ntf;
-- GRANT USAGE ON SCHEMA tapis_ntf TO tapis_ntf;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA tapis_ntf TO tapis_ntf;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA tapis_ntf TO tapis_ntf;

-- ----------------------------------------------------------------------------------------
--    Subscriptions
-- ----------------------------------------------------------------------------------------
-- Subscriptions table
-- A subscription is a request to receive notification of events.
CREATE TABLE subscriptions
(
    seq_id  SERIAL PRIMARY KEY,
    tenant  TEXT NOT NULL,
    owner   TEXT NOT NULL,
    name      TEXT NOT NULL,
    description TEXT,
    enabled  BOOLEAN NOT NULL DEFAULT true,
    type_filter TEXT NOT NULL,
    type_filter1 TEXT NOT NULL,
    type_filter2 TEXT NOT NULL,
    type_filter3 TEXT NOT NULL,
    subject_filter TEXT NOT NULL,
    delivery_targets JSONB NOT NULL,
    ttlMinutes INTEGER NOT NULL DEFAULT -1,
    uuid  UUID NOT NULL,
    expiry  TIMESTAMP WITHOUT TIME ZONE,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    UNIQUE (tenant,owner,name)
);
ALTER TABLE subscriptions OWNER TO tapis_ntf;
COMMENT ON COLUMN subscriptions.seq_id IS 'Subscription sequence id';
COMMENT ON COLUMN subscriptions.tenant IS 'Tenant name associated with the subscription';
COMMENT ON COLUMN subscriptions.owner IS 'User name of owner';
COMMENT ON COLUMN subscriptions.name IS 'Name for the subscription. tenant+owner+name must be unique';
COMMENT ON COLUMN subscriptions.enabled IS 'Indicates if subscription is currently active and available for use';
COMMENT ON COLUMN subscriptions.created IS 'UTC time for when record was created';
COMMENT ON COLUMN subscriptions.updated IS 'UTC time for when record was last updated';

-- Notifications table
-- In-flight notifications. A notification represents an event being delivered to a subscriber
-- Note that event_uuid is needed to find all notifications associated with a specific event.
CREATE TABLE notifications
(
    seq_id SERIAL PRIMARY KEY,
    subscr_seq_id INTEGER REFERENCES subscriptions(seq_id) ON DELETE CASCADE,
    uuid UUID NOT NULL,
    tenant TEXT NOT NULL,
    subscr_name TEXT NOT NULL,
    delivery_target JSONB NOT NULL,
    event_uuid UUID NOT NULL,
    event JSONB NOT NULL,
    bucket_number INTEGER NOT NULL DEFAULT 0,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
ALTER TABLE notifications OWNER TO tapis_ntf;

-- Notifications recovery table
-- In-flight notifications that have failed and are in recovery.
CREATE TABLE notifications_recovery
(
    seq_id SERIAL PRIMARY KEY,
    subscr_seq_id INTEGER REFERENCES subscriptions(seq_id) ON DELETE CASCADE,
    uuid UUID NOT NULL,
    tenant TEXT NOT NULL,
    subscr_name TEXT NOT NULL,
    delivery_target JSONB NOT NULL,
    event_uuid UUID NOT NULL,
    event JSONB NOT NULL,
    bucket_number INTEGER NOT NULL DEFAULT 0,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
ALTER TABLE notifications_recovery OWNER TO tapis_ntf;

-- Notifications last_event
-- Record of last event that a delivery bucket manager was processing. To avoid re-processing after a crash
--   Very few rows expected, one per bucket, i.e. one per delivery bucket manager.
CREATE TABLE notifications_last_event
(
    bucket_number int PRIMARY KEY,
    event_uuid UUID NOT NULL
);
ALTER TABLE notifications_last_event OWNER TO tapis_ntf;

-- Notifications test sequences
-- For recording of notifications received as part of a test sequence
CREATE TABLE notifications_tests
(
    seq_id SERIAL PRIMARY KEY,
    subscr_seq_id INTEGER REFERENCES subscriptions(seq_id) ON DELETE CASCADE,
    tenant TEXT NOT NULL,
    owner  TEXT NOT NULL,
    subscr_name TEXT NOT NULL,
    notification_count INTEGER NOT NULL DEFAULT 0,
    notifications JSONB NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    UNIQUE (tenant,owner,subscr_name)
);
ALTER TABLE notifications_tests OWNER TO tapis_ntf;

