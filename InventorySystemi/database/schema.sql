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
-- FIFO migration: src/migrations/V130_batches__fifo.sql
-- stock_batches: quantity_received/remaining_quantity are pieces or grams.
-- Nullable total_cost/remaining_cost/cost_per_unit mean cost not recorded.
-- Optional selling_price applies only to this batch and its original currency.
-- sale_batch_allocations preserves original batch quantity/cost per sale line.
-- sales.cost_complete=false excludes the line from the known-cost profit cohort.
-- app_settings.staff_profit_visible defaults false; profit remains user-scoped.
SELECT version FROM schema_migrations WHERE version='1.3.0-batches';
