-- Version 1.1: use the application's account management so passwords are hashed.
-- First administrator: login window -> Create first administrator.
-- Staff accounts: admin dashboard -> Staff accounts -> Add staff.
-- Password resets: select staff -> Reset temporary password.
-- Do not insert plaintext demo passwords directly into production databases.

-- Read-only overview (does not expose password hashes):
SELECT user_id, username, role, active, must_change_password FROM users;
