/* v1 — حساب کاربری: پروفایل، حذف حساب، علاقه‌مندی‌ها، اعلان‌ها، دستگاه‌ها و پیام‌های پشتیبانی.
   همهٔ مسیرها نیازمند ورود و محدود به دادهٔ همان کاربرند. */
const express = require('express');
const bcrypt = require('bcryptjs');
const rateLimit = require('express-rate-limit');
const db = require('../../db');
const { clientIp } = require('../../auth');
const { str, int, isMobile } = require('../../validate');
const banned = require('../../banned');
const tokens = require('../tokens');
const { ApiError, bad, notFound, sendError, paging, paged, noStore, toEn } = require('../http');
const { appAuth } = require('../middleware');
const { publicUser } = require('./auth');
const { product, PRODUCT_COLS } = require('./catalog');

const router = express.Router();
// فقط همین مسیرها احراز هویت می‌خواهند (روتر روی ریشهٔ v1 سوار است)
router.use(['/me', '/wishlist', '/notifications', '/devices', '/support'], noStore, appAuth);

/* ---------- پروفایل ---------- */
router.get('/me', (req, res) => {
  const u = db.prepare('SELECT * FROM admins WHERE id=?').get(req.user.id);
  const unread = db.prepare('SELECT COUNT(*) c FROM notifications WHERE user_id=? AND read_at IS NULL').get(u.id).c;
  const orders = db.prepare('SELECT COUNT(DISTINCT code) c FROM orders WHERE owner=?').get(u.username).c;
  const wish = db.prepare('SELECT COUNT(*) c FROM wishlist WHERE user_id=?').get(u.id).c;
  res.json({ user: publicUser(u), counts: { unreadNotifications: unread, orders, wishlist: wish } });
});

router.put('/me', (req, res) => {
  const cur = db.prepare('SELECT * FROM admins WHERE id=?').get(req.user.id);
  const name = str(req.body.name, 120).trim();
  const email = str(req.body.email, 160).trim();
  const phone = toEn(str(req.body.phone, 20)).replace(/[^0-9]/g, '');
  const city = str(req.body.city, 60).trim();
  const address = str(req.body.address, 600).trim();
  const fields = {};
  if (name.length < 2) fields.name = 'نام را وارد کنید.';
  if (!isMobile(phone)) fields.phone = 'شمارهٔ موبایل نامعتبر است (نمونه: 09123456789).';
  if (email && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) fields.email = 'ایمیل نامعتبر است.';
  if (Object.keys(fields).length) throw bad('invalid_input', 'لطفاً خطاهای فرم را برطرف کنید.', fields);
  const w = banned.findBanned(name, address);
  if (w) throw bad('banned_word', 'متن شامل کلمهٔ غیرمجاز «' + w + '» است.');
  if (cur.role === 'customer' && phone !== cur.phone &&
      db.prepare("SELECT id FROM admins WHERE phone=? AND role='customer' AND status='active' AND id<>?").get(phone, cur.id))
    throw new ApiError(409, 'phone_taken', 'این شماره متعلق به حساب دیگری است.', { phone: 'این شماره متعلق به حساب دیگری است.' });
  db.prepare('UPDATE admins SET name=?, email=?, phone=?, city=?, address=? WHERE id=?').run(name, email, phone, city, address, cur.id);
  // همان همگام‌سازی سایت (/api/my/profile): ردیف مشتری در پنل مدیریت
  if (cur.role === 'customer' && cur.phone) db.prepare('UPDATE customers SET name=?, email=?, city=?, phone=? WHERE phone=?').run(name, email, city, phone, cur.phone);
  db.prepare('INSERT INTO activity_log (actor,action,target,ip) VALUES (?,?,?,?)').run(cur.username, 'ویرایش پروفایل', 'اپ اندروید', clientIp(req));
  res.json({ user: publicUser(db.prepare('SELECT * FROM admins WHERE id=?').get(cur.id)) });
});

