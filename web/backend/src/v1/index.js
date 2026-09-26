/* API نسخهٔ ۱ — لایهٔ رسمی اپ اندروید (و همگام‌سازی سایت).
   روی همان پایگاه داده و همان حساب‌های سایت کار می‌کند؛ مسیرهای قدیمی /api/* دست‌نخورده می‌مانند.
   مستندات: backend/API-v1.md */
const express = require('express');
const rateLimit = require('express-rate-limit');
const { ApiError, sendError, noStore } = require('./http');
const { maintenance } = require('./middleware');
const tokens = require('./tokens');

const v1 = express.Router();

// سقف کلی درخواست برای هر IP (مسیرهای حساس محدودیت سخت‌گیرانه‌تر خود را دارند)
v1.use(rateLimit({
  windowMs: 60 * 1000, max: parseInt(process.env.API_RATE_PER_MIN, 10) || 300, standardHeaders: true, legacyHeaders: false,
  handler: (req, res) => sendError(res, new ApiError(429, 'rate_limited', 'درخواست‌های بیش از حد مجاز. کمی بعد تلاش کنید.')),
}));
v1.use((req, res, next) => { res.set('X-API-Version', '1'); next(); });
v1.use(express.json({ limit: '256kb' }));

const catalog = require('./routes/catalog');
// پیکربندی حتی در حالت تعمیرات در دسترس است تا اپ صفحهٔ مناسب نشان دهد
v1.get('/config', noStore, catalog.config);
v1.use(maintenance);
v1.use('/auth', require('./routes/auth').router);
v1.use(catalog.router);
v1.use(require('./routes/orders').router);
v1.use(require('./routes/account').router);

v1.use((req, res) => sendError(res, new ApiError(404, 'not_found', 'مسیر یافت نشد.')));

// خطاهای کنترل‌شده با پیام فارسی؛ خطای ناشناخته بدون جزئیات فنی
// eslint-disable-next-line no-unused-vars
v1.use((err, req, res, next) => {
  if (err instanceof ApiError) return sendError(res, err);
  if (err && err.type === 'entity.parse.failed') return sendError(res, new ApiError(400, 'invalid_json', 'درخواست نامعتبر است.'));
  if (err && err.type === 'entity.too.large') return sendError(res, new ApiError(413, 'too_large', 'حجم درخواست بیش از حد مجاز است.'));
  console.error('[v1]', req.method, req.path, err);
  sendError(res, new ApiError(500, 'server_error', 'خطایی در سرور رخ داد. لطفاً دوباره تلاش کنید.'));
});

// پاک‌سازی دوره‌ای توکن‌ها و کلیدهای یکتایی منقضی
setInterval(() => { try { tokens.prune(); } catch (e) { /* ignore */ } }, 6 * 60 * 60 * 1000).unref();

module.exports = v1;
