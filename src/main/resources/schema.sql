-- Flat, single-table schema for model configurations.
-- This is the source of truth for the DB shape, loaded via H2's INIT=RUNSCRIPT on connect
-- (see ExposedModelConfigRepository / ExposedDaoModelConfigRepository).
CREATE TABLE IF NOT EXISTS model_configs (
    id            VARCHAR(100)  NOT NULL PRIMARY KEY,
    provider      VARCHAR(100)  NOT NULL,
    temperature   DOUBLE        NOT NULL DEFAULT 0.7,
    max_tokens    INT           NOT NULL DEFAULT 1024
);
