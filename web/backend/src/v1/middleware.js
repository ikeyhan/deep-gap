/* میان‌افزارهای API اپ: احراز هویت، تفکیک نقش‌ها و حالت تعمیرات */
const db = require('../db');
const { verify, STAFF } = require('../auth');
const { ApiError, sendError } = require('./http');

function bearer(req) {
  const h = req.headers.authorization || '';
  return h.startsWith('Bearer ') ? h.slice(7) : null;
}

// نقش‌های مجاز در اپ مشتریان. کارکنان پنل (admin/editor/support) عمداً راه ندارند:
// پنل مدیریت جداست و هیچ قابلیت مدیریتی از طریق این API در دسترس نیست.
const APP_ROLES = ['customer', 'seller', 'office'];

function resolve(req) {
  const token = bearer(req);
  if (!token) return { none: true };
  const p = verify(token);
  if (!p) return { error: new ApiError(401, 'token_invalid', 'نشست شما منقضی شده است. دوباره وارد شوید.') };
  const row = db.prepare('SELECT id,username,name,role,status FROM admins WHERE id=?').get(p.id);
  if (!row || row.status !== 'active') return { error: new ApiError(401, 'account_inactive', 'حساب شما غیرفعال است. با پشتیبانی تماس بگیرید.') };
  if (!APP_ROLES.includes(row.role)) return { error: new ApiError(403, 'staff_not_allowed', 'حساب‌های مدیریتی فقط از طریق پنل مدیریت قابل استفاده‌اند.') };
  return { user: row };
}

// ورود الزامی
function appAuth(req, res, next) {
  const r = resolve(req);
  if (r.none) return sendError(res, new ApiError(401, 'auth_required', 'برای این بخش ابتدا وارد حساب خود شوید.'));
  if (r.error) return sendError(res, r.error);
  req.user = r.user;
  next();
}

// ورود اختیاری (مثلاً ثبت سفارش مهمان)؛ توکن نامعتبر خطا می‌دهد تا اپ نشست را نو کند
function optionalAuth(req, res, next) {
  const r = resolve(req);
  if (r.error) return sendError(res, r.error);
  req.user = r.user || null;
  next();
}

// حالت تعمیرات سایت (تنظیمات پنل) — اپ صفحهٔ «در حال به‌روزرسانی» نشان می‌دهد
function maintenance(req, res, next) {
  const r = db.prepare("SELECT value FROM settings WHERE key='maintenance'").get();
  if (r && r.value === '1') return sendError(res, new ApiError(503, 'maintenance', 'اتم ۳۱۳ در حال به‌روزرسانی است. کمی بعد دوباره سر بزنید.'));
  next();
}

module.exports = { appAuth, optionalAuth, maintenance, APP_ROLES, STAFF };
