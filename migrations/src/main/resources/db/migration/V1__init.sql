CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE topics
(
    id        SERIAL PRIMARY KEY,
    tenant_id VARCHAR(256)             NOT NULL,
    created   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uuid      uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    name      VARCHAR(1024)             NOT NULL,
    schema    jsonb NOT NULL,
    description text not null,
    owner VARCHAR(256) NOT NULL
);
CREATE UNIQUE INDEX ix_topics_tenant_name on topics(tenant_id, name);


CREATE TABLE subscriptions
(
    id SERIAL primary key,
    tenant_id VARCHAR(256) not null,
    topic_id  int REFERENCES topics (id) ON DELETE CASCADE ON UPDATE CASCADE,
    uuid      uuid         NOT NULL DEFAULT uuid_generate_v4(),
    filters jsonb
);


CREATE TABLE notification_mechanisms (

    id SERIAL PRIMARY KEY,
    tenant_id VARCHAR(256),
    subscription_id  int REFERENCES subscriptions (id) ON DELETE CASCADE ON UPDATE CASCADE,
    created   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uuid      uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    mechanism VARCHAR(256) NOT NULL,
    target VARCHAR(4096) NOT null  -- could be an email, webhook URL, abaco actor ID, or queue name

)

