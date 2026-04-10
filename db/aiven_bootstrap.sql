-- NexBuy Aiven bootstrap (MySQL 8)
-- Run this file once against your Aiven database (for example, defaultdb).
-- It creates the schema and seeds a storefront-ready catalog in one go.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(191) NOT NULL UNIQUE,
  phone VARCHAR(32) NULL,
  password_hash VARCHAR(255) NOT NULL,
  status ENUM('ACTIVE','INACTIVE','BLOCKED') NOT NULL DEFAULT 'INACTIVE',
  role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  oauth_provider VARCHAR(64) NULL,
  oauth_sub VARCHAR(191) NULL,
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_profiles (
  user_id BIGINT PRIMARY KEY,
  first_name VARCHAR(80) NULL,
  last_name VARCHAR(80) NULL,
  avatar_url VARCHAR(255) NULL,
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
  PRIMARY KEY (product_id, tag),
  CONSTRAINT fk_tag_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

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

CREATE TABLE IF NOT EXISTS refunds (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  payment_id BIGINT NULL,
  amount_cents INT NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'INR',
  status ENUM('pending','processing','processed','failed','cancelled') NOT NULL DEFAULT 'pending',
  provider_refund_id VARCHAR(255) NULL,
  note TEXT NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS return_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  status ENUM('requested','approved','accepted','rejected','completed','cancelled','received','refunded') NOT NULL DEFAULT 'requested',
  refund_status ENUM('not_started','pending','processing','processed','failed','cancelled') NOT NULL DEFAULT 'not_started',
  reason TEXT NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME NULL,
  picked_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_return_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

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

CREATE TABLE IF NOT EXISTS ai_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  type ENUM('chat','recommend','image_search','voice') NOT NULL,
  request_payload JSON NULL,
  response_ref TEXT NULL,
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

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO categories (name, slug, description, image_url) VALUES
  ('Mobiles', 'mobiles', 'Smartphones and daily-use mobile devices for work, study, and entertainment.', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80'),
  ('Electronics', 'electronics', 'Audio gear, connected gadgets, and smart devices for modern desks and homes.', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80'),
  ('Fashion', 'fashion', 'Everyday apparel with a mix of casual, workwear, and occasion-ready pieces.', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80'),
  ('Accessories', 'accessories', 'Practical and style-led accessories for travel, gifting, and daily carry.', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80'),
  ('Home', 'home', 'Home essentials, kitchen upgrades, and decor accents that feel useful and polished.', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80'),
  ('Books', 'books', 'Programming, productivity, business, and personal growth titles.', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80'),
  ('Shoes', 'shoes', 'Running, casual, and formal footwear built for comfort and repeat wear.', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80'),
  ('Gaming', 'gaming', 'Gaming setup gear for console, streaming, and competitive play.', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  image_url = VALUES(image_url),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO brands (name, slug) VALUES
  ('Volt Mobile', 'volt-mobile'),
  ('Sonivo', 'sonivo'),
  ('UrbanLoom', 'urbanloom'),
  ('Modora', 'modora'),
  ('Nestique', 'nestique'),
  ('PenVerse', 'penverse'),
  ('StrideCraft', 'stridecraft'),
  ('ArcNova Gaming', 'arcnova-gaming')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  updated_at = CURRENT_TIMESTAMP;

DROP TEMPORARY TABLE IF EXISTS seed_products;
CREATE TEMPORARY TABLE seed_products (
  category_slug VARCHAR(150) NOT NULL,
  brand_slug VARCHAR(150) NOT NULL,
  title VARCHAR(180) NOT NULL,
  slug VARCHAR(200) NOT NULL,
  description TEXT NOT NULL,
  cover_image VARCHAR(255) NOT NULL,
  sku VARCHAR(120) NOT NULL,
  variant_name VARCHAR(180) NOT NULL,
  price_cents INT NOT NULL,
  stock_qty INT NOT NULL,
  low_stock_threshold INT NOT NULL DEFAULT 5,
  is_backorder_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  tag_primary VARCHAR(80) NOT NULL,
  tag_secondary VARCHAR(80) NOT NULL,
  tag_tertiary VARCHAR(80) NOT NULL,
  PRIMARY KEY (slug),
  UNIQUE KEY uk_seed_products_sku (sku)
) ENGINE=InnoDB;

INSERT INTO seed_products (
  category_slug, brand_slug, title, slug, description, cover_image, sku, variant_name,
  price_cents, stock_qty, low_stock_threshold, is_backorder_allowed, tag_primary, tag_secondary, tag_tertiary
) VALUES
  ('mobiles', 'volt-mobile', 'Volt X1 Pro 5G', 'volt-x1-pro-5g', 'A premium-feeling 5G phone with a reliable camera setup, fast charging, and polished everyday performance.', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80', 'MOB-001', 'Volt X1 Pro 5G', 4299900, 18, 5, FALSE, 'mobiles', '5g', 'camera'),
  ('mobiles', 'volt-mobile', 'Nova S Max', 'nova-s-max', 'A balanced Android phone for shoppers who want a bright display, all-day battery, and dependable speed.', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80', 'MOB-002', 'Nova S Max', 3899900, 14, 5, FALSE, 'mobiles', 'amoled', 'battery'),
  ('mobiles', 'volt-mobile', 'Aero Lite 5G', 'aero-lite-5g', 'A lighter, value-focused 5G handset for students and everyday buyers who want strong performance for the price.', 'https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80', 'MOB-003', 'Aero Lite 5G', 2499900, 26, 5, FALSE, 'mobiles', '5g', 'value'),
  ('electronics', 'sonivo', 'EchoBeam Soundbar', 'echobeam-soundbar', 'A living-room-ready soundbar with fuller dialogue, easy setup, and a cleaner entertainment experience.', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 'ELE-001', 'EchoBeam Soundbar', 1299900, 20, 5, FALSE, 'electronics', 'audio', 'home-theater'),
  ('electronics', 'sonivo', 'AeroPods Studio', 'aeropods-studio', 'Wireless over-ear headphones designed for long listening sessions, focus time, and daily commuting.', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 'ELE-002', 'AeroPods Studio', 849900, 24, 5, FALSE, 'electronics', 'wireless', 'music'),
  ('electronics', 'sonivo', 'SkyWave Projector', 'skywave-projector', 'A compact projector that turns small rooms into movie spaces and supports casual presentation use.', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80', 'ELE-003', 'SkyWave Projector', 1899900, 9, 5, TRUE, 'electronics', 'projector', 'cinema'),
  ('fashion', 'urbanloom', 'Urban Cotton Tee', 'urban-cotton-tee', 'A clean everyday cotton tee that works as an easy staple for casual outfits and layered looks.', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80', 'FAS-001', 'Urban Cotton Tee', 89900, 46, 5, FALSE, 'fashion', 'cotton', 'casual'),
  ('fashion', 'urbanloom', 'Weekend Denim Jacket', 'weekend-denim-jacket', 'A classic denim layer for off-duty looks, transitional weather, and year-round styling.', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80', 'FAS-002', 'Weekend Denim Jacket', 249900, 18, 5, FALSE, 'fashion', 'denim', 'layering'),
  ('fashion', 'urbanloom', 'Tailored Office Blazer', 'tailored-office-blazer', 'A sharper blazer built for office wear, formal events, and polished smart-casual outfits.', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80', 'FAS-003', 'Tailored Office Blazer', 349900, 10, 5, TRUE, 'fashion', 'formal', 'workwear'),
  ('accessories', 'modora', 'ChronoFit Smartwatch', 'chronofit-smartwatch', 'A smartwatch with a sporty look, fitness-friendly tracking, and daily notification support.', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 'ACC-001', 'ChronoFit Smartwatch', 499900, 17, 5, FALSE, 'accessories', 'smartwatch', 'fitness'),
  ('accessories', 'modora', 'Urban Sling Bag', 'urban-sling-bag', 'A compact sling bag sized for everyday essentials, short commutes, and travel-friendly carry.', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80', 'ACC-002', 'Urban Sling Bag', 189900, 21, 5, FALSE, 'accessories', 'bag', 'travel'),
  ('accessories', 'modora', 'Aurora Pendant', 'aurora-pendant', 'A simple statement pendant that works well as a gift item and occasion-ready accessory.', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80', 'ACC-003', 'Aurora Pendant', 159900, 15, 5, FALSE, 'accessories', 'jewelry', 'gift'),
  ('home', 'nestique', 'HearthGlow Lamp', 'hearthglow-lamp', 'A warm table lamp that brings softer lighting and a more finished feel to bedrooms and desks.', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80', 'HOM-001', 'HearthGlow Lamp', 299900, 19, 5, FALSE, 'home', 'lighting', 'decor'),
  ('home', 'nestique', 'FreshPress Coffee Maker', 'freshpress-coffee-maker', 'A compact coffee maker for quick morning routines and easy daily brewing at home.', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80', 'HOM-002', 'FreshPress Coffee Maker', 229900, 16, 5, FALSE, 'home', 'kitchen', 'coffee'),
  ('home', 'nestique', 'AromaMist Diffuser', 'aromamist-diffuser', 'A diffuser that adds a gentle fragrance ritual to bedrooms, workspaces, and relaxation corners.', 'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80', 'HOM-003', 'AromaMist Diffuser', 169900, 23, 5, FALSE, 'home', 'wellness', 'fragrance'),
  ('books', 'penverse', 'Spring Boot Field Guide', 'spring-boot-field-guide', 'A developer-friendly reference for building practical Spring Boot applications with production-minded patterns.', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80', 'BOO-001', 'Spring Boot Field Guide', 94900, 28, 5, FALSE, 'books', 'programming', 'java'),
  ('books', 'penverse', 'AI Playbook for Teams', 'ai-playbook-for-teams', 'A business-focused guide to introducing AI tools, workflows, and decision-making into team operations.', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80', 'BOO-002', 'AI Playbook for Teams', 92900, 21, 5, FALSE, 'books', 'ai', 'leadership'),
  ('books', 'penverse', 'Ecommerce Growth Manual', 'ecommerce-growth-manual', 'A practical title on ecommerce metrics, merchandising, retention, and conversion thinking.', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80', 'BOO-003', 'Ecommerce Growth Manual', 88900, 20, 5, FALSE, 'books', 'ecommerce', 'business'),
  ('shoes', 'stridecraft', 'SprintFlex Runner Pro', 'sprintflex-runner-pro', 'A running shoe built around comfort, grip, and repeat training use on roads and treadmills.', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', 'SHO-001', 'SprintFlex Runner Pro', 379900, 15, 5, FALSE, 'shoes', 'running', 'performance'),
  ('shoes', 'stridecraft', 'CityMove Knit Sneakers', 'citymove-knit-sneakers', 'A knit sneaker designed for casual wear, travel days, and easy all-day comfort.', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', 'SHO-002', 'CityMove Knit Sneakers', 299900, 24, 5, FALSE, 'shoes', 'casual', 'comfort'),
  ('shoes', 'stridecraft', 'OfficeForm Penny Loafers', 'officeform-penny-loafers', 'A formal loafer option for office fits, events, and dressed-up everyday wardrobes.', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80', 'SHO-003', 'OfficeForm Penny Loafers', 279900, 19, 5, FALSE, 'shoes', 'formal', 'office'),
  ('gaming', 'arcnova-gaming', 'ArcPulse Wired Controller', 'arcpulse-wired-controller', 'A responsive wired controller for competitive sessions, couch gaming, and reliable plug-and-play use.', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80', 'GAM-001', 'ArcPulse Wired Controller', 349900, 22, 5, FALSE, 'gaming', 'controller', 'console'),
  ('gaming', 'arcnova-gaming', 'Phantom Aim Mouse', 'phantom-aim-mouse', 'A lightweight gaming mouse tuned for precision, fast response, and long desk sessions.', 'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?auto=format&fit=crop&w=900&q=80', 'GAM-002', 'Phantom Aim Mouse', 269900, 29, 5, FALSE, 'gaming', 'mouse', 'esports'),
  ('gaming', 'arcnova-gaming', 'RiftView Curved Monitor', 'riftview-curved-monitor', 'A curved monitor for immersive play, sharper focus, and a stronger streaming or battlestation setup.', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80', 'GAM-003', 'RiftView Curved Monitor', 2199900, 7, 5, FALSE, 'gaming', 'monitor', 'setup');

INSERT INTO products (category_id, brand_id, title, slug, description, cover_image, status)
SELECT c.id, b.id, sp.title, sp.slug, sp.description, sp.cover_image, 'active'
FROM seed_products sp
JOIN categories c ON c.slug = sp.category_slug
JOIN brands b ON b.slug = sp.brand_slug
ON DUPLICATE KEY UPDATE
  category_id = VALUES(category_id),
  brand_id = VALUES(brand_id),
  title = VALUES(title),
  description = VALUES(description),
  cover_image = VALUES(cover_image),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO product_variants (product_id, sku, name, price_cents, compare_at_cents, currency)
SELECT p.id, sp.sku, sp.variant_name, sp.price_cents, ROUND(sp.price_cents * 1.12), 'INR'
FROM seed_products sp
JOIN products p ON p.slug = sp.slug
ON DUPLICATE KEY UPDATE
  product_id = VALUES(product_id),
  name = VALUES(name),
  price_cents = VALUES(price_cents),
  compare_at_cents = VALUES(compare_at_cents),
  currency = VALUES(currency),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO inventory (variant_id, stock_qty, low_stock_threshold, is_backorder_allowed)
SELECT pv.id, sp.stock_qty, sp.low_stock_threshold, sp.is_backorder_allowed
FROM seed_products sp
JOIN product_variants pv ON pv.sku = sp.sku
ON DUPLICATE KEY UPDATE
  stock_qty = VALUES(stock_qty),
  low_stock_threshold = VALUES(low_stock_threshold),
  is_backorder_allowed = VALUES(is_backorder_allowed),
  updated_at = CURRENT_TIMESTAMP;

DELETE pm
FROM product_media pm
JOIN products p ON p.id = pm.product_id
JOIN seed_products sp ON sp.slug = p.slug;

INSERT INTO product_media (product_id, url, alt_text, sort_order)
SELECT p.id, sp.cover_image, CONCAT(sp.title, ' cover image'), 0
FROM seed_products sp
JOIN products p ON p.slug = sp.slug;

INSERT INTO product_media (product_id, url, alt_text, sort_order)
SELECT p.id, CONCAT('https://picsum.photos/seed/', sp.slug, '-gallery-2/900/900'), CONCAT(sp.title, ' gallery image 2'), 1
FROM seed_products sp
JOIN products p ON p.slug = sp.slug;

INSERT INTO product_media (product_id, url, alt_text, sort_order)
SELECT p.id, CONCAT('https://picsum.photos/seed/', sp.slug, '-gallery-3/900/900'), CONCAT(sp.title, ' gallery image 3'), 2
FROM seed_products sp
JOIN products p ON p.slug = sp.slug;

DELETE pt
FROM product_tags pt
JOIN products p ON p.id = pt.product_id
JOIN seed_products sp ON sp.slug = p.slug;

INSERT INTO product_tags (product_id, tag)
SELECT p.id, sp.tag_primary
FROM seed_products sp
JOIN products p ON p.slug = sp.slug;

INSERT INTO product_tags (product_id, tag)
SELECT p.id, sp.tag_secondary
FROM seed_products sp
JOIN products p ON p.slug = sp.slug;

INSERT INTO product_tags (product_id, tag)
SELECT p.id, sp.tag_tertiary
FROM seed_products sp
JOIN products p ON p.slug = sp.slug;

INSERT INTO searches (user_id, query_text, type, result_count)
SELECT NULL, 'wireless headphones', 'text', 3
WHERE NOT EXISTS (
  SELECT 1
  FROM searches
  WHERE user_id IS NULL AND query_text = 'wireless headphones' AND type = 'text'
);

INSERT INTO searches (user_id, query_text, type, result_count)
SELECT NULL, 'gaming monitor', 'text', 2
WHERE NOT EXISTS (
  SELECT 1
  FROM searches
  WHERE user_id IS NULL AND query_text = 'gaming monitor' AND type = 'text'
);

INSERT INTO ai_requests (user_id, type, request_payload, response_ref)
SELECT NULL, 'recommend', JSON_OBJECT('context', 'bootstrap'), 'Initial storefront recommendation seed'
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_requests
  WHERE user_id IS NULL AND type = 'recommend' AND response_ref = 'Initial storefront recommendation seed'
);

DROP TEMPORARY TABLE IF EXISTS seed_products;

-- Note:
-- The backend still auto-creates admin@nexbuy.com / Admin@123 on startup if that user is missing.
