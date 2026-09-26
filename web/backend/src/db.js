/* لایه دیتابیس — better-sqlite3 (SQL واقعی، فایل‌محور) */
const path = require('path');
const fs = require('fs');
const Database = require('better-sqlite3');

// ATOM_DATA_DIR برای تست‌ها و استقرارهایی که داده را بیرون از پوشهٔ کد نگه می‌دارند
const DATA_DIR = process.env.ATOM_DATA_DIR || path.join(__dirname, '..', 'data');
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });

const db = new Database(path.join(DATA_DIR, 'atom.db'));
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
CREATE TABLE IF NOT EXISTS admins (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  name TEXT,
  email TEXT,
  role TEXT NOT NULL DEFAULT 'support',   -- admin | editor | support
  status TEXT NOT NULL DEFAULT 'active',  -- active | suspended | blocked
  last_login TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS articles (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  slug TEXT,
  category TEXT,
  author TEXT,
  excerpt TEXT,
  cover TEXT,
  body TEXT,
  status TEXT NOT NULL DEFAULT 'draft',    -- draft | published
  views INTEGER NOT NULL DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS products (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  category TEXT,
  seller TEXT,
  price INTEGER NOT NULL DEFAULT 0,
  stock INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'active',
  image TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS customers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  phone TEXT,
  email TEXT,
  city TEXT,
  total_spent INTEGER NOT NULL DEFAULT 0,
  orders_count INTEGER NOT NULL DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS orders (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT,
  customer TEXT,
  product TEXT,
  seller TEXT,
  amount INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'pending',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT
);

CREATE TABLE IF NOT EXISTS feature_flags (
  key TEXT PRIMARY KEY,
  name TEXT,
  enabled INTEGER NOT NULL DEFAULT 0,
  description TEXT
);

CREATE TABLE IF NOT EXISTS activity_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  actor TEXT,
  action TEXT,
  target TEXT,
  ip TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS login_attempts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT,
  ip TEXT,
  success INTEGER NOT NULL DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS categories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  slug TEXT,
  parent TEXT,
  count INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'active',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS sellers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  category TEXT,
  city TEXT,
  rating REAL NOT NULL DEFAULT 5,
  sales INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'active',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS offers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT NOT NULL,
  title TEXT,
  kind TEXT DEFAULT 'percent',
  amount INTEGER NOT NULL DEFAULT 0,
  used INTEGER NOT NULL DEFAULT 0,
  quota INTEGER NOT NULL DEFAULT 0,
  expires TEXT,
  status TEXT NOT NULL DEFAULT 'active',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS comments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  author TEXT,
  product TEXT,
  body TEXT,
  rating INTEGER NOT NULL DEFAULT 5,
  status TEXT NOT NULL DEFAULT 'pending',   -- pending | approved | rejected
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sender TEXT,
  subject TEXT,
  body TEXT,
  status TEXT NOT NULL DEFAULT 'open',       -- open | pending | closed
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS faqs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  question TEXT NOT NULL,
  answer TEXT,
  sort INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'active',      -- active | hidden
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS chat_messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session TEXT,
  role TEXT,                                  -- user | assistant
  content TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS slides (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  eyebrow TEXT,
  title TEXT NOT NULL,
  subtitle TEXT,
  image TEXT,
  cta_label TEXT,
  cta_link TEXT,
  sort INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'active',       -- active | hidden
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS banned_words (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  word TEXT NOT NULL,
  note TEXT,
  status TEXT NOT NULL DEFAULT 'active',        -- active | off
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS offices (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,                            -- نام دفتر محله
  manager TEXT,                                  -- مسئول دفتر
  area TEXT,                                     -- محله / منطقه
  city TEXT,
  address TEXT,
  phone TEXT,
  national_code TEXT,                            -- کد ملی مسئول (اعتبارسنجی)
  license_no TEXT,                               -- شمارهٔ مجوز
  bio TEXT,
  avatar TEXT,
  owner TEXT,                                    -- نام‌کاربری حساب دفتر
  verified INTEGER NOT NULL DEFAULT 0,           -- ۰ = تأییدنشده، ۱ = تأییدشده
  status TEXT NOT NULL DEFAULT 'pending',        -- pending | verified | rejected | blocked
  verify_note TEXT,                              -- یادداشت اعتبارسنجی (دلیل رد)
  rating REAL NOT NULL DEFAULT 5,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS office_services (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  category TEXT,
  office TEXT,                                   -- نام دفتر
  description TEXT,
  price INTEGER NOT NULL DEFAULT 0,
  owner TEXT,                                    -- نام‌کاربری حساب دفتر
  status TEXT NOT NULL DEFAULT 'active',         -- active | inactive
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS office_messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  owner TEXT,                                   -- نام‌کاربری حساب دفتر گیرنده
  office TEXT,                                   -- نام دفتر
  sender_name TEXT,
  sender_phone TEXT,
  body TEXT,
  reply TEXT,                                   -- پاسخ دفتر
  status TEXT NOT NULL DEFAULT 'open',          -- open | replied | closed
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS ads (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  placement TEXT NOT NULL DEFAULT 'top',        -- top (نوار بالای سایت) | corner (مربع گوشهٔ چپ)
  title TEXT,
  text TEXT,
  image TEXT,
  link TEXT,
  cta_label TEXT,
  bg TEXT,
  sort INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'active',         -- active | hidden
  created_at TEXT DEFAULT (datetime('now'))
);
`);

/* ---------- مهاجرت‌های سبک: افزودن ستون‌ها به جدول‌های موجود ---------- */
function addColumn(table, col, def) {
  try { db.exec(`ALTER TABLE ${table} ADD COLUMN ${col} ${def}`); } catch (e) { /* ستون از قبل هست */ }
}
addColumn('products', 'owner', 'TEXT');            // نام‌کاربری فروشندهٔ صاحب محصول
addColumn('products', 'description', 'TEXT');      // توضیحات محصول
addColumn('sellers', 'owner', 'TEXT');             // نام‌کاربری حساب فروشنده
addColumn('sellers', 'bio', 'TEXT');
addColumn('sellers', 'avatar', 'TEXT');
addColumn('sellers', 'phone', 'TEXT');
addColumn('admins', 'seller_name', 'TEXT');        // نام فروشگاه برای نقش فروشنده
addColumn('admins', 'phone', 'TEXT');
addColumn('admins', 'office_name', 'TEXT');        // نام دفتر برای نقش office
addColumn('admins', 'city', 'TEXT');               // شهر (حساب خریدار)
addColumn('admins', 'address', 'TEXT');            // آدرس پیش‌فرض ارسال (حساب خریدار)
addColumn('orders', 'phone', 'TEXT');              // موبایل گیرنده (ثبت سفارش از سایت)
addColumn('orders', 'address', 'TEXT');            // آدرس ارسال
addColumn('orders', 'owner', 'TEXT');              // نام‌کاربری مشتری (اگر وارد شده باشد)
addColumn('orders', 'note', 'TEXT');               // توضیحات سفارش (کد تخفیف، هزینهٔ ارسال، یادداشت خریدار)
addColumn('orders', 'product_id', 'INTEGER');      // شناسهٔ محصول (برای «محصولات من» در اپ و سایت)
addColumn('orders', 'qty', 'INTEGER NOT NULL DEFAULT 1');
addColumn('messages', 'owner', 'TEXT');            // نام‌کاربری فرستنده (اگر وارد شده باشد)
addColumn('messages', 'reply', 'TEXT');            // پاسخ پشتیبانی — در حساب کاربر (سایت و اپ) نمایش داده می‌شود
addColumn('office_messages', 'sender_owner', 'TEXT'); // نام‌کاربری شهروند فرستنده (اگر وارد شده باشد)

/* ---------- جدول‌های API نسخهٔ ۱ (اپ اندروید و همگام‌سازی سایت) ---------- */
db.exec(`
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  token_hash TEXT UNIQUE NOT NULL,              -- SHA-256 توکن؛ خود توکن هرگز ذخیره نمی‌شود
  family TEXT NOT NULL,                          -- زنجیرهٔ چرخش؛ استفادهٔ مجدد = ابطال کل زنجیره
  device TEXT,
  expires_at TEXT NOT NULL,
  revoked_at TEXT,
  replaced_by INTEGER,
  created_at TEXT DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_refresh_user ON refresh_tokens(user_id);

CREATE TABLE IF NOT EXISTS wishlist (
  user_id INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  created_at TEXT DEFAULT (datetime('now')),
  PRIMARY KEY (user_id, product_id)
);

CREATE TABLE IF NOT EXISTS notifications (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  type TEXT NOT NULL,                            -- order_placed | order_status | support_reply | office_reply | account
  title TEXT NOT NULL,
  body TEXT,
  ref TEXT,                                      -- مرجع (کد سفارش، شناسهٔ پیام…) برای باز کردن صفحهٔ مربوط
  read_at TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_notif_user ON notifications(user_id, id);

CREATE TABLE IF NOT EXISTS devices (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  token TEXT NOT NULL,
  platform TEXT NOT NULL DEFAULT 'android',
  provider TEXT NOT NULL DEFAULT 'none',         -- fcm | none (دریافت با همگام‌سازی دوره‌ای)
  app_version TEXT,
  updated_at TEXT DEFAULT (datetime('now')),
  UNIQUE (token)
);

CREATE TABLE IF NOT EXISTS idempotency_keys (
  key TEXT NOT NULL,
  scope TEXT NOT NULL,                           -- نام‌کاربری یا ip:… برای مهمان
  response TEXT NOT NULL,
  status INTEGER NOT NULL,
  created_at TEXT DEFAULT (datetime('now')),
  PRIMARY KEY (key, scope)
);
`);

module.exports = db;
