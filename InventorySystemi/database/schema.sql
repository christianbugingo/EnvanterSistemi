-- Offline H2 schema is created/upgraded additively by:
-- src/database/DatabaseInitializer.java
-- Launch the application once to initialize it. No server or MySQL is used.
-- Read-only inspection examples (do not expose password hashes):
SELECT table_name FROM information_schema.tables WHERE table_schema='public';
SELECT user_id, username, role, active, must_change_password FROM users;
