/* ابزارهای مشترک API نسخهٔ ۱: قالب یکسان خطا، صفحه‌بندی و کش */

// قالب خطا در v1: { error: { code, message, fields? } }
// code ماشین‌خوان است (اپ بر اساس آن رفتار می‌کند) و message فارسی و قابل نمایش به کاربر.
class ApiError extends Error {
  constructor(status, code, message, fields) {
    super(message);
    this.status = status; this.code = code; this.fields = fields;
  }
}
const bad = (code, message, fields) => new ApiError(400, code, message, fields);
const notFound = (message) => new ApiError(404, 'not_found', message || 'مورد درخواستی یافت نشد.');

function sendError(res, err) {
  const body = { error: { code: err.code || 'error', message: err.message } };
  if (err.fields) body.error.fields = err.fields;
  res.status(err.status || 400).json(body);
}

// صفحه‌بندی: page از ۱، limit حداکثر ۵۰
function paging(req, defLimit = 20) {
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || defLimit));
  return { page, limit, offset: (page - 1) * limit };
}
function paged(items, total, p) {
  return { items, page: p.page, limit: p.limit, total, hasMore: p.offset + items.length < total };
}

// کش: دادهٔ عمومی کوتاه‌مدت قابل کش است (ETag خودکار express پاسخ ۳۰۴ می‌دهد)؛ دادهٔ شخصی هرگز
const publicCache = (seconds = 60) => (req, res, next) => { res.set('Cache-Control', `public, max-age=${seconds}`); next(); };
const noStore = (req, res, next) => { res.set('Cache-Control', 'private, no-store'); next(); };

// تبدیل ارقام فارسی به انگلیسی (ورودی فرم‌های موبایل)
const toEn = (s) => String(s == null ? '' : s).replace(/[۰-۹]/g, d => '۰۱۲۳۴۵۶۷۸۹'.indexOf(d)).replace(/[٠-٩]/g, d => '٠١٢٣٤٥٦٧٨٩'.indexOf(d));

module.exports = { ApiError, bad, notFound, sendError, paging, paged, publicCache, noStore, toEn };
