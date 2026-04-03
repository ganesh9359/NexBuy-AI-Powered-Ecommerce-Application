$ErrorActionPreference = 'Stop'

function SqlLiteral([string]$value) {
  if ($null -eq $value) { return 'NULL' }
  return "'" + $value.Replace("'", "''") + "'"
}

function Slugify([string]$value) {
  $slug = $value.ToLowerInvariant() -replace '[^a-z0-9]+', '-' -replace '^-+|-+$', ''
  if ([string]::IsNullOrWhiteSpace($slug)) { return 'item' }
  return $slug
}

$categories = @(
  @{
    Name='Mobiles'; Slug='mobiles'; BrandName='Volt Mobile'; BrandSlug='volt-mobile';
    Description='Smartphones and daily-carry mobile tech for work, study, and entertainment.';
    Image='https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80';
    ImagePool=@(
      'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80'
    );
    Products=@(
      @{ Title='Volt X1 Pro 5G'; Price=4299900; Stock=18; Tag2='5g'; Tag3='camera' },
      @{ Title='Nova S Max'; Price=3899900; Stock=14; Tag2='amoled'; Tag3='fast-charge' },
      @{ Title='Aero Lite 5G'; Price=2499900; Stock=27; Tag2='lightweight'; Tag3='student-pick' },
      @{ Title='PixelEdge Mini'; Price=2999900; Stock=11; Tag2='compact'; Tag3='travel' },
      @{ Title='Horizon Fold Air'; Price=7999900; Stock=6; Tag2='foldable'; Tag3='premium' },
      @{ Title='Spark Note 12'; Price=2199900; Stock=22; Tag2='big-display'; Tag3='battery' },
      @{ Title='Atlas Ultra Camera'; Price=5599900; Stock=8; Tag2='camera'; Tag3='creator' },
      @{ Title='Neo Core 5G'; Price=1899900; Stock=31; Tag2='budget'; Tag3='daily-use' },
      @{ Title='Pulse M6'; Price=2799900; Stock=16; Tag2='gaming'; Tag3='performance' },
      @{ Title='Orbit Go 5G'; Price=1599900; Stock=25; Tag2='value'; Tag3='starter-phone' }
    )
  },
  @{
    Name='Electronics'; Slug='electronics'; BrandName='Sonivo'; BrandSlug='sonivo';
    Description='Connected electronics, audio gear, and smart devices for the modern home office.';
    Image='https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80';
    ImagePool=@(
      'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1580894894513-541e068a3e2b?auto=format&fit=crop&w=900&q=80'
    );
    Products=@(
      @{ Title='EchoBeam Soundbar'; Price=1299900; Stock=19; Tag2='audio'; Tag3='home-theater' },
      @{ Title='SonicDock Speaker'; Price=699900; Stock=24; Tag2='bluetooth'; Tag3='portable' },
      @{ Title='AeroPods Studio'; Price=849900; Stock=20; Tag2='wireless'; Tag3='music' },
      @{ Title='VoltHub Smart Plug'; Price=199900; Stock=40; Tag2='smart-home'; Tag3='energy-save' },
      @{ Title='Lumio Air Purifier'; Price=999900; Stock=12; Tag2='air-care'; Tag3='quiet' },
      @{ Title='StreamBox 4K'; Price=549900; Stock=17; Tag2='streaming'; Tag3='tv' },
      @{ Title='VisionCam Home'; Price=459900; Stock=23; Tag2='security'; Tag3='smart-home' },
      @{ Title='Glide Keyboard'; Price=349900; Stock=28; Tag2='workspace'; Tag3='wireless' },
      @{ Title='Pulse Mouse Pro'; Price=259900; Stock=33; Tag2='productivity'; Tag3='ergonomic' },
      @{ Title='SkyWave Projector'; Price=1899900; Stock=9; Tag2='projector'; Tag3='cinema' }
    )
  },
  @{
    Name='Fashion'; Slug='fashion'; BrandName='UrbanLoom'; BrandSlug='urbanloom';
    Description='Versatile apparel for everyday wear, office hours, and weekend plans.';
    Image='https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80';
    ImagePool=@(
      'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80'
    );
    Products=@(
      @{ Title='Urban Cotton Tee'; Price=89900; Stock=46; Tag2='cotton'; Tag3='casual' },
      @{ Title='Weekend Denim Jacket'; Price=249900; Stock=18; Tag2='denim'; Tag3='layering' },
      @{ Title='CloudFit Hoodie'; Price=189900; Stock=22; Tag2='hoodie'; Tag3='comfort' },
      @{ Title='Linen Ease Shirt'; Price=159900; Stock=20; Tag2='linen'; Tag3='summer' },
      @{ Title='Metro Chino Pants'; Price=179900; Stock=24; Tag2='office-wear'; Tag3='smart-casual' },
      @{ Title='Breeze Kurti Set'; Price=229900; Stock=17; Tag2='ethnic'; Tag3='festive' },
      @{ Title='Tailored Office Blazer'; Price=349900; Stock=10; Tag2='formal'; Tag3='workwear' },
      @{ Title='Everyday Joggers'; Price=139900; Stock=29; Tag2='athleisure'; Tag3='weekend' },
      @{ Title='Classic Polo Shirt'; Price=129900; Stock=26; Tag2='polo'; Tag3='smart-casual' },
      @{ Title='SoftWeave Cardigan'; Price=199900; Stock=15; Tag2='knitwear'; Tag3='layering' }
    )
  },
  @{
    Name='Accessories'; Slug='accessories'; BrandName='Modora'; BrandSlug='modora';
    Description='Functional accessories designed to complement travel, work, and daily routines.';
    Image='https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80';
    ImagePool=@(
      'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80'
    );
    Products=@(
      @{ Title='ChronoFit Smartwatch'; Price=499900; Stock=19; Tag2='smartwatch'; Tag3='fitness' },
      @{ Title='Leather Snap Wallet'; Price=149900; Stock=34; Tag2='wallet'; Tag3='leather' },
      @{ Title='Urban Sling Bag'; Price=189900; Stock=21; Tag2='bag'; Tag3='travel' },
      @{ Title='AeroShield Sunglasses'; Price=129900; Stock=28; Tag2='sunglasses'; Tag3='outdoor' },
      @{ Title='LoopBand Fitness Band'; Price=249900; Stock=24; Tag2='wearable'; Tag3='health' },
      @{ Title='Travel Zip Pouch'; Price=79900; Stock=38; Tag2='organizer'; Tag3='travel' },
      @{ Title='Minimal Key Organizer'; Price=69900; Stock=42; Tag2='edc'; Tag3='minimal' },
      @{ Title='Slim Metal Belt'; Price=99900; Stock=25; Tag2='belt'; Tag3='workwear' },
      @{ Title='Everyday Cap'; Price=59900; Stock=47; Tag2='cap'; Tag3='streetwear' },
      @{ Title='Aurora Pendant'; Price=159900; Stock=14; Tag2='jewelry'; Tag3='gift' }
    )
  },
  @{
    Name='Home'; Slug='home'; BrandName='Nestique'; BrandSlug='nestique';
    Description='Home essentials, kitchen helpers, and decor pieces that make spaces feel finished.';
    Image='https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80';
    ImagePool=@(
      'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1484101403633-562f891dc89a?auto=format&fit=crop&w=900&q=80'
    );
    Products=@(
      @{ Title='HearthGlow Lamp'; Price=219900; Stock=16; Tag2='lighting'; Tag3='decor' },
      @{ Title='CozyNest Cushion Set'; Price=149900; Stock=22; Tag2='soft-furnishing'; Tag3='living-room' },
      @{ Title='SteamChef Kettle'; Price=179900; Stock=18; Tag2='kitchen'; Tag3='appliance' },
      @{ Title='PureMist Diffuser'; Price=129900; Stock=27; Tag2='wellness'; Tag3='aroma' },
      @{ Title='FreshBrew French Press'; Price=99900; Stock=31; Tag2='coffee'; Tag3='kitchen' },
      @{ Title='SoftFold Blanket'; Price=169900; Stock=20; Tag2='bedding'; Tag3='comfort' },
      @{ Title='EcoServe Dinner Set'; Price=249900; Stock=13; Tag2='dining'; Tag3='tableware' },
      @{ Title='NovaDesk Organizer'; Price=89900; Stock=35; Tag2='organization'; Tag3='workspace' },
      @{ Title='CleanSweep Mop Kit'; Price=119900; Stock=28; Tag2='cleaning'; Tag3='utility' },
      @{ Title='AromaJar Candle Pack'; Price=79900; Stock=40; Tag2='fragrance'; Tag3='gift' }
    )
  },
  @{
    Name='Books'; Slug='books'; BrandName='PenVerse'; BrandSlug='penverse';
    Description='Skill-building, business, and personal growth titles for learning-focused readers.';
    Image='https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80';
    ImagePool=@(
      'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80'
    );
    Products=@(
      @{ Title='Build Better Habits'; Price=59900; Stock=51; Tag2='self-growth'; Tag3='paperback' },
      @{ Title='Modern Java Playbook'; Price=89900; Stock=33; Tag2='programming'; Tag3='reference' },
      @{ Title='Retail Strategy Simplified'; Price=74900; Stock=26; Tag2='business'; Tag3='strategy' },
      @{ Title='Design Thinking Notes'; Price=64900; Stock=29; Tag2='design'; Tag3='innovation' },
      @{ Title='Startup Finance Basics'; Price=69900; Stock=25; Tag2='finance'; Tag3='startup' },
      @{ Title='Mastering Data Stories'; Price=79900; Stock=21; Tag2='analytics'; Tag3='communication' },
      @{ Title='Everyday English Guide'; Price=49900; Stock=45; Tag2='language'; Tag3='study' },
      @{ Title='Productive Study Methods'; Price=54900; Stock=38; Tag2='education'; Tag3='students' },
      @{ Title='Clean Code Companion'; Price=99900; Stock=19; Tag2='coding'; Tag3='best-practices' },
      @{ Title='AI for Business Teams'; Price=84900; Stock=24; Tag2='ai'; Tag3='leadership' }
    )
  },
  @{
    Name='Shoes'; Slug='shoes'; BrandName='StrideCraft'; BrandSlug='stridecraft';
    Description='Running, casual, and outdoor footwear built for comfort across daily use cases.';
    Image='https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80';
    ImagePool=@(
      'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80'
    );
    Products=@(
      @{ Title='SprintRun Mesh Sneakers'; Price=329900; Stock=18; Tag2='running'; Tag3='mesh' },
      @{ Title='CityWalk Casual Shoes'; Price=279900; Stock=23; Tag2='casual'; Tag3='daily' },
      @{ Title='TrailPeak Hiking Shoes'; Price=399900; Stock=14; Tag2='hiking'; Tag3='outdoor' },
      @{ Title='CourtFlex Trainers'; Price=309900; Stock=19; Tag2='training'; Tag3='gym' },
      @{ Title='AeroStride Running Shoes'; Price=359900; Stock=17; Tag2='running'; Tag3='lightweight' },
      @{ Title='StreetMode High Tops'; Price=289900; Stock=21; Tag2='streetwear'; Tag3='high-top' },
      @{ Title='TerraGrip Sandals'; Price=169900; Stock=30; Tag2='sandals'; Tag3='travel' },
      @{ Title='OfficeEase Loafers'; Price=249900; Stock=20; Tag2='formal'; Tag3='office' },
      @{ Title='CloudStep Slip-ons'; Price=199900; Stock=27; Tag2='slip-on'; Tag3='comfort' },
      @{ Title='PacePro Sports Shoes'; Price=339900; Stock=16; Tag2='sports'; Tag3='performance' }
    )
  },
  @{
    Name='Gaming'; Slug='gaming'; BrandName='ArcNova Gaming'; BrandSlug='arcnova-gaming';
    Description='Gaming gear and setup essentials for streaming, competitive play, and comfort.';
    Image='https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80';
    ImagePool=@(
      'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?auto=format&fit=crop&w=900&q=80',
      'https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=900&q=80'
    );
    Products=@(
      @{ Title='ArcStorm Controller'; Price=399900; Stock=18; Tag2='controller'; Tag3='console' },
      @{ Title='Quantum RGB Keyboard'; Price=349900; Stock=22; Tag2='keyboard'; Tag3='rgb' },
      @{ Title='Phantom Gaming Mouse'; Price=249900; Stock=26; Tag2='mouse'; Tag3='esports' },
      @{ Title='PulseWave Headset'; Price=299900; Stock=20; Tag2='headset'; Tag3='voice-chat' },
      @{ Title='NovaPlay Capture Card'; Price=699900; Stock=9; Tag2='streaming'; Tag3='creator' },
      @{ Title='TurboCharge Dock'; Price=219900; Stock=24; Tag2='charging'; Tag3='console' },
      @{ Title='PixelForge Gaming Chair'; Price=1249900; Stock=7; Tag2='chair'; Tag3='setup' },
      @{ Title='ShadowPad XXL Mouse Mat'; Price=99900; Stock=34; Tag2='deskmat'; Tag3='rgb' },
      @{ Title='RiftView 144Hz Monitor'; Price=1899900; Stock=8; Tag2='monitor'; Tag3='144hz' },
      @{ Title='HexCore Mini Console'; Price=1599900; Stock=11; Tag2='console'; Tag3='family-gaming' }
    )
  }
)

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('USE nexbuy_db;')
$lines.Add('')
$lines.Add('-- Storefront-aligned catalog seed: 8 categories, 80 products, media, inventory, and tags.')
$lines.Add('')

