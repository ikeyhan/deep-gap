/* v1 — دادهٔ عمومی فروشگاه: پیکربندی، خانه، محصولات، فروشندگان، دفاتر، بلاگ، سؤالات متداول */
const express = require('express');
const rateLimit = require('express-rate-limit');
const db = require('../../db');
const { str, int, isMobile } = require('../../validate');
const banned = require('../../banned');
const orderService = require('../../orders-service');
const { ApiError, bad, notFound, sendError, paging, paged, publicCache, noStore, toEn } = require('../http');
const { appAuth, optionalAuth } = require('../middleware');

const router = express.Router();

const setting = (k, d) => orderService.setting(k, d);

/* ---------- نگاشت محصول ---------- */
const PRODUCT_COLS = `p.id,p.title,p.category,p.seller,p.price,p.stock,p.image,p.description,p.created_at,
  (SELECT id FROM sellers s WHERE s.name=p.seller AND s.status='active' ORDER BY s.id LIMIT 1) AS seller_id,
  (SELECT AVG(rating) FROM comments c WHERE c.status='approved' AND c.product=p.title) AS rating,
  (SELECT COUNT(*) FROM comments c WHERE c.status='approved' AND c.product=p.title) AS reviews`;

function product(r, withDescription) {
  const out = {
    id: r.id, title: r.title, category: r.category || '', seller: r.seller || '', sellerId: r.seller_id || null,
    price: r.price, stock: r.stock, inStock: r.stock > 0, image: r.image || null,
    rating: r.rating ? Math.round(r.rating * 10) / 10 : null, reviewCount: r.reviews || 0, createdAt: r.created_at,
  };
  if (withDescription) out.description = r.description || '';
  return out;
}

// نرمال‌سازی حروف عربی/فارسی برای جستجو
const norm = (s) => toEn(s).replace(/ي/g, 'ی').replace(/ك/g, 'ک').replace(/‌/g, ' ').trim();

/* ---------- پیکربندی اپ ---------- */
function config(req, res) {
  const keys = ['site_name', 'contact_email', 'contact_phone', 'address', 'social_instagram', 'social_telegram',
    'social_whatsapp', 'social_linkedin', 'site_description', 'domain'];
  const s = {}; keys.forEach(k => { s[k] = setting(k, ''); });
  res.json({
    site: {
      name: s.site_name || 'اتم ۳۱۳', description: s.site_description, domain: s.domain,
      email: s.contact_email, phone: s.contact_phone, address: s.address,
      social: { instagram: s.social_instagram, telegram: s.social_telegram, whatsapp: s.social_whatsapp, linkedin: s.social_linkedin },
    },
    commerce: {
      shippingCost: orderService.numSetting('shipping_cost', 0),
      freeShippingMin: orderService.numSetting('free_shipping_min', 500000),
      minOrder: orderService.numSetting('min_order', 0),
      // درگاه پرداخت آنلاین هنوز به سایت متصل نیست؛ پرداخت پس از ثبت سفارش هماهنگ می‌شود
      onlinePayment: false,
      paymentNote: 'پس از ثبت سفارش، هماهنگی پرداخت و ارسال با شما انجام می‌شود.',
    },
    support: {
      chatEnabled: setting('chat_enabled', '1') === '1',
      aiEnabled: !!setting('ai_api_key', ''),
      title: setting('chat_title', 'پشتیبانی اتم'),
      welcome: setting('chat_welcome', ''),
    },
    registrationOpen: setting('registration', '1') !== '0',
    maintenance: setting('maintenance', '0') === '1',
    android: {
      minVersionCode: parseInt(setting('android_min_version', '1'), 10) || 1,
      latestVersionCode: parseInt(setting('android_latest_version', '1'), 10) || 1,
    },
  });
}
router.get('/config', noStore, config);