// حذف حساب (الزام Google Play): حساب غیرفعال و اطلاعات شخصی حذف می‌شود؛
// ردیف‌های سفارش برای حسابداری باقی می‌مانند اما از حساب جدا می‌شوند.
router.delete('/me', rateLimit({ windowMs: 15 * 60 * 1000, max: 5, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'درخواست‌های بیش از حد مجاز.')) }), (req, res) => {
  const u = db.prepare('SELECT * FROM admins WHERE id=?').get(req.user.id);
  if (!bcrypt.compareSync(str(req.body.password, 200), u.password_hash))
    throw bad('wrong_password', 'رمز عبور نادرست است.', { password: 'رمز عبور نادرست است.' });
  if (u.role !== 'customer')
    throw new ApiError(409, 'business_account', 'حذف حساب فروشنده یا دفتر از طریق پشتیبانی انجام می‌شود.');
  db.transaction(() => {
    db.prepare("UPDATE admins SET status='deleted', name='کاربر حذف‌شده', email='', phone='', city='', address='', password_hash=? WHERE id=?")
      .run(bcrypt.hashSync(require('crypto').randomBytes(24).toString('hex'), 10), u.id);
    db.prepare("UPDATE orders SET owner='' WHERE owner=?").run(u.username);
    db.prepare('DELETE FROM wishlist WHERE user_id=?').run(u.id);
    db.prepare('DELETE FROM notifications WHERE user_id=?').run(u.id);
    db.prepare('DELETE FROM devices WHERE user_id=?').run(u.id);
    tokens.revokeAll(u.id);
    db.prepare('INSERT INTO activity_log (actor,action,target,ip) VALUES (?,?,?,?)').run(u.username, 'حذف حساب', 'به درخواست کاربر (اپ)', clientIp(req));
  })();
  res.json({ ok: true });
});

/* ---------- علاقه‌مندی‌ها (مشترک سایت و اپ) ---------- */
router.get('/wishlist', (req, res) => {
  const rows = db.prepare(`SELECT ${PRODUCT_COLS}, p.status FROM wishlist w JOIN products p ON p.id=w.product_id WHERE w.user_id=? ORDER BY w.created_at DESC`).all(req.user.id);
  res.json({ items: rows.map(r => ({ ...product(r), available: r.status === 'active' })), ids: rows.map(r => r.id) });
});
router.put('/wishlist/:productId', (req, res) => {
  const pid = int(req.params.productId);
  if (!db.prepare('SELECT id FROM products WHERE id=?').get(pid)) throw notFound('محصول یافت نشد.');
  if (db.prepare('SELECT COUNT(*) c FROM wishlist WHERE user_id=?').get(req.user.id).c >= 500) throw bad('limit', 'فهرست علاقه‌مندی پر است.');
  db.prepare('INSERT OR IGNORE INTO wishlist (user_id,product_id) VALUES (?,?)').run(req.user.id, pid);
  res.json({ ok: true });
});
router.delete('/wishlist/:productId', (req, res) => {
  db.prepare('DELETE FROM wishlist WHERE user_id=? AND product_id=?').run(req.user.id, int(req.params.productId));
  res.json({ ok: true });
});
// ادغام فهرست محلی (سایت یا اپ آفلاین) با سرور — فقط افزودن، بدون حذف
router.post('/wishlist/merge', (req, res) => {
  const ids = (Array.isArray(req.body.productIds) ? req.body.productIds : []).slice(0, 200).map(x => int(x)).filter(x => x > 0);
  const ins = db.prepare('INSERT OR IGNORE INTO wishlist (user_id,product_id) SELECT ?, id FROM products WHERE id=?');
  db.transaction(() => ids.forEach(id => ins.run(req.user.id, id)))();
  res.json({ ids: db.prepare('SELECT product_id FROM wishlist WHERE user_id=?').all(req.user.id).map(r => r.product_id) });
});

