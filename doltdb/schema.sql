-- Bootstrap schema + seed data for the Dolt-backed `model_configs` table.
-- The `doltdb/` folder itself (the actual Dolt repo state under .dolt/) is gitignored, since it's
-- generated local database state, not source. Run this once against a freshly `dolt init`-ed
-- repo to recreate the same table the webapp expects - see README.md "Setting up the Dolt
-- database" for the full step-by-step.

CREATE TABLE IF NOT EXISTS model_configs (
    id            VARCHAR(100)  NOT NULL PRIMARY KEY,
    provider      VARCHAR(100)  NOT NULL,
    temperature   DOUBLE        NOT NULL DEFAULT 0.7,
    max_tokens    INT           NOT NULL DEFAULT 1024
);

INSERT INTO model_configs (id, provider, temperature, max_tokens) VALUES
    ('gpt-base', 'openai', 0.55, 4024),
    ('gpt-large', 'openai', 0.7, 4096),
    ('claude-opus', 'anthropic', 0.5, 8185)
ON DUPLICATE KEY UPDATE provider = VALUES(provider),
                        temperature = VALUES(temperature),
                        max_tokens = VALUES(max_tokens);
