USE nexbuy_db;

-- Storefront-aligned catalog seed: 8 categories, 240 products, media, inventory, and tags.

INSERT INTO categories (name, slug, description, image_url) VALUES
  ('Mobiles', 'mobiles', 'Smartphones and daily-carry mobile tech for work, study, and entertainment.', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80'),
  ('Electronics', 'electronics', 'Connected electronics, audio gear, and smart devices for the modern home office.', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80'),
  ('Fashion', 'fashion', 'Versatile apparel for everyday wear, office hours, and weekend plans.', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80'),
  ('Accessories', 'accessories', 'Functional accessories designed to complement travel, work, and daily routines.', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80'),
  ('Home', 'home', 'Home essentials, kitchen helpers, and decor pieces that make spaces feel finished.', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80'),
  ('Books', 'books', 'Skill-building, business, and personal growth titles for learning-focused readers.', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80'),
  ('Shoes', 'shoes', 'Running, casual, and outdoor footwear built for comfort across daily use cases.', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80'),
  ('Gaming', 'gaming', 'Gaming gear and setup essentials for streaming, competitive play, and comfort.', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), image_url = VALUES(image_url);

INSERT INTO brands (name, slug) VALUES
  ('Volt Mobile', 'volt-mobile'),
  ('Sonivo', 'sonivo'),
  ('UrbanLoom', 'urbanloom'),
  ('Modora', 'modora'),
  ('Nestique', 'nestique'),
  ('PenVerse', 'penverse'),
  ('StrideCraft', 'stridecraft'),
  ('ArcNova Gaming', 'arcnova-gaming')
ON DUPLICATE KEY UPDATE name = VALUES(name);

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
  tag_tertiary VARCHAR(80) NOT NULL
) ENGINE=InnoDB;

INSERT INTO seed_products (category_slug, brand_slug, title, slug, description, cover_image, sku, variant_name, price_cents, stock_qty, low_stock_threshold, is_backorder_allowed, tag_primary, tag_secondary, tag_tertiary) VALUES
  ('mobiles', 'volt-mobile', 'Volt X1 Pro 5G', 'volt-x1-pro-5g', 'Volt X1 Pro 5G is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80', 'MOB-001', 'Volt X1 Pro 5G', 4299900, 18, 5, FALSE, 'mobiles', '5g', 'camera'),
  ('mobiles', 'volt-mobile', 'Nova S Max', 'nova-s-max', 'Nova S Max is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80', 'MOB-002', 'Nova S Max', 3899900, 14, 5, FALSE, 'mobiles', 'amoled', 'fast-charge'),
  ('mobiles', 'volt-mobile', 'Aero Lite 5G', 'aero-lite-5g', 'Aero Lite 5G is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80', 'MOB-003', 'Aero Lite 5G', 2499900, 27, 5, FALSE, 'mobiles', 'lightweight', 'student-pick'),
  ('mobiles', 'volt-mobile', 'PixelEdge Mini', 'pixeledge-mini', 'PixelEdge Mini is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80', 'MOB-004', 'PixelEdge Mini', 2999900, 11, 5, FALSE, 'mobiles', 'compact', 'travel'),
  ('mobiles', 'volt-mobile', 'Horizon Fold Air', 'horizon-fold-air', 'Horizon Fold Air is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80', 'MOB-005', 'Horizon Fold Air', 7999900, 6, 5, TRUE, 'mobiles', 'foldable', 'premium'),
  ('mobiles', 'volt-mobile', 'Spark Note 12', 'spark-note-12', 'Spark Note 12 is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80', 'MOB-006', 'Spark Note 12', 2199900, 22, 5, FALSE, 'mobiles', 'big-display', 'battery'),
  ('mobiles', 'volt-mobile', 'Atlas Ultra Camera', 'atlas-ultra-camera', 'Atlas Ultra Camera is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80', 'MOB-007', 'Atlas Ultra Camera', 5599900, 8, 5, TRUE, 'mobiles', 'camera', 'creator'),
  ('mobiles', 'volt-mobile', 'Neo Core 5G', 'neo-core-5g', 'Neo Core 5G is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80', 'MOB-008', 'Neo Core 5G', 1899900, 31, 5, FALSE, 'mobiles', 'budget', 'daily-use'),
  ('mobiles', 'volt-mobile', 'Pulse M6', 'pulse-m6', 'Pulse M6 is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80', 'MOB-009', 'Pulse M6', 2799900, 16, 5, FALSE, 'mobiles', 'gaming', 'performance'),
  ('mobiles', 'volt-mobile', 'Orbit Go 5G', 'orbit-go-5g', 'Orbit Go 5G is a NexBuy-ready mobiles pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80', 'MOB-010', 'Orbit Go 5G', 1599900, 25, 5, FALSE, 'mobiles', 'value', 'starter-phone'),
  ('electronics', 'sonivo', 'EchoBeam Soundbar', 'echobeam-soundbar', 'EchoBeam Soundbar is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 'ELE-001', 'EchoBeam Soundbar', 1299900, 19, 5, FALSE, 'electronics', 'audio', 'home-theater'),
  ('electronics', 'sonivo', 'SonicDock Speaker', 'sonicdock-speaker', 'SonicDock Speaker is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80', 'ELE-002', 'SonicDock Speaker', 699900, 24, 5, FALSE, 'electronics', 'bluetooth', 'portable'),
  ('electronics', 'sonivo', 'AeroPods Studio', 'aeropods-studio', 'AeroPods Studio is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 'ELE-003', 'AeroPods Studio', 849900, 20, 5, FALSE, 'electronics', 'wireless', 'music'),
  ('electronics', 'sonivo', 'VoltHub Smart Plug', 'volthub-smart-plug', 'VoltHub Smart Plug is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1580894894513-541e068a3e2b?auto=format&fit=crop&w=900&q=80', 'ELE-004', 'VoltHub Smart Plug', 199900, 40, 5, FALSE, 'electronics', 'smart-home', 'energy-save'),
  ('electronics', 'sonivo', 'Lumio Air Purifier', 'lumio-air-purifier', 'Lumio Air Purifier is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 'ELE-005', 'Lumio Air Purifier', 999900, 12, 5, FALSE, 'electronics', 'air-care', 'quiet'),
  ('electronics', 'sonivo', 'StreamBox 4K', 'streambox-4k', 'StreamBox 4K is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80', 'ELE-006', 'StreamBox 4K', 549900, 17, 5, FALSE, 'electronics', 'streaming', 'tv'),
  ('electronics', 'sonivo', 'VisionCam Home', 'visioncam-home', 'VisionCam Home is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 'ELE-007', 'VisionCam Home', 459900, 23, 5, FALSE, 'electronics', 'security', 'smart-home'),
  ('electronics', 'sonivo', 'Glide Keyboard', 'glide-keyboard', 'Glide Keyboard is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1580894894513-541e068a3e2b?auto=format&fit=crop&w=900&q=80', 'ELE-008', 'Glide Keyboard', 349900, 28, 5, FALSE, 'electronics', 'workspace', 'wireless'),
  ('electronics', 'sonivo', 'Pulse Mouse Pro', 'pulse-mouse-pro', 'Pulse Mouse Pro is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 'ELE-009', 'Pulse Mouse Pro', 259900, 33, 5, FALSE, 'electronics', 'productivity', 'ergonomic'),
  ('electronics', 'sonivo', 'SkyWave Projector', 'skywave-projector', 'SkyWave Projector is a NexBuy-ready electronics pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80', 'ELE-010', 'SkyWave Projector', 1899900, 9, 5, TRUE, 'electronics', 'projector', 'cinema'),
  ('fashion', 'urbanloom', 'Urban Cotton Tee', 'urban-cotton-tee', 'Urban Cotton Tee is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80', 'FAS-001', 'Urban Cotton Tee', 89900, 46, 5, FALSE, 'fashion', 'cotton', 'casual'),
  ('fashion', 'urbanloom', 'Weekend Denim Jacket', 'weekend-denim-jacket', 'Weekend Denim Jacket is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80', 'FAS-002', 'Weekend Denim Jacket', 249900, 18, 5, FALSE, 'fashion', 'denim', 'layering'),
  ('fashion', 'urbanloom', 'CloudFit Hoodie', 'cloudfit-hoodie', 'CloudFit Hoodie is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80', 'FAS-003', 'CloudFit Hoodie', 189900, 22, 5, FALSE, 'fashion', 'hoodie', 'comfort'),
  ('fashion', 'urbanloom', 'Linen Ease Shirt', 'linen-ease-shirt', 'Linen Ease Shirt is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80', 'FAS-004', 'Linen Ease Shirt', 159900, 20, 5, FALSE, 'fashion', 'linen', 'summer'),
  ('fashion', 'urbanloom', 'Metro Chino Pants', 'metro-chino-pants', 'Metro Chino Pants is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80', 'FAS-005', 'Metro Chino Pants', 179900, 24, 5, FALSE, 'fashion', 'office-wear', 'smart-casual'),
  ('fashion', 'urbanloom', 'Breeze Kurti Set', 'breeze-kurti-set', 'Breeze Kurti Set is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80', 'FAS-006', 'Breeze Kurti Set', 229900, 17, 5, FALSE, 'fashion', 'ethnic', 'festive'),
  ('fashion', 'urbanloom', 'Tailored Office Blazer', 'tailored-office-blazer', 'Tailored Office Blazer is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80', 'FAS-007', 'Tailored Office Blazer', 349900, 10, 5, TRUE, 'fashion', 'formal', 'workwear'),
  ('fashion', 'urbanloom', 'Everyday Joggers', 'everyday-joggers', 'Everyday Joggers is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80', 'FAS-008', 'Everyday Joggers', 139900, 29, 5, FALSE, 'fashion', 'athleisure', 'weekend'),
  ('fashion', 'urbanloom', 'Classic Polo Shirt', 'classic-polo-shirt', 'Classic Polo Shirt is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80', 'FAS-009', 'Classic Polo Shirt', 129900, 26, 5, FALSE, 'fashion', 'polo', 'smart-casual'),
  ('fashion', 'urbanloom', 'SoftWeave Cardigan', 'softweave-cardigan', 'SoftWeave Cardigan is a NexBuy-ready fashion pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80', 'FAS-010', 'SoftWeave Cardigan', 199900, 15, 5, FALSE, 'fashion', 'knitwear', 'layering'),
  ('accessories', 'modora', 'ChronoFit Smartwatch', 'chronofit-smartwatch', 'ChronoFit Smartwatch is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 'ACC-001', 'ChronoFit Smartwatch', 499900, 19, 5, FALSE, 'accessories', 'smartwatch', 'fitness'),
  ('accessories', 'modora', 'Leather Snap Wallet', 'leather-snap-wallet', 'Leather Snap Wallet is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80', 'ACC-002', 'Leather Snap Wallet', 149900, 34, 5, FALSE, 'accessories', 'wallet', 'leather'),
  ('accessories', 'modora', 'Urban Sling Bag', 'urban-sling-bag', 'Urban Sling Bag is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80', 'ACC-003', 'Urban Sling Bag', 189900, 21, 5, FALSE, 'accessories', 'bag', 'travel'),
  ('accessories', 'modora', 'AeroShield Sunglasses', 'aeroshield-sunglasses', 'AeroShield Sunglasses is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80', 'ACC-004', 'AeroShield Sunglasses', 129900, 28, 5, FALSE, 'accessories', 'sunglasses', 'outdoor'),
  ('accessories', 'modora', 'LoopBand Fitness Band', 'loopband-fitness-band', 'LoopBand Fitness Band is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 'ACC-005', 'LoopBand Fitness Band', 249900, 24, 5, FALSE, 'accessories', 'wearable', 'health'),
  ('accessories', 'modora', 'Travel Zip Pouch', 'travel-zip-pouch', 'Travel Zip Pouch is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80', 'ACC-006', 'Travel Zip Pouch', 79900, 38, 5, FALSE, 'accessories', 'organizer', 'travel'),
  ('accessories', 'modora', 'Minimal Key Organizer', 'minimal-key-organizer', 'Minimal Key Organizer is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80', 'ACC-007', 'Minimal Key Organizer', 69900, 42, 5, FALSE, 'accessories', 'edc', 'minimal'),
  ('accessories', 'modora', 'Slim Metal Belt', 'slim-metal-belt', 'Slim Metal Belt is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80', 'ACC-008', 'Slim Metal Belt', 99900, 25, 5, FALSE, 'accessories', 'belt', 'workwear'),
  ('accessories', 'modora', 'Everyday Cap', 'everyday-cap', 'Everyday Cap is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 'ACC-009', 'Everyday Cap', 59900, 47, 5, FALSE, 'accessories', 'cap', 'streetwear'),
  ('accessories', 'modora', 'Aurora Pendant', 'aurora-pendant', 'Aurora Pendant is a NexBuy-ready accessories pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80', 'ACC-010', 'Aurora Pendant', 159900, 14, 5, FALSE, 'accessories', 'jewelry', 'gift'),
  ('home', 'nestique', 'HearthGlow Lamp', 'hearthglow-lamp', 'HearthGlow Lamp is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80', 'HOM-001', 'HearthGlow Lamp', 219900, 16, 5, FALSE, 'home', 'lighting', 'decor'),
  ('home', 'nestique', 'CozyNest Cushion Set', 'cozynest-cushion-set', 'CozyNest Cushion Set is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80', 'HOM-002', 'CozyNest Cushion Set', 149900, 22, 5, FALSE, 'home', 'soft-furnishing', 'living-room'),
  ('home', 'nestique', 'SteamChef Kettle', 'steamchef-kettle', 'SteamChef Kettle is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80', 'HOM-003', 'SteamChef Kettle', 179900, 18, 5, FALSE, 'home', 'kitchen', 'appliance'),
  ('home', 'nestique', 'PureMist Diffuser', 'puremist-diffuser', 'PureMist Diffuser is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?auto=format&fit=crop&w=900&q=80', 'HOM-004', 'PureMist Diffuser', 129900, 27, 5, FALSE, 'home', 'wellness', 'aroma'),
  ('home', 'nestique', 'FreshBrew French Press', 'freshbrew-french-press', 'FreshBrew French Press is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80', 'HOM-005', 'FreshBrew French Press', 99900, 31, 5, FALSE, 'home', 'coffee', 'kitchen'),
  ('home', 'nestique', 'SoftFold Blanket', 'softfold-blanket', 'SoftFold Blanket is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80', 'HOM-006', 'SoftFold Blanket', 169900, 20, 5, FALSE, 'home', 'bedding', 'comfort'),
  ('home', 'nestique', 'EcoServe Dinner Set', 'ecoserve-dinner-set', 'EcoServe Dinner Set is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80', 'HOM-007', 'EcoServe Dinner Set', 249900, 13, 5, FALSE, 'home', 'dining', 'tableware'),
  ('home', 'nestique', 'NovaDesk Organizer', 'novadesk-organizer', 'NovaDesk Organizer is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?auto=format&fit=crop&w=900&q=80', 'HOM-008', 'NovaDesk Organizer', 89900, 35, 5, FALSE, 'home', 'organization', 'workspace'),
  ('home', 'nestique', 'CleanSweep Mop Kit', 'cleansweep-mop-kit', 'CleanSweep Mop Kit is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80', 'HOM-009', 'CleanSweep Mop Kit', 119900, 28, 5, FALSE, 'home', 'cleaning', 'utility'),
  ('home', 'nestique', 'AromaJar Candle Pack', 'aromajar-candle-pack', 'AromaJar Candle Pack is a NexBuy-ready home pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80', 'HOM-010', 'AromaJar Candle Pack', 79900, 40, 5, FALSE, 'home', 'fragrance', 'gift'),
  ('books', 'penverse', 'Build Better Habits', 'build-better-habits', 'Build Better Habits is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80', 'BOO-001', 'Build Better Habits', 59900, 51, 5, FALSE, 'books', 'self-growth', 'paperback'),
  ('books', 'penverse', 'Modern Java Playbook', 'modern-java-playbook', 'Modern Java Playbook is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80', 'BOO-002', 'Modern Java Playbook', 89900, 33, 5, FALSE, 'books', 'programming', 'reference'),
  ('books', 'penverse', 'Retail Strategy Simplified', 'retail-strategy-simplified', 'Retail Strategy Simplified is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80', 'BOO-003', 'Retail Strategy Simplified', 74900, 26, 5, FALSE, 'books', 'business', 'strategy'),
  ('books', 'penverse', 'Design Thinking Notes', 'design-thinking-notes', 'Design Thinking Notes is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80', 'BOO-004', 'Design Thinking Notes', 64900, 29, 5, FALSE, 'books', 'design', 'innovation'),
  ('books', 'penverse', 'Startup Finance Basics', 'startup-finance-basics', 'Startup Finance Basics is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80', 'BOO-005', 'Startup Finance Basics', 69900, 25, 5, FALSE, 'books', 'finance', 'startup'),
  ('books', 'penverse', 'Mastering Data Stories', 'mastering-data-stories', 'Mastering Data Stories is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80', 'BOO-006', 'Mastering Data Stories', 79900, 21, 5, FALSE, 'books', 'analytics', 'communication'),
  ('books', 'penverse', 'Everyday English Guide', 'everyday-english-guide', 'Everyday English Guide is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80', 'BOO-007', 'Everyday English Guide', 49900, 45, 5, FALSE, 'books', 'language', 'study'),
  ('books', 'penverse', 'Productive Study Methods', 'productive-study-methods', 'Productive Study Methods is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80', 'BOO-008', 'Productive Study Methods', 54900, 38, 5, FALSE, 'books', 'education', 'students'),
  ('books', 'penverse', 'Clean Code Companion', 'clean-code-companion', 'Clean Code Companion is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80', 'BOO-009', 'Clean Code Companion', 99900, 19, 5, FALSE, 'books', 'coding', 'best-practices'),
  ('books', 'penverse', 'AI for Business Teams', 'ai-for-business-teams', 'AI for Business Teams is a NexBuy-ready books pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80', 'BOO-010', 'AI for Business Teams', 84900, 24, 5, FALSE, 'books', 'ai', 'leadership'),
  ('shoes', 'stridecraft', 'SprintRun Mesh Sneakers', 'sprintrun-mesh-sneakers', 'SprintRun Mesh Sneakers is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', 'SHO-001', 'SprintRun Mesh Sneakers', 329900, 18, 5, FALSE, 'shoes', 'running', 'mesh'),
  ('shoes', 'stridecraft', 'CityWalk Casual Shoes', 'citywalk-casual-shoes', 'CityWalk Casual Shoes is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', 'SHO-002', 'CityWalk Casual Shoes', 279900, 23, 5, FALSE, 'shoes', 'casual', 'daily'),
  ('shoes', 'stridecraft', 'TrailPeak Hiking Shoes', 'trailpeak-hiking-shoes', 'TrailPeak Hiking Shoes is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=900&q=80', 'SHO-003', 'TrailPeak Hiking Shoes', 399900, 14, 5, FALSE, 'shoes', 'hiking', 'outdoor'),
  ('shoes', 'stridecraft', 'CourtFlex Trainers', 'courtflex-trainers', 'CourtFlex Trainers is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80', 'SHO-004', 'CourtFlex Trainers', 309900, 19, 5, FALSE, 'shoes', 'training', 'gym'),
  ('shoes', 'stridecraft', 'AeroStride Running Shoes', 'aerostride-running-shoes', 'AeroStride Running Shoes is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', 'SHO-005', 'AeroStride Running Shoes', 359900, 17, 5, FALSE, 'shoes', 'running', 'lightweight'),
  ('shoes', 'stridecraft', 'StreetMode High Tops', 'streetmode-high-tops', 'StreetMode High Tops is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', 'SHO-006', 'StreetMode High Tops', 289900, 21, 5, FALSE, 'shoes', 'streetwear', 'high-top'),
  ('shoes', 'stridecraft', 'TerraGrip Sandals', 'terragrip-sandals', 'TerraGrip Sandals is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=900&q=80', 'SHO-007', 'TerraGrip Sandals', 169900, 30, 5, FALSE, 'shoes', 'sandals', 'travel'),
  ('shoes', 'stridecraft', 'OfficeEase Loafers', 'officeease-loafers', 'OfficeEase Loafers is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80', 'SHO-008', 'OfficeEase Loafers', 249900, 20, 5, FALSE, 'shoes', 'formal', 'office'),
  ('shoes', 'stridecraft', 'CloudStep Slip-ons', 'cloudstep-slip-ons', 'CloudStep Slip-ons is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', 'SHO-009', 'CloudStep Slip-ons', 199900, 27, 5, FALSE, 'shoes', 'slip-on', 'comfort'),
  ('shoes', 'stridecraft', 'PacePro Sports Shoes', 'pacepro-sports-shoes', 'PacePro Sports Shoes is a NexBuy-ready shoes pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', 'SHO-010', 'PacePro Sports Shoes', 339900, 16, 5, FALSE, 'shoes', 'sports', 'performance'),
  ('gaming', 'arcnova-gaming', 'ArcStorm Controller', 'arcstorm-controller', 'ArcStorm Controller is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80', 'GAM-001', 'ArcStorm Controller', 399900, 18, 5, FALSE, 'gaming', 'controller', 'console'),
  ('gaming', 'arcnova-gaming', 'Quantum RGB Keyboard', 'quantum-rgb-keyboard', 'Quantum RGB Keyboard is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80', 'GAM-002', 'Quantum RGB Keyboard', 349900, 22, 5, FALSE, 'gaming', 'keyboard', 'rgb'),
  ('gaming', 'arcnova-gaming', 'Phantom Gaming Mouse', 'phantom-gaming-mouse', 'Phantom Gaming Mouse is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?auto=format&fit=crop&w=900&q=80', 'GAM-003', 'Phantom Gaming Mouse', 249900, 26, 5, FALSE, 'gaming', 'mouse', 'esports'),
  ('gaming', 'arcnova-gaming', 'PulseWave Headset', 'pulsewave-headset', 'PulseWave Headset is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=900&q=80', 'GAM-004', 'PulseWave Headset', 299900, 20, 5, FALSE, 'gaming', 'headset', 'voice-chat'),
  ('gaming', 'arcnova-gaming', 'NovaPlay Capture Card', 'novaplay-capture-card', 'NovaPlay Capture Card is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80', 'GAM-005', 'NovaPlay Capture Card', 699900, 9, 5, TRUE, 'gaming', 'streaming', 'creator'),
  ('gaming', 'arcnova-gaming', 'TurboCharge Dock', 'turbocharge-dock', 'TurboCharge Dock is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80', 'GAM-006', 'TurboCharge Dock', 219900, 24, 5, FALSE, 'gaming', 'charging', 'console'),
  ('gaming', 'arcnova-gaming', 'PixelForge Gaming Chair', 'pixelforge-gaming-chair', 'PixelForge Gaming Chair is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?auto=format&fit=crop&w=900&q=80', 'GAM-007', 'PixelForge Gaming Chair', 1249900, 7, 5, TRUE, 'gaming', 'chair', 'setup'),
  ('gaming', 'arcnova-gaming', 'ShadowPad XXL Mouse Mat', 'shadowpad-xxl-mouse-mat', 'ShadowPad XXL Mouse Mat is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=900&q=80', 'GAM-008', 'ShadowPad XXL Mouse Mat', 99900, 34, 5, FALSE, 'gaming', 'deskmat', 'rgb'),
  ('gaming', 'arcnova-gaming', 'RiftView 144Hz Monitor', 'riftview-144hz-monitor', 'RiftView 144Hz Monitor is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80', 'GAM-009', 'RiftView 144Hz Monitor', 1899900, 8, 5, TRUE, 'gaming', 'monitor', '144hz'),
  ('gaming', 'arcnova-gaming', 'HexCore Mini Console', 'hexcore-mini-console', 'HexCore Mini Console is a NexBuy-ready gaming pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80', 'GAM-010', 'HexCore Mini Console', 1599900, 11, 5, FALSE, 'gaming', 'console', 'family-gaming');

DROP TEMPORARY TABLE IF EXISTS seed_product_expansion;
CREATE TEMPORARY TABLE seed_product_expansion (
  category_slug VARCHAR(150) NOT NULL,
  seq_no INT NOT NULL,
  title VARCHAR(180) NOT NULL,
  cover_image VARCHAR(255) NOT NULL,
  price_cents INT NOT NULL,
  stock_qty INT NOT NULL,
  low_stock_threshold INT NOT NULL DEFAULT 5,
  is_backorder_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  tag_secondary VARCHAR(80) NOT NULL,
  tag_tertiary VARCHAR(80) NOT NULL,
  PRIMARY KEY (category_slug, seq_no)
) ENGINE=InnoDB;

INSERT INTO seed_product_expansion (category_slug, seq_no, title, cover_image, price_cents, stock_qty, low_stock_threshold, is_backorder_allowed, tag_secondary, tag_tertiary) VALUES
  ('mobiles', 11, 'Volt Edge Lite 5G', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80', 3399900, 19, 5, FALSE, '5g', 'battery'),
  ('mobiles', 12, 'Nova Vision Max', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80', 4499900, 12, 5, FALSE, 'amoled', 'camera'),
  ('mobiles', 13, 'Aero Zoom Pro', 'https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80', 2999900, 21, 5, FALSE, 'camera', 'creator'),
  ('mobiles', 14, 'PixelTrail 5G', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80', 2699900, 24, 5, FALSE, '5g', 'travel'),
  ('mobiles', 15, 'Horizon Note Air', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80', 3199900, 16, 5, FALSE, 'big-display', 'battery'),
  ('mobiles', 16, 'Spark View Plus', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80', 2299900, 28, 5, FALSE, 'value', 'daily-use'),
  ('mobiles', 17, 'Atlas Neo 5G', 'https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80', 3899900, 14, 5, FALSE, 'performance', '5g'),
  ('mobiles', 18, 'Orbit Camera X', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80', 4799900, 11, 5, FALSE, 'camera', 'premium'),
  ('mobiles', 19, 'Pulse Core Max', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80', 2599900, 26, 5, FALSE, 'gaming', 'performance'),
  ('mobiles', 20, 'Neo Stream Lite', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80', 1999900, 31, 5, FALSE, 'budget', 'streaming'),
  ('mobiles', 21, 'Volt Zoom 5G', 'https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80', 3599900, 18, 5, FALSE, '5g', 'camera'),
  ('mobiles', 22, 'Nova Pace Ultra', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80', 5199900, 9, 5, FALSE, 'premium', 'battery'),
  ('mobiles', 23, 'Aero Note Go', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80', 1899900, 33, 5, FALSE, 'starter-phone', 'student-pick'),
  ('mobiles', 24, 'PixelRise Max', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80', 2899900, 20, 5, FALSE, 'amoled', 'fast-charge'),
  ('mobiles', 25, 'Horizon Snap 5G', 'https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80', 3299900, 17, 5, FALSE, '5g', 'creator'),
  ('mobiles', 26, 'Spark Play Turbo', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80', 2799900, 22, 5, FALSE, 'gaming', 'fast-charge'),
  ('mobiles', 27, 'Atlas Mini Pro', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80', 2399900, 25, 5, FALSE, 'compact', 'camera'),
  ('mobiles', 28, 'Orbit Charge 5G', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80', 3099900, 19, 5, FALSE, 'battery', '5g'),
  ('mobiles', 29, 'Pulse Vision Air', 'https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80', 3699900, 15, 5, FALSE, 'camera', 'travel'),
  ('mobiles', 30, 'Neo Lite One', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80', 1499900, 36, 5, FALSE, 'budget', 'starter-phone'),
  ('electronics', 11, 'EchoWave Mini Speaker', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 459900, 27, 5, FALSE, 'bluetooth', 'portable'),
  ('electronics', 12, 'SonicHub Sound Tower', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80', 1499900, 13, 5, FALSE, 'audio', 'home-theater'),
  ('electronics', 13, 'AeroTune Earbuds Pro', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 599900, 34, 5, FALSE, 'wireless', 'music'),
  ('electronics', 14, 'VoltLink Hub Dock', 'https://images.unsplash.com/photo-1580894894513-541e068a3e2b?auto=format&fit=crop&w=900&q=80', 299900, 41, 5, FALSE, 'workspace', 'usb-c'),
  ('electronics', 15, 'Lumio Desk Light', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 249900, 29, 5, FALSE, 'lighting', 'desk'),
  ('electronics', 16, 'StreamCast TV Stick', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80', 449900, 24, 5, FALSE, 'streaming', 'tv'),
  ('electronics', 17, 'VisionBell Door Cam', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 549900, 18, 5, FALSE, 'security', 'smart-home'),
  ('electronics', 18, 'GlidePad Wireless Combo', 'https://images.unsplash.com/photo-1580894894513-541e068a3e2b?auto=format&fit=crop&w=900&q=80', 399900, 30, 5, FALSE, 'workspace', 'wireless'),
  ('electronics', 19, 'PulseBeam Webcam', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 329900, 32, 5, FALSE, 'webcam', 'creator'),
  ('electronics', 20, 'SkySync Router Max', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80', 899900, 15, 5, FALSE, 'wifi', 'network'),
  ('electronics', 21, 'EchoCharge Power Station', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 999900, 12, 5, FALSE, 'power', 'travel'),
  ('electronics', 22, 'SonicFlow Neckband', 'https://images.unsplash.com/photo-1580894894513-541e068a3e2b?auto=format&fit=crop&w=900&q=80', 349900, 28, 5, FALSE, 'audio', 'commute'),
  ('electronics', 23, 'AeroView Tablet Stand', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 179900, 39, 5, FALSE, 'tablet', 'workspace'),
  ('electronics', 24, 'VoltSafe Surge Strip', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80', 149900, 46, 5, FALSE, 'safety', 'utility'),
  ('electronics', 25, 'Lumio Bedside Speaker', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 519900, 20, 5, FALSE, 'audio', 'bedroom'),
  ('electronics', 26, 'StreamMic Creator Kit', 'https://images.unsplash.com/photo-1580894894513-541e068a3e2b?auto=format&fit=crop&w=900&q=80', 699900, 17, 5, FALSE, 'creator', 'streaming'),
  ('electronics', 27, 'VisionSense Smart Display', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 1299900, 11, 5, FALSE, 'smart-home', 'display'),
  ('electronics', 28, 'GlideType Mechanical Keys', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80', 489900, 23, 5, FALSE, 'keyboard', 'mechanical'),
  ('electronics', 29, 'PulseCharge Mag Pad', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', 199900, 35, 5, FALSE, 'charging', 'wireless'),
  ('electronics', 30, 'SkyCast Mini Projector', 'https://images.unsplash.com/photo-1580894894513-541e068a3e2b?auto=format&fit=crop&w=900&q=80', 1099900, 14, 5, FALSE, 'projector', 'portable');

INSERT INTO seed_product_expansion (category_slug, seq_no, title, cover_image, price_cents, stock_qty, low_stock_threshold, is_backorder_allowed, tag_secondary, tag_tertiary) VALUES
  ('fashion', 11, 'Urban Ease Tee Pack', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80', 119900, 44, 5, FALSE, 'cotton', 'casual'),
  ('fashion', 12, 'Weekend Denim Shirt', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80', 189900, 23, 5, FALSE, 'denim', 'casual'),
  ('fashion', 13, 'CloudSoft Zip Hoodie', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80', 219900, 19, 5, FALSE, 'hoodie', 'comfort'),
  ('fashion', 14, 'Linen Drift Kurta', 'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80', 169900, 24, 5, FALSE, 'linen', 'ethnic'),
  ('fashion', 15, 'Metro Tailor Trousers', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80', 209900, 21, 5, FALSE, 'office-wear', 'formal'),
  ('fashion', 16, 'Breeze Bloom Dress', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80', 259900, 18, 5, FALSE, 'dress', 'occasion'),
  ('fashion', 17, 'Tailored Edge Suit Set', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80', 459900, 9, 5, FALSE, 'formal', 'workwear'),
  ('fashion', 18, 'Everyday Motion Trackpants', 'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80', 149900, 31, 5, FALSE, 'athleisure', 'weekend'),
  ('fashion', 19, 'Classic Stripe Polo', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80', 139900, 28, 5, FALSE, 'polo', 'smart-casual'),
  ('fashion', 20, 'SoftWeave Winter Sweater', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80', 229900, 16, 5, FALSE, 'knitwear', 'layering'),
  ('fashion', 21, 'Urban Flex Cargo Pants', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80', 199900, 22, 5, FALSE, 'streetwear', 'travel'),
  ('fashion', 22, 'Weekend Layer Shirt', 'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80', 159900, 27, 5, FALSE, 'casual', 'layering'),
  ('fashion', 23, 'Cloud Lounge Coord Set', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80', 279900, 14, 5, FALSE, 'comfort', 'lounge'),
  ('fashion', 24, 'Linen Harbor Shirt', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80', 179900, 26, 5, FALSE, 'summer', 'linen'),
  ('fashion', 25, 'Metro Office Dress', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80', 239900, 17, 5, FALSE, 'workwear', 'office-wear'),
  ('fashion', 26, 'Breeze Festive Saree', 'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80', 389900, 12, 5, FALSE, 'festive', 'ethnic'),
  ('fashion', 27, 'Tailored Comfort Waistcoat', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80', 189900, 20, 5, FALSE, 'formal', 'layering'),
  ('fashion', 28, 'Everyday Essential Tee', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80', 99900, 48, 5, FALSE, 'cotton', 'basics'),
  ('fashion', 29, 'Classic Knit Top', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80', 149900, 25, 5, FALSE, 'knitwear', 'smart-casual'),
  ('fashion', 30, 'SoftWeave Lounge Pants', 'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80', 159900, 29, 5, FALSE, 'lounge', 'comfort'),
  ('accessories', 11, 'ChronoLite Watch SE', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 359900, 21, 5, FALSE, 'smartwatch', 'fitness'),
  ('accessories', 12, 'Leather Fold Card Case', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80', 99900, 40, 5, FALSE, 'wallet', 'leather'),
  ('accessories', 13, 'Urban Trek Backpack', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80', 249900, 24, 5, FALSE, 'bag', 'travel'),
  ('accessories', 14, 'AeroTint Sunglasses', 'https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80', 139900, 27, 5, FALSE, 'sunglasses', 'outdoor'),
  ('accessories', 15, 'LoopFit Smart Band', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 179900, 30, 5, FALSE, 'wearable', 'health'),
  ('accessories', 16, 'Travel Cube Organizer', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80', 89900, 38, 5, FALSE, 'organizer', 'travel'),
  ('accessories', 17, 'Minimal MagSafe Stand', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80', 129900, 33, 5, FALSE, 'phone-stand', 'minimal'),
  ('accessories', 18, 'SlimFlex Belt', 'https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80', 109900, 29, 5, FALSE, 'belt', 'workwear'),
  ('accessories', 19, 'Everyday Canvas Cap', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 69900, 41, 5, FALSE, 'cap', 'streetwear'),
  ('accessories', 20, 'Aurora Pearl Bracelet', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80', 149900, 18, 5, FALSE, 'jewelry', 'gift'),
  ('accessories', 21, 'ChronoFit Sport Strap', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80', 79900, 36, 5, FALSE, 'smartwatch', 'wearable'),
  ('accessories', 22, 'Leather Zip Passport Case', 'https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80', 119900, 31, 5, FALSE, 'travel', 'leather'),
  ('accessories', 23, 'Urban Tech Messenger', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 279900, 19, 5, FALSE, 'bag', 'workspace'),
  ('accessories', 24, 'AeroShield Case Pro', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80', 159900, 34, 5, FALSE, 'phone-case', 'protection'),
  ('accessories', 25, 'LoopBand Yoga Strap', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80', 59900, 45, 5, FALSE, 'fitness', 'wellness'),
  ('accessories', 26, 'Travel Snap Luggage Tag', 'https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80', 49900, 52, 5, FALSE, 'travel', 'organizer'),
  ('accessories', 27, 'Minimal Desk Pouch', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 89900, 37, 5, FALSE, 'organizer', 'workspace'),
  ('accessories', 28, 'SlimCard Metal Wallet', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80', 169900, 22, 5, FALSE, 'wallet', 'edc'),
  ('accessories', 29, 'Everyday Beanie', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80', 64900, 39, 5, FALSE, 'winter', 'casual'),
  ('accessories', 30, 'Aurora Gift Set', 'https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80', 199900, 16, 5, FALSE, 'jewelry', 'premium');

INSERT INTO seed_product_expansion (category_slug, seq_no, title, cover_image, price_cents, stock_qty, low_stock_threshold, is_backorder_allowed, tag_secondary, tag_tertiary) VALUES
  ('home', 11, 'HearthGlow Floor Lamp', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80', 399900, 18, 5, FALSE, 'lighting', 'decor'),
  ('home', 12, 'PurePour Kettle', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80', 149900, 34, 5, FALSE, 'kitchen', 'utility'),
  ('home', 13, 'CozyNest Accent Cushion Duo', 'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80', 129900, 27, 5, FALSE, 'decor', 'comfort'),
  ('home', 14, 'GreenLeaf Planter Duo', 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?auto=format&fit=crop&w=900&q=80', 99900, 32, 5, FALSE, 'decor', 'garden'),
  ('home', 15, 'FreshPress Coffee Maker', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80', 229900, 19, 5, FALSE, 'coffee', 'kitchen'),
  ('home', 16, 'SoftRest Quilt Cover', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80', 189900, 23, 5, FALSE, 'bedding', 'comfort'),
  ('home', 17, 'EcoPrep Storage Jars', 'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80', 119900, 36, 5, FALSE, 'kitchen', 'organization'),
  ('home', 18, 'NovaShelf Desk Rack', 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?auto=format&fit=crop&w=900&q=80', 159900, 25, 5, FALSE, 'workspace', 'organization'),
  ('home', 19, 'CleanFlow Spin Mop', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80', 139900, 29, 5, FALSE, 'cleaning', 'utility'),
  ('home', 20, 'AromaMist Diffuser', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80', 169900, 21, 5, FALSE, 'fragrance', 'wellness'),
  ('home', 21, 'HearthGlow Wall Sconce', 'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80', 249900, 14, 5, FALSE, 'lighting', 'decor'),
  ('home', 22, 'PureServe Glass Pitcher', 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?auto=format&fit=crop&w=900&q=80', 89900, 42, 5, FALSE, 'dining', 'kitchen'),
  ('home', 23, 'CozyBath Towel Set', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80', 149900, 28, 5, FALSE, 'bath', 'comfort'),
  ('home', 24, 'GreenLeaf Watering Can', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80', 79900, 33, 5, FALSE, 'garden', 'utility'),
  ('home', 25, 'FreshBake Oven Tray Set', 'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80', 109900, 37, 5, FALSE, 'kitchen', 'baking'),
  ('home', 26, 'SoftFold Mattress Topper', 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?auto=format&fit=crop&w=900&q=80', 329900, 11, 5, FALSE, 'bedding', 'premium'),
  ('home', 27, 'EcoServe Lunch Box Set', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80', 99900, 40, 5, FALSE, 'dining', 'travel'),
  ('home', 28, 'NovaDesk Cable Box', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80', 69900, 48, 5, FALSE, 'workspace', 'organization'),
  ('home', 29, 'CleanEase Brush Kit', 'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80', 59900, 51, 5, FALSE, 'cleaning', 'utility'),
  ('home', 30, 'AromaHome Reed Diffuser', 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?auto=format&fit=crop&w=900&q=80', 129900, 24, 5, FALSE, 'fragrance', 'decor'),
  ('books', 11, 'Growth Mindset Journal', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80', 44900, 60, 5, FALSE, 'self-growth', 'journaling'),
  ('books', 12, 'Spring Boot Field Guide', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80', 94900, 28, 5, FALSE, 'programming', 'java'),
  ('books', 13, 'Modern Retail Analytics', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80', 84900, 22, 5, FALSE, 'business', 'analytics'),
  ('books', 14, 'Design Systems Handbook', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80', 89900, 20, 5, FALSE, 'design', 'reference'),
  ('books', 15, 'Founder Finance Workbook', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80', 79900, 24, 5, FALSE, 'finance', 'startup'),
  ('books', 16, 'Data Storytelling Playbook', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80', 87900, 19, 5, FALSE, 'analytics', 'communication'),
  ('books', 17, 'Daily English Practice', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80', 52900, 47, 5, FALSE, 'language', 'study'),
  ('books', 18, 'Focus Study Planner', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80', 49900, 53, 5, FALSE, 'students', 'productivity'),
  ('books', 19, 'Java Clean Architecture', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80', 104900, 17, 5, FALSE, 'coding', 'best-practices'),
  ('books', 20, 'AI Playbook for Teams', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80', 92900, 21, 5, FALSE, 'ai', 'leadership'),
  ('books', 21, 'Product Management Notes', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80', 81900, 23, 5, FALSE, 'product', 'business'),
  ('books', 22, 'UX Research Starter', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80', 73900, 26, 5, FALSE, 'design', 'research'),
  ('books', 23, 'Market Positioning Guide', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80', 68900, 31, 5, FALSE, 'marketing', 'strategy'),
  ('books', 24, 'Sales Conversations Toolkit', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80', 65900, 29, 5, FALSE, 'sales', 'communication'),
  ('books', 25, 'Personal Branding Blueprint', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80', 62900, 34, 5, FALSE, 'career', 'self-growth'),
  ('books', 26, 'Deep Work Study Guide', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80', 57900, 38, 5, FALSE, 'productivity', 'students'),
  ('books', 27, 'Python Data Recipes', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80', 97900, 18, 5, FALSE, 'programming', 'analytics'),
  ('books', 28, 'Creative Thinking Workbook', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80', 55900, 42, 5, FALSE, 'creativity', 'education'),
  ('books', 29, 'Ecommerce Growth Manual', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80', 88900, 20, 5, FALSE, 'ecommerce', 'business'),
  ('books', 30, 'Team Leadership Essentials', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80', 76900, 27, 5, FALSE, 'leadership', 'management');

INSERT INTO seed_product_expansion (category_slug, seq_no, title, cover_image, price_cents, stock_qty, low_stock_threshold, is_backorder_allowed, tag_secondary, tag_tertiary) VALUES
  ('shoes', 11, 'SprintFlex Runner Pro', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', 379900, 15, 5, FALSE, 'running', 'performance'),
  ('shoes', 12, 'CityMove Knit Sneakers', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', 299900, 24, 5, FALSE, 'casual', 'comfort'),
  ('shoes', 13, 'TrailEdge Trek Boots', 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=900&q=80', 429900, 13, 5, FALSE, 'hiking', 'outdoor'),
  ('shoes', 14, 'CourtDrive Trainers', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80', 319900, 18, 5, FALSE, 'training', 'gym'),
  ('shoes', 15, 'AeroRun Marathon Shoes', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', 409900, 14, 5, FALSE, 'running', 'lightweight'),
  ('shoes', 16, 'StreetBeat Mid Tops', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', 309900, 20, 5, FALSE, 'streetwear', 'high-top'),
  ('shoes', 17, 'TerraWalk Sandals', 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=900&q=80', 189900, 28, 5, FALSE, 'sandals', 'travel'),
  ('shoes', 18, 'OfficeForm Penny Loafers', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80', 279900, 19, 5, FALSE, 'formal', 'office'),
  ('shoes', 19, 'CloudEase Slip Sneakers', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', 229900, 25, 5, FALSE, 'slip-on', 'comfort'),
  ('shoes', 20, 'PaceDrive Sports Shoes', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', 349900, 16, 5, FALSE, 'sports', 'performance'),
  ('shoes', 21, 'SprintMesh Joggers', 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=900&q=80', 289900, 23, 5, FALSE, 'running', 'breathable'),
  ('shoes', 22, 'CityClassic Canvas Shoes', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80', 249900, 27, 5, FALSE, 'casual', 'daily'),
  ('shoes', 23, 'TrailPeak Trek Sandals', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', 199900, 21, 5, FALSE, 'outdoor', 'sandals'),
  ('shoes', 24, 'CourtCore Indoor Shoes', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', 299900, 17, 5, FALSE, 'gym', 'indoor'),
  ('shoes', 25, 'AeroSprint Flex Shoes', 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=900&q=80', 369900, 18, 5, FALSE, 'running', 'speed'),
  ('shoes', 26, 'StreetMode Canvas High Tops', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80', 319900, 20, 5, FALSE, 'streetwear', 'canvas'),
  ('shoes', 27, 'TerraGrip Water Sandals', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80', 179900, 26, 5, FALSE, 'travel', 'outdoor'),
  ('shoes', 28, 'OfficeEase Derby Shoes', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', 329900, 15, 5, FALSE, 'formal', 'leather'),
  ('shoes', 29, 'CloudStep Recovery Slides', 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=900&q=80', 149900, 32, 5, FALSE, 'comfort', 'slides'),
  ('shoes', 30, 'PacePro Court Shoes', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80', 339900, 17, 5, FALSE, 'sports', 'court'),
  ('gaming', 11, 'ArcPulse Wired Controller', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80', 349900, 21, 5, FALSE, 'controller', 'console'),
  ('gaming', 12, 'Quantum Switch Keys', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80', 389900, 24, 5, FALSE, 'keyboard', 'rgb'),
  ('gaming', 13, 'Phantom Aim Mouse', 'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?auto=format&fit=crop&w=900&q=80', 269900, 28, 5, FALSE, 'mouse', 'esports'),
  ('gaming', 14, 'PulseTalk Headset X', 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=900&q=80', 329900, 22, 5, FALSE, 'headset', 'voice-chat'),
  ('gaming', 15, 'NovaStream Capture Hub', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80', 749900, 12, 5, FALSE, 'streaming', 'creator'),
  ('gaming', 16, 'TurboDock Charge Base', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80', 239900, 27, 5, FALSE, 'charging', 'console'),
  ('gaming', 17, 'PixelForge Footrest', 'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?auto=format&fit=crop&w=900&q=80', 199900, 18, 5, FALSE, 'setup', 'comfort'),
  ('gaming', 18, 'ShadowPad Speed Mat', 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=900&q=80', 119900, 39, 5, FALSE, 'deskmat', 'precision'),
  ('gaming', 19, 'RiftView Curved Monitor', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80', 2199900, 7, 5, FALSE, 'monitor', '165hz'),
  ('gaming', 20, 'HexCore Arcade Stick', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80', 499900, 14, 5, FALSE, 'console', 'fighting'),
  ('gaming', 21, 'ArcNova RGB Speakers', 'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?auto=format&fit=crop&w=900&q=80', 289900, 25, 5, FALSE, 'audio', 'setup'),
  ('gaming', 22, 'Quantum Macro Pad', 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=900&q=80', 159900, 30, 5, FALSE, 'keyboard', 'streaming'),
  ('gaming', 23, 'Phantom Glide Mouse Bungee', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80', 89900, 44, 5, FALSE, 'mouse', 'desk-setup'),
  ('gaming', 24, 'PulseCast Streaming Mic', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80', 429900, 19, 5, FALSE, 'creator', 'voice'),
  ('gaming', 25, 'NovaPlay Handheld Dock', 'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?auto=format&fit=crop&w=900&q=80', 329900, 16, 5, FALSE, 'handheld', 'charging'),
  ('gaming', 26, 'TurboCool Laptop Stand', 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=900&q=80', 179900, 33, 5, FALSE, 'setup', 'cooling'),
  ('gaming', 27, 'PixelView Webcam Light', 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80', 249900, 26, 5, FALSE, 'streaming', 'lighting'),
  ('gaming', 28, 'ShadowZone Cable Kit', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80', 69900, 46, 5, FALSE, 'setup', 'organization'),
  ('gaming', 29, 'RiftFrame Dual Monitor Arm', 'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?auto=format&fit=crop&w=900&q=80', 399900, 13, 5, FALSE, 'monitor', 'workspace'),
  ('gaming', 30, 'HexCore RGB Desk Tower', 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=900&q=80', 549900, 11, 5, FALSE, 'setup', 'rgb');

INSERT INTO seed_products (category_slug, brand_slug, title, slug, description, cover_image, sku, variant_name, price_cents, stock_qty, low_stock_threshold, is_backorder_allowed, tag_primary, tag_secondary, tag_tertiary)
SELECT spe.category_slug,
       CASE spe.category_slug
         WHEN 'mobiles' THEN 'volt-mobile'
         WHEN 'electronics' THEN 'sonivo'
         WHEN 'fashion' THEN 'urbanloom'
         WHEN 'accessories' THEN 'modora'
         WHEN 'home' THEN 'nestique'
         WHEN 'books' THEN 'penverse'
         WHEN 'shoes' THEN 'stridecraft'
         WHEN 'gaming' THEN 'arcnova-gaming'
       END,
       spe.title,
       REPLACE(LOWER(spe.title), ' ', '-'),
       CONCAT(spe.title, ' is a NexBuy-ready ', spe.category_slug, ' pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.'),
       spe.cover_image,
       CONCAT(
         CASE spe.category_slug
           WHEN 'mobiles' THEN 'MOB'
           WHEN 'electronics' THEN 'ELE'
           WHEN 'fashion' THEN 'FAS'
           WHEN 'accessories' THEN 'ACC'
           WHEN 'home' THEN 'HOM'
           WHEN 'books' THEN 'BOO'
           WHEN 'shoes' THEN 'SHO'
           WHEN 'gaming' THEN 'GAM'
         END,
         '-',
         LPAD(spe.seq_no, 3, '0')
       ),
       spe.title,
       spe.price_cents,
       spe.stock_qty,
       spe.low_stock_threshold,
       spe.is_backorder_allowed,
       spe.category_slug,
       spe.tag_secondary,
       spe.tag_tertiary
FROM seed_product_expansion spe;

DROP TEMPORARY TABLE IF EXISTS seed_product_expansion;

UPDATE seed_products
SET description = CASE MOD(CAST(SUBSTRING_INDEX(sku, '-', -1) AS UNSIGNED), 5)
    WHEN 0 THEN CONCAT(
        title, ' brings ', REPLACE(tag_secondary, '-', ' '), ' focus together with ',
        REPLACE(tag_tertiary, '-', ' '), ' for shoppers comparing ', REPLACE(category_slug, '-', ' '),
        ' options with live stock and everyday value in mind.'
    )
    WHEN 1 THEN CONCAT(
        'Built around ', REPLACE(tag_secondary, '-', ' '), ', ', title, ' adds ',
        REPLACE(tag_tertiary, '-', ' '), ' appeal in a ', REPLACE(category_slug, '-', ' '),
        ' listing designed for quick discovery and confident checkout decisions.'
    )
    WHEN 2 THEN CONCAT(
        title, ' balances ', REPLACE(tag_secondary, '-', ' '), ' with ',
        REPLACE(tag_tertiary, '-', ' '), ' so buyers can shortlist a ',
        REPLACE(category_slug, '-', ' '), ' pick that feels considered rather than generic.'
    )
    WHEN 3 THEN CONCAT(
        'For customers exploring ', REPLACE(category_slug, '-', ' '), ' essentials, ', title,
        ' stands out with ', REPLACE(tag_secondary, '-', ' '), ', ',
        REPLACE(tag_tertiary, '-', ' '), ', and a storefront-ready presentation.'
    )
    ELSE CONCAT(
        title, ' is shaped for buyers who care about ', REPLACE(tag_secondary, '-', ' '), ' and ',
        REPLACE(tag_tertiary, '-', ' '), ', making it a sharper ', REPLACE(category_slug, '-', ' '),
        ' addition for browsing, comparison, and conversion.'
    )
END,
    cover_image = CONCAT(
        'https://picsum.photos/seed/',
        category_slug,
        '-',
        LPAD(CAST(SUBSTRING_INDEX(sku, '-', -1) AS UNSIGNED), 3, '0'),
        '-cover/900/900.jpg'
    );

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

INSERT INTO product_variants (product_id, sku, name, price_cents, compare_at_cents, currency, created_at, updated_at)
SELECT p.id, sp.sku, sp.variant_name, sp.price_cents, ROUND(sp.price_cents * 1.15), 'INR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seed_products sp
JOIN products p ON p.slug = sp.slug
ON DUPLICATE KEY UPDATE
  product_id = VALUES(product_id),
  name = VALUES(name),
  price_cents = VALUES(price_cents),
  compare_at_cents = VALUES(compare_at_cents),
  currency = VALUES(currency),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO inventory (variant_id, stock_qty, low_stock_threshold, is_backorder_allowed, updated_at)
SELECT pv.id, sp.stock_qty, sp.low_stock_threshold, sp.is_backorder_allowed, CURRENT_TIMESTAMP
FROM seed_products sp
JOIN product_variants pv ON pv.sku = sp.sku
ON DUPLICATE KEY UPDATE
  stock_qty = VALUES(stock_qty),
  low_stock_threshold = VALUES(low_stock_threshold),
  is_backorder_allowed = VALUES(is_backorder_allowed),
  updated_at = CURRENT_TIMESTAMP;

DELETE pm FROM product_media pm
JOIN products p ON p.id = pm.product_id
JOIN seed_products sp ON sp.slug = p.slug;

INSERT INTO product_media (product_id, url, alt_text, sort_order)
SELECT p.id, sp.cover_image, CONCAT(sp.title, ' cover image'), 0
FROM seed_products sp
JOIN products p ON p.slug = sp.slug;

INSERT INTO product_media (product_id, url, alt_text, sort_order)
SELECT p.id,
       CONCAT(
         'https://picsum.photos/seed/',
         sp.category_slug,
         '-',
         LPAD(CAST(SUBSTRING_INDEX(sp.sku, '-', -1) AS UNSIGNED), 3, '0'),
         '-gallery-2/900/900.jpg'
       ),
       CONCAT(sp.title, ' gallery image 2'),
       1
FROM seed_products sp
JOIN products p ON p.slug = sp.slug;

INSERT INTO product_media (product_id, url, alt_text, sort_order)
SELECT p.id,
       CONCAT(
         'https://picsum.photos/seed/',
         sp.category_slug,
         '-',
         LPAD(CAST(SUBSTRING_INDEX(sp.sku, '-', -1) AS UNSIGNED), 3, '0'),
         '-gallery-3/900/900.jpg'
       ),
       CONCAT(sp.title, ' gallery image 3'),
       2
FROM seed_products sp
JOIN products p ON p.slug = sp.slug;

DELETE pt FROM product_tags pt
JOIN products p ON p.id = pt.product_id
JOIN seed_products sp ON sp.slug = p.slug;

INSERT INTO product_tags (product_id, tag)
SELECT p.id, sp.tag_primary FROM seed_products sp JOIN products p ON p.slug = sp.slug;

INSERT INTO product_tags (product_id, tag)
SELECT p.id, sp.tag_secondary FROM seed_products sp JOIN products p ON p.slug = sp.slug;

INSERT INTO product_tags (product_id, tag)
SELECT p.id, sp.tag_tertiary FROM seed_products sp JOIN products p ON p.slug = sp.slug;

DROP TEMPORARY TABLE IF EXISTS seed_products;
