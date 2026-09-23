-- Flat, single-table schema for model configurations.
-- This is the source of truth for the DB shape; Hibernate is set to "validate"
-- (see persistence.xml), i.e. it checks the JPA entity matches this table
-- instead of generating/owning the schema itself.
CREATE TABLE IF NOT EXISTS model_configs (
    id            VARCHAR(100)  NOT NULL PRIMARY KEY,
    provider      VARCHAR(100)  NOT NULL,
    temperature   DOUBLE        NOT NULL DEFAULT 0.7,
    max_tokens    INT           NOT NULL DEFAULT 1024
);
