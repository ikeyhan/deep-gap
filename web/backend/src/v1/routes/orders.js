/* v1 — سبد خرید، ثبت سفارش (ایمن در برابر ارسال تکراری)، سفارش‌ها و «محصولات من» */
const express = require('express');
const rateLimit = require('express-rate-limit');
const db = require('../../db');
const { clientIp } = require('../../auth');
const { str, isMobile } = require('../../validate');
const banned = require('../../banned');
const orderService = require('../../orders-service');
const { ApiError, bad, notFound, sendError, paging, paged, noStore, toEn } = require('../http');
const { appAuth, optionalAuth } = require('../middleware');

const router = express.Router();
router.use(noStore);

/* ---------- گروه‌بندی ردیف‌های سفارش (هر قلم یک ردیف با کد مشترک است) ---------- */
const PROGRESS = { pending: 0, review: 1, shipping: 2, delivered: 3 };
function groupStatus(rows) {
  const live = rows.filter(r => r.status !== 'returned');
  if (!live.length) return 'returned';
  return live.reduce((s, r) => ((PROGRESS[r.status] ?? 0) < (PROGRESS[s] ?? 0) ? r.status : s), 'delivered');
}
const cleanTitle = (t) => String(t || '').replace(/\s×\d+$/, '');
const imgStmt = () => db.prepare('SELECT image FROM products WHERE id=?');

function line(r, img) {
  const p = r.product_id ? img.get(r.product_id) : null;
  return {
    id: r.id, productId: r.product_id || null, title: cleanTitle(r.product), qty: r.qty || 1,
    seller: r.seller || '', amount: r.amount, status: r.status, image: (p && p.image) || null,
  };
}
function group(rows, { includeAddress }) {
  const img = imgStmt();
  const first = rows[0];
  const out = {
    code: first.code, status: groupStatus(rows), createdAt: first.created_at,
    total: rows.reduce((s, r) => s + (r.amount || 0), 0),
    itemCount: rows.reduce((s, r) => s + (r.qty || 1), 0),
    items: rows.map(r => line(r, img)), note: first.note || '',
  };
  if (includeAddress) { out.recipient = first.customer || ''; out.phone = first.phone || ''; out.address = first.address || ''; }
  return out;
}

// سفارش‌های کاربر فقط بر اساس مالکیت حساب. (تطبیق با شمارهٔ موبایل ناامن است چون موبایل
// تأییدشده نیست؛ سفارش مهمان با «افزودن سفارش» و اثبات کد سفارش + موبایل به حساب متصل می‌شود.)
function ownWhere(req) {
  return { sql: 'owner=?', args: [req.user.username] };
}

/* ---------- پیش‌فاکتور: قیمت‌ها، تخفیف و ارسال فقط سمت سرور ---------- */
router.post('/cart/quote', (req, res) => {
  const items = (Array.isArray(req.body.items) ? req.body.items : []).map(i => ({ productId: i && i.productId, qty: i && i.qty }));
  if (!items.length) return res.json({ lines: [], subtotal: 0, discount: 0, shipping: 0, total: 0, minOrder: 0, freeShippingMin: 0, coupon: null, couponError: null });
  const q = orderService.quote(items, req.body.coupon, { strict: true });
  res.json(q);
});

/* ---------- ثبت سفارش ---------- */
const orderLimiter = rateLimit({ windowMs: 10 * 60 * 1000, max: 20, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'تعداد درخواست‌های ثبت سفارش بیش از حد مجاز است. کمی بعد تلاش کنید.')) });

router.post('/orders', optionalAuth, orderLimiter, (req, res) => {
  // کلید یکتا از سمت اپ: ارسال دوباره (قطع شبکه، دوبار لمس) سفارش تکراری نمی‌سازد
  const key = str(req.get('Idempotency-Key'), 100).trim();
  if (!/^[A-Za-z0-9_-]{8,100}$/.test(key)) throw bad('idempotency_key_required', 'درخواست نامعتبر است. دوباره تلاش کنید.');
  const scope = req.user ? 'u:' + req.user.username : 'ip:' + clientIp(req);
  const prev = db.prepare('SELECT status,response FROM idempotency_keys WHERE key=? AND scope=?').get(key, scope);
  if (prev) return res.status(prev.status).set('Idempotent-Replay', 'true').json(JSON.parse(prev.response));

  const name = str(req.body.name, 120).trim();
  const phone = toEn(str(req.body.phone, 20)).replace(/[^0-9]/g, '');
  const city = str(req.body.city, 60).trim();
  const address = str(req.body.address, 600).trim();
  const note = str(req.body.note, 300).trim();
  const fields = {};
  if (name.length < 2) fields.name = 'نام گیرنده را وارد کنید.';
  if (!isMobile(phone)) fields.phone = 'شمارهٔ موبایل نامعتبر است (نمونه: 09123456789).';
  if (address.length < 10) fields.address = 'آدرس کامل پستی را وارد کنید.';
  if (Object.keys(fields).length) throw bad('invalid_input', 'لطفاً اطلاعات ارسال را کامل کنید.', fields);
  const w = banned.findBanned(name, address, note);
  if (w) throw bad('banned_word', 'متن شامل کلمهٔ غیرمجاز «' + w + '» است.');

  const items = (Array.isArray(req.body.items) ? req.body.items : []).map(i => ({ productId: i && i.productId, qty: i && i.qty }));
  let result;
  try {
    result = orderService.place({ items, coupon: req.body.coupon, name, phone, address, city, note,
      owner: req.user ? req.user.username : '', ip: clientIp(req), strict: true });
  } catch (e) {
    if (e instanceof orderService.OrderError) throw new ApiError(e.status, e.code, e.message);
    throw e;
  }
  const body = { order: group(db.prepare('SELECT * FROM orders WHERE code=? ORDER BY id').all(result.code), { includeAddress: true }),
    subtotal: result.subtotal, discount: result.discount, shipping: result.shipping, total: result.total };
  db.prepare('INSERT OR IGNORE INTO idempotency_keys (key,scope,response,status) VALUES (?,?,?,?)').run(key, scope, JSON.stringify(body), 201);
  res.status(201).json(body);
});

