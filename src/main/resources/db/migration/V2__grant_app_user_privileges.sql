-- Flyway migration to (re)grant DML privileges to the application user.
-- This is intentionally idempotent and safe to run even if privileges already exist.

GRANT SELECT, INSERT, UPDATE, DELETE ON app_file TO ${appUser};

