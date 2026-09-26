/* داده‌های عمومی سایت — بدون احراز هویت (فقط خواندنی و امن) */
const express = require('express');
const db = require('../db');
const { str, int, isMobile } = require('../validate');
const rateLimit = require('express-rate-limit');
const { verify, clientIp } = require('../auth');
const banned = require('../banned');
const router = express.Router();

// ارسال پیام شهروند به یک دفتر محله (گفتگو)
router.post('/office-message', (req, res) => {
  const officeId = parseInt(req.body.office_id, 10);
  const name = str(req.body.name, 120).trim() || 'شهروند';
  const phone = str(req.body.phone, 20).trim();
  const body = str(req.body.body, 4000).trim();
  if (!officeId || !body) return res.status(400).json({ error: 'انتخاب دفتر و متن پیام الزامی است.' });
  if (!banned.guard(res, name, body)) return;
  const office = db.prepare("SELECT id,name,owner FROM offices WHERE id=? AND status='verified'").get(officeId);
  if (!office) return res.status(404).json({ error: 'دفتر یافت نشد یا هنوز تأیید نشده است.' });
  const h = req.headers.authorization || '';
  const who = h.startsWith('Bearer ') ? verify(h.slice(7)) : null;
  db.prepare("INSERT INTO office_messages (owner,office,sender_name,sender_phone,body,status,sender_owner) VALUES (?,?,?,?,?,?,?)")
    .run(office.owner || '', office.name, name, phone, body, 'open', who ? who.username : '');
  res.status(201).json({ ok: true });
});

// اسلایدهای فعال بخش اولیهٔ سایت
router.get('/slides', (req, res) => {
  const items = db.prepare(
    "SELECT id,eyebrow,title,subtitle,image,cta_label,cta_link FROM slides WHERE status='active' ORDER BY sort ASC, id ASC"
  ).all();
  res.json({ items });
});

// تبلیغات فعال سایت (نوار بالا + مربع گوشه)
router.get('/ads', (req, res) => {
  const items = db.prepare(
    "SELECT id,placement,title,text,image,link,cta_label,bg FROM ads WHERE status='active' ORDER BY sort ASC, id ASC"
  ).all();
  res.json({
    top: items.filter(a => a.placement === 'top'),
    corner: items.filter(a => a.placement === 'corner'),
  });
});

// دفاتر محلات تأییدشده برای نمایش در سایت
router.get('/offices', (req, res) => {
  const items = db.prepare(
    "SELECT id,name,manager,area,city,address,phone,bio,avatar,rating,created_at FROM offices WHERE status='verified' ORDER BY rating DESC, id DESC LIMIT 48"
  ).all();
  res.json({ items });
});

// خدمات یک دفتر (عمومی) — با شناسهٔ دفتر (نام‌کاربری حساب‌ها افشا نمی‌شود)
router.get('/office-services', (req, res) => {
  const officeId = parseInt(req.query.office_id, 10);
  let owner = '';
  if (officeId) {
    const o = db.prepare("SELECT owner FROM offices WHERE id=? AND status='verified'").get(officeId);
    if (!o) return res.json({ items: [] });
    owner = o.owner || '';
  }
  const rows = officeId
    ? db.prepare("SELECT id,title,category,office,description,price FROM office_services WHERE status='active' AND owner=? ORDER BY id DESC").all(owner)
    : db.prepare("SELECT id,title,category,office,description,price FROM office_services WHERE status='active' ORDER BY id DESC LIMIT 60").all();
  res.json({ items: rows });
});

// فروشندگان تأییدشده برای نمایش در سایت
router.get('/sellers', (req, res) => {
  const items = db.prepare(
    "SELECT id,name,category,city,rating,sales,bio,avatar,created_at FROM sellers WHERE status='active' ORDER BY sales DESC, id DESC LIMIT 24"
  ).all();
  res.json({ items });
});

// محصولات فعال (برای جستجو و فهرست سایت)
router.get('/products', (req, res) => {
  const items = db.prepare(
    "SELECT id,title,category,seller,price,stock,image,description FROM products WHERE status='active' ORDER BY id DESC LIMIT 200"
  ).all();
  res.json({ items });
});

const orderService = require('../orders-service');

// بررسی کد تخفیف
router.get('/offer', (req, res) => {
  const o = orderService.findOffer(req.query.code);
  if (!o) return res.status(404).json({ error: 'کد تخفیف نامعتبر یا منقضی است.' });
  res.json({ code: o.code, title: o.title, kind: o.kind, amount: o.amount });
});

