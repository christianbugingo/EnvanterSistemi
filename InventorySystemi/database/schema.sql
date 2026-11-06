-- Offline H2 schema and versioned migration entry points:
-- src/database/DatabaseInitializer.java
-- src/database/Migrations.java and src/migrations/V130__offline_shop.sql
-- Migration backups are created next to the database in migration-backups/.
-- Quantity columns: BIGINT pieces for PIECE; BIGINT grams for KG.
-- Money: DECIMAL(19,4), rounded by sale-time currency minor units.
-- Global migration: src/migrations/V130_global__currency_tax_legal.sql
-- Currency, language, tax, legal version/digest/date are local app_settings values.
SELECT version, applied_at FROM schema_migrations;
SELECT user_id, username, role, active, must_change_password FROM users;