/* ---------- خانه: یک درخواست برای کل صفحه (کاهش رفت‌وبرگشت شبکه) ---------- */
router.get('/home', publicCache(60), (req, res) => {
  const slides = db.prepare("SELECT id,eyebrow,title,subtitle,image,cta_label,cta_link FROM slides WHERE status='active' ORDER BY sort ASC, id ASC LIMIT 8").all()
    .map(s => ({ id: s.id, eyebrow: s.eyebrow || '', title: s.title, subtitle: s.subtitle || '', image: s.image || null, ctaLabel: s.cta_label || '', ctaLink: s.cta_link || '' }));
  const topAd = db.prepare("SELECT id,title,text,image,link,cta_label FROM ads WHERE status='active' AND placement='top' ORDER BY sort,id LIMIT 1").get();
  res.json({
    slides,
    announcement: topAd ? { id: topAd.id, title: topAd.title || '', text: topAd.text || '', link: topAd.link || '', ctaLabel: topAd.cta_label || '' } : null,
    categories: categories(12),
    newest: db.prepare(`SELECT ${PRODUCT_COLS} FROM products p WHERE p.status='active' ORDER BY p.id DESC LIMIT 10`).all().map(r => product(r)),
    popular: db.prepare(`SELECT ${PRODUCT_COLS}, (SELECT COUNT(*) FROM orders o WHERE o.product_id=p.id) AS sold
      FROM products p WHERE p.status='active' AND p.stock>0 ORDER BY sold DESC, p.price DESC LIMIT 10`).all().map(r => product(r)),
    sellers: db.prepare("SELECT id,name,category,city,rating,sales,avatar FROM sellers WHERE status='active' ORDER BY sales DESC, id DESC LIMIT 10").all().map(seller),
    offices: db.prepare("SELECT id,name,manager,area,city,avatar,rating FROM offices WHERE status='verified' ORDER BY rating DESC, id DESC LIMIT 6").all().map(office),
    articles: db.prepare("SELECT id,title,category,author,excerpt,cover,views,created_at FROM articles WHERE status='published' ORDER BY id DESC LIMIT 4").all().map(article),
  });
});

function categories(limit) {
  return db.prepare("SELECT category AS name, COUNT(*) AS count FROM products WHERE status='active' AND category<>'' GROUP BY category ORDER BY count DESC, category LIMIT ?")
    .all(limit).map(c => ({ name: c.name, count: c.count }));
}
router.get('/categories', publicCache(300), (req, res) => res.json({ items: categories(100) }));

/* ---------- محصولات ---------- */
router.get('/products', publicCache(30), (req, res) => {
  const p = paging(req, 20);
  const wh = ["p.status='active'"], args = [];
  const q = norm(str(req.query.q, 100));
  q.split(/\s+/).filter(Boolean).slice(0, 6).forEach(w => {
    wh.push("(p.title LIKE ? OR p.description LIKE ? OR p.category LIKE ? OR p.seller LIKE ?)");
    const like = '%' + w.replace(/[%_]/g, '') + '%'; args.push(like, like, like, like);
  });
  if (req.query.category) { wh.push('p.category=?'); args.push(str(req.query.category, 80)); }
  if (req.query.sellerId) {
    const s = db.prepare("SELECT name FROM sellers WHERE id=? AND status='active'").get(int(req.query.sellerId));
    wh.push('p.seller=?'); args.push(s ? s.name : '\u0000');
  }
  if (req.query.minPrice) { wh.push('p.price>=?'); args.push(int(toEn(req.query.minPrice))); }
  if (req.query.maxPrice) { wh.push('p.price<=?'); args.push(int(toEn(req.query.maxPrice))); }
  if (req.query.inStock === '1') wh.push('p.stock>0');
  const order = {
    newest: 'p.id DESC',
    price_asc: 'p.price ASC, p.id DESC',
    price_desc: 'p.price DESC, p.id DESC',
    popular: '(SELECT COUNT(*) FROM orders o WHERE o.product_id=p.id) DESC, p.id DESC',
    rating: 'rating IS NULL, rating DESC, p.id DESC',
  }[req.query.sort] || 'p.id DESC';
  const where = 'WHERE ' + wh.join(' AND ');
  const total = db.prepare(`SELECT COUNT(*) c FROM products p ${where}`).get(...args).c;
  const items = db.prepare(`SELECT ${PRODUCT_COLS} FROM products p ${where} ORDER BY ${order} LIMIT ? OFFSET ?`)
    .all(...args, p.limit, p.offset).map(r => product(r));
  res.json(paged(items, total, p));
});

router.get('/products/:id', publicCache(30), (req, res) => {
  const r = db.prepare(`SELECT ${PRODUCT_COLS}, p.status FROM products p WHERE p.id=?`).get(int(req.params.id));
  if (!r || r.status !== 'active') throw notFound('این محصول دیگر در دسترس نیست.');
  const reviews = db.prepare("SELECT id,author,body,rating,created_at FROM comments WHERE status='approved' AND product=? ORDER BY id DESC LIMIT 20")
    .all(r.title).map(c => ({ id: c.id, author: c.author, body: c.body, rating: c.rating, createdAt: c.created_at }));
  const dist = [1, 2, 3, 4, 5].map(k => db.prepare("SELECT COUNT(*) c FROM comments WHERE status='approved' AND product=? AND rating=?").get(r.title, k).c);
  const related = db.prepare(`SELECT ${PRODUCT_COLS} FROM products p WHERE p.status='active' AND p.id<>? AND (p.category=? OR p.seller=?) ORDER BY p.stock>0 DESC, p.id DESC LIMIT 8`)
    .all(r.id, r.category || '', r.seller || '').map(x => product(x));
  res.json({ product: product(r, true), reviews, ratingDistribution: dist, related });
});

