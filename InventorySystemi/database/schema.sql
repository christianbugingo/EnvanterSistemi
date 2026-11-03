-- Reference schema for a NEW MySQL 8 shop. The Java initializer handles upgrades.
-- No users or passwords are seeded. Back up before upgrading an existing shop.
CREATE DATABASE IF NOT EXISTS inventory_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE inventory_shop;
CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(30) NOT NULL,
  must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS products (
  product_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  category VARCHAR(255) NOT NULL,
  quantity DECIMAL(14,3) NOT NULL DEFAULT 0 CHECK(quantity >= 0),
  price DECIMAL(12,2) NOT NULL CHECK(price >= 0),
  supplier VARCHAR(255) NOT NULL,
  unit VARCHAR(10) NOT NULL DEFAULT 'PIECE',
  low_stock DECIMAL(14,3) NOT NULL DEFAULT 10,
  version INT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS sales (
  sale_id INT AUTO_INCREMENT PRIMARY KEY,
  product_id INT NOT NULL,
  user_id INT NOT NULL,
  quantity_sold DECIMAL(14,3) NOT NULL CHECK(quantity_sold > 0),
  sale_date DATE NOT NULL,
  unit VARCHAR(10) NOT NULL DEFAULT 'PIECE',
  unit_price DECIMAL(12,2),
  product_name VARCHAR(255),
  voided BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sales_product FOREIGN KEY(product_id) REFERENCES products(product_id),
  CONSTRAINT fk_sales_user FOREIGN KEY(user_id) REFERENCES users(user_id)
);
CREATE TABLE IF NOT EXISTS audit_log (
  audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  username VARCHAR(100) NOT NULL,
  action VARCHAR(40) NOT NULL,
  details VARCHAR(2000) NOT NULL,
  workstation VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS app_settings(setting_key VARCHAR(100) PRIMARY KEY,setting_value VARCHAR(255) NOT NULL);
INSERT INTO app_settings(setting_key,setting_value)
SELECT 'bootstrap','1' WHERE NOT EXISTS(SELECT 1 FROM app_settings WHERE setting_key='bootstrap');
