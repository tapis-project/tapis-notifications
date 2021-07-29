CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE topics
(
    id        SERIAL PRIMARY KEY,
    tenant_id VARCHAR(256)             NOT NULL,
    created   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uuid      uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    name      VARCHAR(1024)             NOT NULL,
    description text not null,
    owner VARCHAR(256) NOT NULL
);
CREATE UNIQUE INDEX ix_topics_tenant_name on topics(tenant_id, name);
CREATE INDEX ix_topics_uuid on topics(uuid);
CREATE INDEX ix_topics_description ON topics USING gin(to_tsvector('simple', description));

CREATE TABLE subscriptions
(
    id SERIAL primary key,
    tenant_id VARCHAR(256) not null,
    topic_id  int REFERENCES topics (id) ON DELETE CASCADE ON UPDATE CASCADE,
    uuid      uuid         NOT NULL DEFAULT uuid_generate_v4(),
    filters jsonb NOT NULL  DEFAULT '{}'::jsonb,
    created   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_subscriptions_uuid on subscriptions(uuid);

CREATE TABLE notification_mechanisms (

    id SERIAL PRIMARY KEY,
    tenant_id VARCHAR(256),
    subscription_id  int REFERENCES subscriptions (id) ON DELETE CASCADE ON UPDATE CASCADE,
    created   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uuid      uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    mechanism VARCHAR(256) NOT NULL,
    target VARCHAR(4096) NOT null  -- could be an email, webhook URL, abaco actor ID, or queue name
);
CREATE INDEX ix_mechanisms_tenant_sub_uuid on notification_mechanisms(subscription_id, tenant_id, uuid);

CREATE TABLE queues (
    id SERIAL PRIMARY KEY,
    tenant_id VARCHAR(256) NOT NULL ,
    uuid      uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    name VARCHAR(256) NOT NULL,
    created   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    owner VARCHAR(256) NOT NULL
);
CREATE index ix_queues_uuid on queues(uuid);
CREATE UNIQUE INDEX ix_queues_tenant_name on queues(tenant_id, name);


CREATE TABLE metrics (
    id SERIAL PRIMARY KEY,
    tenant_id VARCHAR(256),
    topic_name VARCHAR(256),
    created   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data jsonb
)