// ثبت نظر — در اپ فقط با حساب کاربری (نام از حساب خوانده می‌شود) و پس از تأیید مدیر نمایش داده می‌شود
const reviewLimiter = rateLimit({ windowMs: 10 * 60 * 1000, max: 10, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'تعداد نظرات ارسالی بیش از حد مجاز است. کمی بعد تلاش کنید.')) });
router.post('/products/:id/reviews', noStore, appAuth, reviewLimiter, (req, res) => {
  const p = db.prepare("SELECT title FROM products WHERE id=? AND status='active'").get(int(req.params.id));
  if (!p) throw notFound('این محصول دیگر در دسترس نیست.');
  const body = str(req.body.body, 2000).trim();
  const rating = Math.max(1, Math.min(5, int(req.body.rating, 5)));
  if (body.length < 5) throw bad('invalid_input', 'متن نظر را کامل‌تر بنویسید (حداقل ۵ نویسه).', { body: 'حداقل ۵ نویسه' });
  const w = banned.findBanned(body);
  if (w) throw bad('banned_word', 'متن شامل کلمهٔ غیرمجاز «' + w + '» است.');
  db.prepare("INSERT INTO comments (author,product,body,rating,status) VALUES (?,?,?,?,'pending')").run(req.user.name || req.user.username, p.title, body, rating);
  res.status(201).json({ ok: true, status: 'pending' });
});

/* ---------- فروشندگان ---------- */
function seller(s) {
  return { id: s.id, name: s.name, category: s.category || '', city: s.city || '', rating: s.rating, sales: s.sales, avatar: s.avatar || null, bio: s.bio };
}
router.get('/sellers', publicCache(120), (req, res) => {
  const p = paging(req, 20);
  const q = norm(str(req.query.q, 80));
  const wh = "status='active'" + (q ? ' AND (name LIKE ? OR category LIKE ? OR city LIKE ?)' : '');
  const args = q ? ['%' + q + '%', '%' + q + '%', '%' + q + '%'] : [];
  const total = db.prepare(`SELECT COUNT(*) c FROM sellers WHERE ${wh}`).get(...args).c;
  const items = db.prepare(`SELECT id,name,category,city,rating,sales,avatar FROM sellers WHERE ${wh} ORDER BY sales DESC, id DESC LIMIT ? OFFSET ?`)
    .all(...args, p.limit, p.offset).map(seller);
  res.json(paged(items, total, p));
});
router.get('/sellers/:id', publicCache(60), (req, res) => {
  const s = db.prepare("SELECT id,name,category,city,rating,sales,avatar,bio,created_at FROM sellers WHERE id=? AND status='active'").get(int(req.params.id));
  if (!s) throw notFound('این فروشگاه یافت نشد.');
  const productCount = db.prepare("SELECT COUNT(*) c FROM products WHERE seller=? AND status='active'").get(s.name).c;
  res.json({ seller: { ...seller(s), bio: s.bio || '', createdAt: s.created_at, productCount } });
});

/* ---------- دفاتر محلات ---------- */
function office(o) {
  return { id: o.id, name: o.name, manager: o.manager || '', area: o.area || '', city: o.city || '', avatar: o.avatar || null, rating: o.rating };
}
router.get('/offices', publicCache(120), (req, res) => {
  const p = paging(req, 20);
  const q = norm(str(req.query.q, 80));
  const wh = "status='verified'" + (q ? ' AND (name LIKE ? OR area LIKE ? OR city LIKE ? OR manager LIKE ?)' : '');
  const args = q ? Array(4).fill('%' + q + '%') : [];
  const total = db.prepare(`SELECT COUNT(*) c FROM offices WHERE ${wh}`).get(...args).c;
  const items = db.prepare(`SELECT id,name,manager,area,city,avatar,rating FROM offices WHERE ${wh} ORDER BY rating DESC, id DESC LIMIT ? OFFSET ?`)
    .all(...args, p.limit, p.offset).map(office);
  res.json(paged(items, total, p));
});
router.get('/offices/:id', publicCache(60), (req, res) => {
  const o = db.prepare("SELECT id,name,manager,area,city,address,phone,bio,avatar,rating,owner,created_at FROM offices WHERE id=? AND status='verified'").get(int(req.params.id));
  if (!o) throw notFound('این دفتر یافت نشد یا هنوز تأیید نشده است.');
  const services = db.prepare("SELECT id,title,category,description,price FROM office_services WHERE status='active' AND owner=? ORDER BY id DESC")
    .all(o.owner || '\u0000').map(s => ({ id: s.id, title: s.title, category: s.category || '', description: s.description || '', price: s.price }));
  // نام‌کاربری حساب دفتر هرگز افشا نمی‌شود
  res.json({ office: { ...office(o), address: o.address || '', phone: o.phone || '', bio: o.bio || '', createdAt: o.created_at }, services });
});