$categoryValues = foreach ($category in $categories) {
  '  (' + (@((SqlLiteral $category.Name), (SqlLiteral $category.Slug), (SqlLiteral $category.Description), (SqlLiteral $category.Image)) -join ', ') + ')'
}
$lines.Add('INSERT INTO categories (name, slug, description, image_url) VALUES')
$lines.Add(($categoryValues -join ",`n") + "`nON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), image_url = VALUES(image_url);")
$lines.Add('')

$brandValues = foreach ($category in $categories) {
  '  (' + (@((SqlLiteral $category.BrandName), (SqlLiteral $category.BrandSlug)) -join ', ') + ')'
}
$lines.Add('INSERT INTO brands (name, slug) VALUES')
$lines.Add(($brandValues -join ",`n") + "`nON DUPLICATE KEY UPDATE name = VALUES(name);")
$lines.Add('')

$lines.Add('DROP TEMPORARY TABLE IF EXISTS seed_products;')
$lines.Add('CREATE TEMPORARY TABLE seed_products (')
$lines.Add('  category_slug VARCHAR(150) NOT NULL,')
$lines.Add('  brand_slug VARCHAR(150) NOT NULL,')
$lines.Add('  title VARCHAR(180) NOT NULL,')
$lines.Add('  slug VARCHAR(200) NOT NULL,')
$lines.Add('  description TEXT NOT NULL,')
$lines.Add('  cover_image VARCHAR(255) NOT NULL,')
$lines.Add('  sku VARCHAR(120) NOT NULL,')
$lines.Add('  variant_name VARCHAR(180) NOT NULL,')
$lines.Add('  price_cents INT NOT NULL,')
$lines.Add('  stock_qty INT NOT NULL,')
$lines.Add('  low_stock_threshold INT NOT NULL DEFAULT 5,')
$lines.Add('  is_backorder_allowed BOOLEAN NOT NULL DEFAULT FALSE,')
$lines.Add('  tag_primary VARCHAR(80) NOT NULL,')
$lines.Add('  tag_secondary VARCHAR(80) NOT NULL,')
$lines.Add('  tag_tertiary VARCHAR(80) NOT NULL')
$lines.Add(') ENGINE=InnoDB;')
$lines.Add('')