// ثبت سفارش از سبد خرید — بدون نیاز به ورود (در صورت ورود مشتری، به حساب او متصل می‌شود)
// قیمت‌گذاری و ثبت در سرویس مشترک orders-service انجام می‌شود (همان منطق API اپ).
const orderLimiter = rateLimit({ windowMs: 10 * 60 * 1000, max: 20, standardHeaders: true, legacyHeaders: false,
  message: { error: 'تعداد درخواست‌های ثبت سفارش بیش از حد مجاز است. کمی بعد تلاش کنید.' } });
router.post('/order', orderLimiter, (req, res) => {
  const name = str(req.body.name, 120).trim();
  const phone = str(req.body.phone, 20).trim();
  const address = str(req.body.address, 600).trim();
  const city = str(req.body.city, 60).trim();
  if (!Array.isArray(req.body.items) || !req.body.items.length) return res.status(400).json({ error: 'سبد خرید خالی است.' });
  if (!name || !address) return res.status(400).json({ error: 'نام گیرنده و آدرس کامل را وارد کنید.' });
  if (!isMobile(phone)) return res.status(400).json({ error: 'شمارهٔ موبایل نامعتبر است (نمونهٔ درست: 09xxxxxxxxx).' });
  if (!banned.guard(res, name, address, req.body.note)) return;

  // اگر مشتری وارد شده باشد، سفارش به حساب او متصل می‌شود
  const h = req.headers.authorization || '';
  const who = h.startsWith('Bearer ') ? verify(h.slice(7)) : null;
  try {
    const r = orderService.place({
      items: req.body.items, coupon: req.body.coupon, name, phone, address, city, note: req.body.note,
      owner: who ? who.username : '', ip: clientIp(req), strict: false,
    });
    res.status(201).json({ ok: true, code: r.code, subtotal: r.subtotal, discount: r.discount, shipping: r.shipping, total: r.total });
  } catch (e) {
    if (e instanceof orderService.OrderError) return res.status(e.status).json({ error: e.message });
    throw e;
  }
});

// نظرات تأییدشدهٔ یک محصول
router.get('/comments', (req, res) => {
  const product = str(req.query.product, 200).trim();
  const items = product
    ? db.prepare("SELECT id,author,product,body,rating,created_at FROM comments WHERE status='approved' AND product=? ORDER BY id DESC LIMIT 100").all(product)
    : db.prepare("SELECT id,author,product,body,rating,created_at FROM comments WHERE status='approved' ORDER BY id DESC LIMIT 200").all();
  res.json({ items });
});

// ثبت نظر خریدار — پس از تأیید مدیر در سایت نمایش داده می‌شود
const commentLimiter = rateLimit({ windowMs: 10 * 60 * 1000, max: 10, standardHeaders: true, legacyHeaders: false,
  message: { error: 'تعداد نظرات ارسالی بیش از حد مجاز است. کمی بعد تلاش کنید.' } });
router.post('/comment', commentLimiter, (req, res) => {
  const author = str(req.body.author, 120).trim();
  const product = str(req.body.product, 200).trim();
  const body = str(req.body.body, 2000).trim();
  const rating = Math.max(1, Math.min(5, int(req.body.rating, 5)));
  if (!author || !product || body.length < 5) return res.status(400).json({ error: 'نام، محصول و متن نظر (حداقل ۵ نویسه) الزامی است.' });
  if (!banned.guard(res, author, body)) return;
  db.prepare("INSERT INTO comments (author,product,body,rating,status) VALUES (?,?,?,?,'pending')").run(author, product, body, rating);
  res.status(201).json({ ok: true });
});

// پیگیری سفارش با کد سفارش + موبایل
router.get('/track', (req, res) => {
  const code = str(req.query.code, 40).trim();
  const phone = str(req.query.phone, 20).trim();
  if (!code || !phone) return res.status(400).json({ error: 'کد سفارش و موبایل را وارد کنید.' });
  const items = db.prepare("SELECT code,product,seller,amount,status,created_at FROM orders WHERE code=? AND phone=?").all(code, phone);
  if (!items.length) return res.status(404).json({ error: 'سفارشی با این مشخصات یافت نشد.' });
  res.json({ items });
});

module.exports = router;
