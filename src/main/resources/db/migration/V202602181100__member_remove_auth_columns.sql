-- Option B: login is via AdminUser only; remove redundant auth columns from MEMBERS.
-- Keeps: email (contact), created_at (audit). Drops: password, enabled, failed_login_attempts, locked_at, last_login.
-- Requires MySQL 8.0.23+ for IF EXISTS; otherwise use plain DROP COLUMN.

ALTER TABLE MEMBERS DROP COLUMN IF EXISTS password;
ALTER TABLE MEMBERS DROP COLUMN IF EXISTS enabled;
ALTER TABLE MEMBERS DROP COLUMN IF EXISTS failed_login_attempts;
ALTER TABLE MEMBERS DROP COLUMN IF EXISTS locked_at;
ALTER TABLE MEMBERS DROP COLUMN IF EXISTS last_login;