$productRows = [System.Collections.Generic.List[string]]::new()
$catalogCount = 0
foreach ($category in $categories) {
  for ($i = 0; $i -lt $category.Products.Count; $i++) {
    $product = $category.Products[$i]
    $image = $category.ImagePool[$i % $category.ImagePool.Count]
    $slug = Slugify $product.Title
    $skuPrefix = $category.Slug.Substring(0, [Math]::Min(3, $category.Slug.Length)).ToUpperInvariant()
    $sku = '{0}-{1:000}' -f $skuPrefix, ($i + 1)
    $description = '{0} is a NexBuy-ready {1} pick built for reliable everyday performance, polished styling, and strong value for fast-moving catalog listings.' -f $product.Title, $category.Name.ToLowerInvariant()
    $backorder = if ($product.Stock -le 10) { 'TRUE' } else { 'FALSE' }
    $rowValues = @(
      (SqlLiteral $category.Slug),
      (SqlLiteral $category.BrandSlug),
      (SqlLiteral $product.Title),
      (SqlLiteral $slug),
      (SqlLiteral $description),
      (SqlLiteral $image),
      (SqlLiteral $sku),
      (SqlLiteral $product.Title),
      $product.Price,
      $product.Stock,
      5,
      $backorder,
      (SqlLiteral $category.Slug),
      (SqlLiteral $product.Tag2),
      (SqlLiteral $product.Tag3)
    )
    $productRows.Add('  (' + ($rowValues -join ', ') + ')')
    $catalogCount++
  }
}
$lines.Add('INSERT INTO seed_products (category_slug, brand_slug, title, slug, description, cover_image, sku, variant_name, price_cents, stock_qty, low_stock_threshold, is_backorder_allowed, tag_primary, tag_secondary, tag_tertiary) VALUES')
$lines.Add(($productRows -join ",`n") + ';')
$lines.Add('')