/* ---------- سفارش‌های من ---------- */
router.get('/orders', appAuth, (req, res) => {
  const p = paging(req, 20);
  const w = ownWhere(req);
  const total = db.prepare(`SELECT COUNT(DISTINCT code) c FROM orders WHERE ${w.sql}`).get(...w.args).c;
  const codes = db.prepare(`SELECT code, MAX(id) mx FROM orders WHERE ${w.sql} GROUP BY code ORDER BY mx DESC LIMIT ? OFFSET ?`)
    .all(...w.args, p.limit, p.offset).map(r => r.code);
  const rowsBy = db.prepare(`SELECT * FROM orders WHERE code=? AND ${w.sql} ORDER BY id`);
  const items = codes.map(c => group(rowsBy.all(c, ...w.args), { includeAddress: false }));
  res.json(paged(items, total, p));
});

router.get('/orders/:code', appAuth, (req, res) => {
  const w = ownWhere(req);
  const rows = db.prepare(`SELECT * FROM orders WHERE code=? AND ${w.sql} ORDER BY id`).all(toEn(str(req.params.code, 40)), ...w.args);
  // سفارش دیگران «یافت نشد» برمی‌گرداند (نه ۴۰۳) تا وجود آن افشا نشود
  if (!rows.length) throw notFound('سفارش یافت نشد.');
  res.json({ order: group(rows, { includeAddress: true }) });
});

// اتصال سفارش مهمان به حساب: همان مدرکی که برای پیگیری لازم است (کد سفارش + موبایل)
const claimLimiter = rateLimit({ windowMs: 10 * 60 * 1000, max: 10, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'درخواست‌های بیش از حد مجاز. کمی بعد تلاش کنید.')) });
router.post('/orders/claim', appAuth, claimLimiter, (req, res) => {
  const code = toEn(str(req.body.code, 40)).replace(/[^0-9]/g, '');
  const phone = toEn(str(req.body.phone, 20)).replace(/[^0-9]/g, '');
  if (!code || !isMobile(phone)) throw bad('invalid_input', 'کد سفارش و شمارهٔ موبایل معتبر را وارد کنید.');
  const rows = db.prepare('SELECT * FROM orders WHERE code=? AND phone=? ORDER BY id').all(code, phone);
  if (!rows.length) throw notFound('سفارشی با این مشخصات یافت نشد.');
  if (rows.some(r => r.owner && r.owner !== req.user.username)) throw new ApiError(409, 'order_owned', 'این سفارش به حساب دیگری متصل است.');
  db.prepare("UPDATE orders SET owner=? WHERE code=? AND phone=? AND (owner IS NULL OR owner='')").run(req.user.username, code, phone);
  res.json({ order: group(db.prepare('SELECT * FROM orders WHERE code=? ORDER BY id').all(code), { includeAddress: true }) });
});

/* ---------- محصولات من: اقلام خریداری‌شده با وضعیت ---------- */
router.get('/my/products', appAuth, (req, res) => {
  const p = paging(req, 20);
  const w = ownWhere(req);
  const filter = { active: "status IN ('pending','review','shipping')", delivered: "status='delivered'", returned: "status='returned'" }[req.query.status];
  const where = w.sql + (filter ? ' AND ' + filter : '');
  const total = db.prepare(`SELECT COUNT(*) c FROM orders WHERE ${where}`).get(...w.args).c;
  const rows = db.prepare(`SELECT * FROM orders WHERE ${where} ORDER BY id DESC LIMIT ? OFFSET ?`).all(...w.args, p.limit, p.offset);
  const img = imgStmt();
  const active = db.prepare("SELECT status FROM products WHERE id=?");
  const items = rows.map(r => {
    const l = line(r, img);
    const prod = r.product_id ? active.get(r.product_id) : null;
    return { ...l, orderCode: r.code, purchasedAt: r.created_at, canReorder: !!(prod && prod.status === 'active') };
  });
  res.json(paged(items, total, p));
});

module.exports = { router, group };
