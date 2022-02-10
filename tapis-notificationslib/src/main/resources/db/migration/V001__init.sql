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
CREATE TABLE subscriptions
(
    seq_id  SERIAL PRIMARY KEY,
    tenant  TEXT NOT NULL,
    id      TEXT NOT NULL,
    description TEXT,
    owner   TEXT NOT NULL,
    enabled  BOOLEAN NOT NULL DEFAULT true,
    type_filter TEXT NOT NULL,
    subject_filter TEXT,
    delivery_methods JSONB NOT NULL,
    ttl INTEGER NOT NULL DEFAULT -1,
    notes JSONB NOT NULL,
    uuid  uuid NOT NULL,
    expiry  TIMESTAMP WITHOUT TIME ZONE,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    UNIQUE (tenant,id)
);
ALTER TABLE subscriptions OWNER TO tapis_ntf;
CREATE INDEX subscriptions_tenant_id_idx ON subscriptions (tenant, id);
COMMENT ON COLUMN subscriptions.seq_id IS 'Subscription sequence id';
COMMENT ON COLUMN subscriptions.tenant IS 'Tenant name associated with the subscription';
COMMENT ON COLUMN subscriptions.id IS 'Unique name for the subscription';
COMMENT ON COLUMN subscriptions.owner IS 'User name of owner';
COMMENT ON COLUMN subscriptions.enabled IS 'Indicates if subscription is currently active and available for use';
COMMENT ON COLUMN subscriptions.created IS 'UTC time for when record was created';
COMMENT ON COLUMN subscriptions.updated IS 'UTC time for when record was last updated';

-- Subscription updates table
-- Track update requests for subscriptions
CREATE TABLE subscription_updates
(
    seq_id SERIAL PRIMARY KEY,
    subscription_seq_id INTEGER REFERENCES subscriptions(seq_id) ON DELETE CASCADE,
    subscription_tenant TEXT NOT NULL,
    subscription_id TEXT NOT NULL,
    user_tenant TEXT NOT NULL,
    user_name TEXT NOT NULL,
    operation TEXT NOT NULL,
    upd_json JSONB NOT NULL,
    upd_text TEXT,
    uuid uuid NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
ALTER TABLE subscription_updates OWNER TO tapis_ntf;