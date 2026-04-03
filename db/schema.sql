-- NexBuy schema (MySQL 8)
-- Charset / engine
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

CREATE DATABASE IF NOT EXISTS nexbuy_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nexbuy_db;

-- USERS
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(191) NOT NULL UNIQUE,
  phone VARCHAR(32) NULL,
  password_hash VARCHAR(255) NOT NULL,
  status ENUM('active','inactive','blocked') NOT NULL DEFAULT 'active',
  role ENUM('user','admin') NOT NULL DEFAULT 'user',
  oauth_provider VARCHAR(64) NULL,
  oauth_sub VARCHAR(191) NULL,
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_profiles (
  user_id BIGINT PRIMARY KEY,
  first_name VARCHAR(80),
  last_name VARCHAR(80),
  avatar_url VARCHAR(255),
  dob DATE NULL,
  gender VARCHAR(24) NULL,
  language_pref VARCHAR(10) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS addresses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  label VARCHAR(80) NULL,
  line1 VARCHAR(191) NOT NULL,
  line2 VARCHAR(191) NULL,
  city VARCHAR(120) NOT NULL,
  state VARCHAR(120) NOT NULL,
  postal_code VARCHAR(20) NOT NULL,
  country VARCHAR(80) NOT NULL,
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_addr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS otps ( 
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  code VARCHAR(10) NOT NULL,
  purpose ENUM('register','reset','login') NOT NULL,
  expires_at DATETIME NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pending_registrations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(191) NOT NULL UNIQUE,
  phone VARCHAR(32) NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(80) NULL,
  last_name VARCHAR(80) NULL,
  line1 VARCHAR(191) NULL,
  city VARCHAR(120) NULL,
  state VARCHAR(120) NULL,
  postal_code VARCHAR(20) NULL,
  country VARCHAR(80) NULL,
  otp_code VARCHAR(10) NOT NULL,
  otp_expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  expires_at DATETIME NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- CATALOG
CREATE TABLE IF NOT EXISTS categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NULL,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(150) NOT NULL UNIQUE,
  description TEXT NULL,
  image_url VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_cat_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS brands (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  slug VARCHAR(150) NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  brand_id BIGINT NULL,
  title VARCHAR(180) NOT NULL,
  slug VARCHAR(200) NOT NULL UNIQUE,
  description TEXT NULL,
  cover_image VARCHAR(255) NULL,
  status ENUM('active','inactive') NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES brands(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_variants (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  sku VARCHAR(120) NOT NULL UNIQUE,
  name VARCHAR(180) NOT NULL,
  price_cents INT NOT NULL,
  compare_at_cents INT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'INR',
  attributes JSON NULL,
  weight_grams INT NULL,
  dimensions JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inventory (
  variant_id BIGINT PRIMARY KEY,
  stock_qty INT NOT NULL DEFAULT 0,
  low_stock_threshold INT DEFAULT 5,
  is_backorder_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_inventory_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_media (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  url VARCHAR(255) NOT NULL,
  alt_text VARCHAR(255) NULL,
  sort_order INT DEFAULT 0,
  CONSTRAINT fk_media_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_tags (
  product_id BIGINT NOT NULL,
  tag VARCHAR(80) NOT NULL,
  PRIMARY KEY(product_id, tag),
  CONSTRAINT fk_tag_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- CART & ORDERS
CREATE TABLE IF NOT EXISTS carts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  status ENUM('active','converted','expired') NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cart_id BIGINT NOT NULL,
  variant_id BIGINT NOT NULL,
  qty INT NOT NULL,
  price_cents_snapshot INT NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'INR',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  cart_id BIGINT NULL,
  order_number VARCHAR(40) NOT NULL UNIQUE,
  status ENUM('pending','paid','failed','shipped','delivered','cancelled') NOT NULL DEFAULT 'pending',
  subtotal_cents INT NOT NULL,
  tax_cents INT NOT NULL DEFAULT 0,
  shipping_cents INT NOT NULL DEFAULT 0,
  discount_cents INT NOT NULL DEFAULT 0,
  total_cents INT NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'INR',
  payment_status ENUM('initiated','success','failed','refund_pending','refunded','cancelled') NOT NULL DEFAULT 'initiated',
  payment_ref VARCHAR(120) NULL,
  shipping_address_snapshot JSON NOT NULL,
  billing_address_snapshot JSON NOT NULL,
  placed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_order_cart FOREIGN KEY (cart_id) REFERENCES carts(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  variant_id BIGINT NOT NULL,
  title_snapshot VARCHAR(191) NOT NULL,
  sku_snapshot VARCHAR(120) NOT NULL,
  attributes_snapshot JSON NULL,
  unit_price_cents INT NOT NULL,
  qty INT NOT NULL,
  line_total_cents INT NOT NULL,
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  provider ENUM('razorpay','stripe','cod') NOT NULL,
  amount_cents INT NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'INR',
  status ENUM('created','authorized','captured','failed','refund_pending','refunded','cancelled') NOT NULL DEFAULT 'created',
  provider_payment_id VARCHAR(191) NULL,
  provider_order_id VARCHAR(191) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS shipments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  carrier VARCHAR(80) NULL,
  tracking_number VARCHAR(120) NULL,
  status ENUM('pending','packed','in_transit','delivered') NOT NULL DEFAULT 'pending',
  shipped_at DATETIME NULL,
  delivered_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ship_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- REVIEWS & WISHLIST
CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  title VARCHAR(150) NULL,
  body TEXT NULL,
  status ENUM('pending','published','rejected') NOT NULL DEFAULT 'pending',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wishlists (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wishlist_items (
  wishlist_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  PRIMARY KEY (wishlist_id, product_id),
  CONSTRAINT fk_wli_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlists(id) ON DELETE CASCADE,
  CONSTRAINT fk_wli_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

-- AI / ANALYTICS (lightweight)
CREATE TABLE IF NOT EXISTS ai_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  type ENUM('chat','recommend','image_search','voice') NOT NULL,
  request_payload JSON,
  response_ref TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS searches (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  query_text VARCHAR(255) NOT NULL,
  type ENUM('text','voice','image') NOT NULL,
  result_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_search_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- INDEXES
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_variants_product ON product_variants(product_id);
CREATE INDEX idx_inventory_stock ON inventory(stock_qty);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_payments_order ON payments(order_id);

SET FOREIGN_KEY_CHECKS=1;
