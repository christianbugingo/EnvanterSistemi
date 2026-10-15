-- Safe, additive initialization for a new InventorySystem database.
-- CREATE TABLE IF NOT EXISTS leaves existing installations and rows untouched.
CREATE DATABASE IF NOT EXISTS InventorySystem CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE InventorySystem;

CREATE TABLE IF NOT EXISTS users (
  user_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
  product_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  category VARCHAR(255) NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  price DECIMAL(12,2) NOT NULL,
  supplier VARCHAR(255) NOT NULL,
  CHECK (quantity >= 0),
  CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS sales (
  sale_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id INT NOT NULL,
  user_id INT NOT NULL,
  quantity_sold INT NOT NULL,
  sale_date DATE NOT NULL,
  CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES products(product_id),
  CONSTRAINT fk_sales_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CHECK (quantity_sold > 0)
);