const officeMsgLimiter = rateLimit({ windowMs: 10 * 60 * 1000, max: 10, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'تعداد پیام‌ها بیش از حد مجاز است. کمی بعد تلاش کنید.')) });
router.post('/offices/:id/messages', noStore, optionalAuth, officeMsgLimiter, (req, res) => {
  const o = db.prepare("SELECT id,name,owner FROM offices WHERE id=? AND status='verified'").get(int(req.params.id));
  if (!o) throw notFound('این دفتر یافت نشد یا هنوز تأیید نشده است.');
  const me = req.user ? db.prepare('SELECT name,phone FROM admins WHERE id=?').get(req.user.id) : null;
  const name = str(req.body.name, 120).trim() || (me && me.name) || 'شهروند';
  const phone = toEn(str(req.body.phone, 20)).replace(/[^0-9]/g, '') || (me && me.phone) || '';
  const body = str(req.body.body, 4000).trim();
  const fields = {};
  if (body.length < 3) fields.body = 'متن پیام را بنویسید.';
  if (!me && !isMobile(phone)) fields.phone = 'برای دریافت پاسخ، شمارهٔ موبایل معتبر وارد کنید.';
  if (Object.keys(fields).length) throw bad('invalid_input', 'لطفاً خطاهای فرم را برطرف کنید.', fields);
  const w = banned.findBanned(name, body);
  if (w) throw bad('banned_word', 'متن شامل کلمهٔ غیرمجاز «' + w + '» است.');
  db.prepare('INSERT INTO office_messages (owner,office,sender_name,sender_phone,body,status,sender_owner) VALUES (?,?,?,?,?,?,?)')
    .run(o.owner || '', o.name, name, phone, body, 'open', req.user ? req.user.username : '');
  res.status(201).json({ ok: true });
});

/* ---------- بلاگ و سؤالات متداول ---------- */
function article(a) {
  return { id: a.id, title: a.title, category: a.category || '', author: a.author || '', excerpt: a.excerpt || '', cover: a.cover || null, views: a.views, createdAt: a.created_at };
}
router.get('/articles', publicCache(300), (req, res) => {
  const p = paging(req, 10);
  const total = db.prepare("SELECT COUNT(*) c FROM articles WHERE status='published'").get().c;
  const items = db.prepare("SELECT id,title,category,author,excerpt,cover,views,created_at FROM articles WHERE status='published' ORDER BY id DESC LIMIT ? OFFSET ?")
    .all(p.limit, p.offset).map(article);
  res.json(paged(items, total, p));
});
router.get('/articles/:id', publicCache(300), (req, res) => {
  const a = db.prepare("SELECT * FROM articles WHERE id=? AND status='published'").get(int(req.params.id));
  if (!a) throw notFound('این مطلب یافت نشد.');
  db.prepare('UPDATE articles SET views=views+1 WHERE id=?').run(a.id);
  // بدنه HTML است؛ اپ آن را بدون اجرای اسکریپت (متن غنی بومی) نمایش می‌دهد
  res.json({ article: { ...article(a), body: a.body || '' } });
});
router.get('/faqs', publicCache(300), (req, res) => {
  res.json({ items: db.prepare("SELECT id,question,answer FROM faqs WHERE status='active' ORDER BY sort,id").all() });
});

/* ---------- پیگیری سفارش مهمان ---------- */
const trackLimiter = rateLimit({ windowMs: 10 * 60 * 1000, max: 30, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'درخواست‌های بیش از حد مجاز. کمی بعد تلاش کنید.')) });
router.get('/track', noStore, trackLimiter, (req, res) => {
  const code = toEn(str(req.query.code, 40)).replace(/[^0-9]/g, '');
  const phone = toEn(str(req.query.phone, 20)).replace(/[^0-9]/g, '');
  if (!code || !isMobile(phone)) throw bad('invalid_input', 'کد سفارش و شمارهٔ موبایل معتبر را وارد کنید.');
  const rows = db.prepare('SELECT * FROM orders WHERE code=? AND phone=? ORDER BY id').all(code, phone);
  if (!rows.length) throw notFound('سفارشی با این مشخصات یافت نشد.');
  res.json({ order: require('./orders').group(rows, { includeAddress: false }) });
});

module.exports = { router, config, product, PRODUCT_COLS };
