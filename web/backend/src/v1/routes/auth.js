/* v1 — احراز هویت اپ: همان حساب‌های سایت (جدول admins)، با توکن چرخشی */
const express = require('express');
const bcrypt = require('bcryptjs');
const rateLimit = require('express-rate-limit');
const db = require('../../db');
const { clientIp } = require('../../auth');
const { str, isMobile } = require('../../validate');
const banned = require('../../banned');
const tokens = require('../tokens');
const { ApiError, bad, sendError, noStore, toEn } = require('../http');
const { appAuth, APP_ROLES } = require('../middleware');

const router = express.Router();
router.use(noStore);

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, max: 10, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'تلاش‌های بیش از حد مجاز. چند دقیقه صبر کنید.')),
});
const refreshLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, max: 60, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'درخواست‌های بیش از حد مجاز. کمی بعد تلاش کنید.')),
});

const MAX_FAILS = 5, LOCK_MINUTES = 15;

// خروجی عمومی حساب — هرگز هش رمز یا داده‌های داخلی برنمی‌گردد
function publicUser(u) {
  return {
    id: u.id, username: u.username, name: u.name || u.username, email: u.email || '', phone: u.phone || '',
    city: u.city || '', address: u.address || '', role: u.role,
    storeName: u.seller_name || u.office_name || '', createdAt: u.created_at || null,
  };
}
const device = (req) => str(req.get('X-Device-Name') || req.get('User-Agent'), 120);

router.post('/login', authLimiter, (req, res) => {
  const username = str(req.body.username, 60).trim().toLowerCase();
  const password = str(req.body.password, 200);
  const ip = clientIp(req);
  if (!username || !password) throw bad('invalid_input', 'نام کاربری و رمز عبور را وارد کنید.');

  const fails = db.prepare(`SELECT COUNT(*) c FROM login_attempts WHERE username=? AND success=0 AND created_at > datetime('now', ?)`)
    .get(username, `-${LOCK_MINUTES} minutes`).c;
  if (fails >= MAX_FAILS) throw new ApiError(429, 'account_locked', `به دلیل تلاش‌های ناموفق، ورود ${LOCK_MINUTES} دقیقه قفل شد.`);

  // ورود با نام کاربری یا شمارهٔ موبایل (برای مشتریان)
  let u = db.prepare('SELECT * FROM admins WHERE username=?').get(username);
  const phone = toEn(username).replace(/[^0-9]/g, '');
  if (!u && isMobile(phone)) u = db.prepare("SELECT * FROM admins WHERE phone=? AND role='customer' AND status='active' ORDER BY id LIMIT 1").get(phone);
  const ok = u && u.status === 'active' && bcrypt.compareSync(password, u.password_hash);
  db.prepare('INSERT INTO login_attempts (username,ip,success) VALUES (?,?,?)').run(username, ip, ok ? 1 : 0);
  if (!ok) throw new ApiError(401, 'invalid_credentials', 'نام کاربری یا رمز عبور اشتباه است.');
  // کارکنان پنل از اپ مشتریان وارد نمی‌شوند (تفکیک کامل پنل مدیریت)
  if (!APP_ROLES.includes(u.role)) throw new ApiError(403, 'staff_not_allowed', 'حساب‌های مدیریتی فقط از طریق پنل مدیریت قابل استفاده‌اند.');

  db.prepare("UPDATE admins SET last_login=datetime('now') WHERE id=?").run(u.id);
  db.prepare('INSERT INTO activity_log (actor,action,target,ip) VALUES (?,?,?,?)').run(u.username, 'ورود', 'ورود از اپ اندروید', ip);
  res.json({ ...tokens.issue(u, device(req)), user: publicUser(u) });
});