$lines.Add('INSERT INTO products (category_id, brand_id, title, slug, description, cover_image, status)')
$lines.Add('SELECT c.id, b.id, sp.title, sp.slug, sp.description, sp.cover_image, ''active''')
$lines.Add('FROM seed_products sp')
$lines.Add('JOIN categories c ON c.slug = sp.category_slug')
$lines.Add('JOIN brands b ON b.slug = sp.brand_slug')
$lines.Add('ON DUPLICATE KEY UPDATE')
$lines.Add('  category_id = VALUES(category_id),')
$lines.Add('  brand_id = VALUES(brand_id),')
$lines.Add('  title = VALUES(title),')
$lines.Add('  description = VALUES(description),')
$lines.Add('  cover_image = VALUES(cover_image),')
$lines.Add('  status = VALUES(status),')
$lines.Add('  updated_at = CURRENT_TIMESTAMP;')
$lines.Add('')

$lines.Add('INSERT INTO product_variants (product_id, sku, name, price_cents, compare_at_cents, currency, created_at, updated_at)')
$lines.Add('SELECT p.id, sp.sku, sp.variant_name, sp.price_cents, ROUND(sp.price_cents * 1.15), ''INR'', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP')
$lines.Add('FROM seed_products sp')
$lines.Add('JOIN products p ON p.slug = sp.slug')
$lines.Add('ON DUPLICATE KEY UPDATE')
$lines.Add('  product_id = VALUES(product_id),')
$lines.Add('  name = VALUES(name),')
$lines.Add('  price_cents = VALUES(price_cents),')
$lines.Add('  compare_at_cents = VALUES(compare_at_cents),')
$lines.Add('  currency = VALUES(currency),')
$lines.Add('  updated_at = CURRENT_TIMESTAMP;')
$lines.Add('')