/* ---------- اعلان‌ها ---------- */
router.get('/notifications', (req, res) => {
  const p = paging(req, 30);
  const since = int(req.query.since, 0); // همگام‌سازی افزایشی: فقط اعلان‌های جدیدتر از این شناسه
  const where = 'user_id=?' + (since ? ' AND id>?' : '');
  const args = since ? [req.user.id, since] : [req.user.id];
  const total = db.prepare(`SELECT COUNT(*) c FROM notifications WHERE ${where}`).get(...args).c;
  const items = db.prepare(`SELECT * FROM notifications WHERE ${where} ORDER BY id DESC LIMIT ? OFFSET ?`).all(...args, p.limit, p.offset)
    .map(n => ({ id: n.id, type: n.type, title: n.title, body: n.body || '', ref: n.ref || '', read: !!n.read_at, createdAt: n.created_at }));
  const unread = db.prepare('SELECT COUNT(*) c FROM notifications WHERE user_id=? AND read_at IS NULL').get(req.user.id).c;
  res.json({ ...paged(items, total, p), unread });
});
router.post('/notifications/read', (req, res) => {
  if (req.body.all) db.prepare("UPDATE notifications SET read_at=datetime('now') WHERE user_id=? AND read_at IS NULL").run(req.user.id);
  else {
    const ids = (Array.isArray(req.body.ids) ? req.body.ids : []).slice(0, 200).map(x => int(x)).filter(Boolean);
    const up = db.prepare("UPDATE notifications SET read_at=datetime('now') WHERE user_id=? AND id=? AND read_at IS NULL");
    db.transaction(() => ids.forEach(id => up.run(req.user.id, id)))();
  }
  res.json({ ok: true });
});

// ثبت دستگاه برای پوش (FCM در آینده) — provider=none یعنی دریافت با همگام‌سازی دوره‌ای
router.post('/devices', (req, res) => {
  const token = str(req.body.token, 300).trim();
  if (token.length < 8) throw bad('invalid_input', 'شناسهٔ دستگاه نامعتبر است.');
  const provider = ['fcm', 'none'].includes(req.body.provider) ? req.body.provider : 'none';
  db.prepare(`INSERT INTO devices (user_id,token,platform,provider,app_version,updated_at) VALUES (?,?,?,?,?,datetime('now'))
    ON CONFLICT(token) DO UPDATE SET user_id=excluded.user_id, provider=excluded.provider, app_version=excluded.app_version, updated_at=datetime('now')`)
    .run(req.user.id, token, 'android', provider, str(req.body.appVersion, 20));
  res.json({ ok: true });
});

/* ---------- پشتیبانی: تیکت‌های کاربر و گفتگو با دفاتر ---------- */
router.get('/support/messages', (req, res) => {
  const items = db.prepare('SELECT id,subject,body,reply,status,created_at FROM messages WHERE owner=? ORDER BY id DESC LIMIT 100').all(req.user.username)
    .map(m => ({ id: m.id, subject: m.subject || '', body: m.body || '', reply: m.reply || '', status: m.status, createdAt: m.created_at }));
  res.json({ items });
});
const ticketLimiter = rateLimit({ windowMs: 10 * 60 * 1000, max: 10, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'تعداد پیام‌ها بیش از حد مجاز است. کمی بعد تلاش کنید.')) });
router.post('/support/messages', ticketLimiter, (req, res) => {
  const subject = str(req.body.subject, 200).trim() || 'پیام از اپ اندروید';
  const body = str(req.body.body, 4000).trim();
  if (body.length < 5) throw bad('invalid_input', 'متن پیام را کامل‌تر بنویسید.', { body: 'حداقل ۵ نویسه' });
  const w = banned.findBanned(subject, body);
  if (w) throw bad('banned_word', 'متن شامل کلمهٔ غیرمجاز «' + w + '» است.');
  const u = db.prepare('SELECT name,username,phone FROM admins WHERE id=?').get(req.user.id);
  const info = db.prepare("INSERT INTO messages (sender,subject,body,status,owner) VALUES (?,?,?,'open',?)")
    .run((u.name || u.username) + ' (' + (u.phone || u.username) + ')', subject, body, u.username);
  res.status(201).json({ item: { id: info.lastInsertRowid, subject, body, reply: '', status: 'open', createdAt: new Date().toISOString() } });
});
router.get('/support/office-messages', (req, res) => {
  const items = db.prepare('SELECT id,office,body,reply,status,created_at FROM office_messages WHERE sender_owner=? ORDER BY id DESC LIMIT 100').all(req.user.username)
    .map(m => ({ id: m.id, office: m.office || '', body: m.body || '', reply: m.reply || '', status: m.status, createdAt: m.created_at }));
  res.json({ items });
});

module.exports = { router };