// ثبت‌نام خریدار — همان قوانین سایت (/api/auth/register-customer)
router.post('/register', authLimiter, (req, res) => {
  const reg = db.prepare("SELECT value FROM settings WHERE key='registration'").get();
  if (reg && reg.value === '0') throw new ApiError(403, 'registration_closed', 'ثبت‌نام حساب جدید موقتاً غیرفعال است.');
  const name = str(req.body.name, 120).trim();
  const phone = toEn(str(req.body.phone, 20)).replace(/[^0-9]/g, '');
  const email = str(req.body.email, 160).trim();
  const city = str(req.body.city, 60).trim();
  const username = str(req.body.username, 60).trim().toLowerCase();
  const password = str(req.body.password, 200);

  const fields = {};
  if (!name) fields.name = 'نام و نام خانوادگی را وارد کنید.';
  if (!isMobile(phone)) fields.phone = 'شمارهٔ موبایل نامعتبر است (نمونه: 09123456789).';
  if (!/^[a-z0-9_.]{3,30}$/.test(username)) fields.username = 'فقط حروف و اعداد انگلیسی، حداقل ۳ نویسه.';
  if (password.length < 8) fields.password = 'رمز عبور باید حداقل ۸ نویسه باشد.';
  if (email && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) fields.email = 'ایمیل نامعتبر است.';
  if (Object.keys(fields).length) throw bad('invalid_input', 'لطفاً خطاهای فرم را برطرف کنید.', fields);
  const w = banned.findBanned(name, username);
  if (w) throw bad('banned_word', 'استفاده از کلمهٔ «' + w + '» مجاز نیست.');
  if (db.prepare('SELECT id FROM admins WHERE username=?').get(username))
    throw new ApiError(409, 'username_taken', 'این نام کاربری قبلاً ثبت شده است.', { username: 'این نام کاربری قبلاً ثبت شده است.' });
  if (db.prepare("SELECT id FROM admins WHERE phone=? AND role='customer' AND status='active'").get(phone))
    throw new ApiError(409, 'phone_taken', 'با این شماره قبلاً حساب ساخته شده است. وارد شوید.', { phone: 'با این شماره قبلاً حساب ساخته شده است.' });

  const info = db.prepare("INSERT INTO admins (username,password_hash,name,email,role,status,phone,city) VALUES (?,?,?,?,?,?,?,?)")
    .run(username, bcrypt.hashSync(password, 10), name, email, 'customer', 'active', phone, city);
  if (!db.prepare('SELECT id FROM customers WHERE phone=?').get(phone))
    db.prepare('INSERT INTO customers (name,phone,email,city,total_spent,orders_count) VALUES (?,?,?,?,0,0)').run(name, phone, email, city);
  db.prepare('INSERT INTO activity_log (actor,action,target,ip) VALUES (?,?,?,?)').run(username, 'ثبت‌نام مشتری', 'اپ اندروید', clientIp(req));
  const u = db.prepare('SELECT * FROM admins WHERE id=?').get(info.lastInsertRowid);
  res.status(201).json({ ...tokens.issue(u, device(req)), user: publicUser(u) });
});

router.post('/refresh', refreshLimiter, (req, res) => {
  const r = tokens.rotate(str(req.body.refreshToken, 200), device(req));
  if (r.error) throw new ApiError(401, r.error, 'نشست شما منقضی شده است. دوباره وارد شوید.');
  if (!APP_ROLES.includes(r.user.role)) throw new ApiError(403, 'staff_not_allowed', 'حساب‌های مدیریتی فقط از طریق پنل مدیریت قابل استفاده‌اند.');
  res.json({ ...r.tokens, user: publicUser(r.user) });
});

// خروج: ابطال همین refresh و حذف ثبت دستگاه برای اعلان‌ها
router.post('/logout', (req, res) => {
  tokens.revoke(str(req.body.refreshToken, 200));
  const dt = str(req.body.deviceToken, 300);
  if (dt) db.prepare('DELETE FROM devices WHERE token=?').run(dt);
  res.json({ ok: true });
});

router.post('/change-password', appAuth, (req, res) => {
  const current = str(req.body.current, 200);
  const next = str(req.body.next, 200);
  if (next.length < 8) throw bad('invalid_input', 'رمز جدید باید حداقل ۸ نویسه باشد.', { next: 'حداقل ۸ نویسه' });
  const u = db.prepare('SELECT * FROM admins WHERE id=?').get(req.user.id);
  if (!bcrypt.compareSync(current, u.password_hash)) throw bad('wrong_password', 'رمز فعلی نادرست است.', { current: 'رمز فعلی نادرست است.' });
  db.prepare('UPDATE admins SET password_hash=? WHERE id=?').run(bcrypt.hashSync(next, 10), u.id);
  // همهٔ نشست‌های دیگر باطل و یک نشست تازه برای همین دستگاه صادر می‌شود
  tokens.revokeAll(u.id);
  db.prepare('INSERT INTO activity_log (actor,action,target,ip) VALUES (?,?,?,?)').run(u.username, 'تغییر رمز', 'از اپ اندروید', clientIp(req));
  res.json({ ok: true, ...tokens.issue(u, device(req)) });
});

module.exports = { router, publicUser };