$lines.Add('INSERT INTO inventory (variant_id, stock_qty, low_stock_threshold, is_backorder_allowed, updated_at)')
$lines.Add('SELECT pv.id, sp.stock_qty, sp.low_stock_threshold, sp.is_backorder_allowed, CURRENT_TIMESTAMP')
$lines.Add('FROM seed_products sp')
$lines.Add('JOIN product_variants pv ON pv.sku = sp.sku')
$lines.Add('ON DUPLICATE KEY UPDATE')
$lines.Add('  stock_qty = VALUES(stock_qty),')
$lines.Add('  low_stock_threshold = VALUES(low_stock_threshold),')
$lines.Add('  is_backorder_allowed = VALUES(is_backorder_allowed),')
$lines.Add('  updated_at = CURRENT_TIMESTAMP;')
$lines.Add('')

$lines.Add('DELETE pm FROM product_media pm')
$lines.Add('JOIN products p ON p.id = pm.product_id')
$lines.Add('JOIN seed_products sp ON sp.slug = p.slug;')
$lines.Add('')

$lines.Add('INSERT INTO product_media (product_id, url, alt_text, sort_order)')
$lines.Add('SELECT p.id, sp.cover_image, CONCAT(sp.title, '' cover image''), 0')
$lines.Add('FROM seed_products sp')
$lines.Add('JOIN products p ON p.slug = sp.slug;')
$lines.Add('')
$lines.Add('INSERT INTO product_media (product_id, url, alt_text, sort_order)')
$lines.Add('SELECT p.id,')
$lines.Add('       CASE sp.category_slug')
$lines.Add('         WHEN ''mobiles'' THEN ''https://images.unsplash.com/photo-1556656793-08538906a9f8?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''electronics'' THEN ''https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''fashion'' THEN ''https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''accessories'' THEN ''https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''home'' THEN ''https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''books'' THEN ''https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''shoes'' THEN ''https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''gaming'' THEN ''https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80''')
$lines.Add('       END,')
$lines.Add('       CONCAT(sp.title, '' gallery image 2''),')
$lines.Add('       1')
$lines.Add('FROM seed_products sp')
$lines.Add('JOIN products p ON p.slug = sp.slug;')
$lines.Add('')
$lines.Add('INSERT INTO product_media (product_id, url, alt_text, sort_order)')
$lines.Add('SELECT p.id,')
$lines.Add('       CASE sp.category_slug')
$lines.Add('         WHEN ''mobiles'' THEN ''https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''electronics'' THEN ''https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''fashion'' THEN ''https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''accessories'' THEN ''https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''home'' THEN ''https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''books'' THEN ''https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''shoes'' THEN ''https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=900&q=80''')
$lines.Add('         WHEN ''gaming'' THEN ''https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=900&q=80''')
$lines.Add('       END,')
$lines.Add('       CONCAT(sp.title, '' gallery image 3''),')
$lines.Add('       2')
$lines.Add('FROM seed_products sp')
$lines.Add('JOIN products p ON p.slug = sp.slug;')
$lines.Add('')

$lines.Add('DELETE pt FROM product_tags pt')
$lines.Add('JOIN products p ON p.id = pt.product_id')
$lines.Add('JOIN seed_products sp ON sp.slug = p.slug;')
$lines.Add('')

$lines.Add('INSERT INTO product_tags (product_id, tag)')
$lines.Add('SELECT p.id, sp.tag_primary FROM seed_products sp JOIN products p ON p.slug = sp.slug;')
$lines.Add('')
$lines.Add('INSERT INTO product_tags (product_id, tag)')
$lines.Add('SELECT p.id, sp.tag_secondary FROM seed_products sp JOIN products p ON p.slug = sp.slug;')
$lines.Add('')
$lines.Add('INSERT INTO product_tags (product_id, tag)')
$lines.Add('SELECT p.id, sp.tag_tertiary FROM seed_products sp JOIN products p ON p.slug = sp.slug;')
$lines.Add('')
$lines.Add('DROP TEMPORARY TABLE IF EXISTS seed_products;')

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path 'db/seed.sql'), ($lines -join "`r`n") + "`r`n", $utf8NoBom)
Write-Output "seed_products_count=$catalogCount"




