/* توکن‌های اپ: access کوتاه‌مدت (JWT) + refresh بلندمدت چرخشی.
   - refresh تصادفی ۳۲ بایتی است و فقط هش SHA-256 آن ذخیره می‌شود.
   - هر بار استفاده، توکن قبلی باطل و توکن جدید در همان «زنجیره» صادر می‌شود.
   - ارائهٔ دوبارهٔ توکنِ باطل‌شده (نشانهٔ سرقت) کل زنجیره را باطل می‌کند. */
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const db = require('../db');
const { SECRET } = require('../auth');

const ACCESS_TTL_SEC = parseInt(process.env.APP_ACCESS_TTL_SEC, 10) || 15 * 60;
const REFRESH_TTL_DAYS = parseInt(process.env.APP_REFRESH_TTL_DAYS, 10) || 30;

const sha = (t) => crypto.createHash('sha256').update(t).digest('hex');

function signAccess(user) {
  return jwt.sign({ id: user.id, username: user.username, role: user.role, name: user.name, aud: 'app' },
    SECRET, { expiresIn: ACCESS_TTL_SEC });
}

function insertRefresh(userId, family, device) {
  const token = crypto.randomBytes(32).toString('base64url');
  const info = db.prepare(`INSERT INTO refresh_tokens (user_id,token_hash,family,device,expires_at)
    VALUES (?,?,?,?,datetime('now', ?))`).run(userId, sha(token), family, String(device || '').slice(0, 120), `+${REFRESH_TTL_DAYS} days`);
  return { token, id: info.lastInsertRowid };
}

// صدور جفت توکن برای ورود/ثبت‌نام
function issue(user, device) {
  const r = insertRefresh(user.id, crypto.randomUUID(), device);
  return { accessToken: signAccess(user), refreshToken: r.token, expiresIn: ACCESS_TTL_SEC, tokenType: 'Bearer' };
}

// چرخش: { user, tokens } یا خطا با کد
function rotate(refreshToken, device) {
  const row = db.prepare('SELECT * FROM refresh_tokens WHERE token_hash=?').get(sha(String(refreshToken || '')));
  if (!row) return { error: 'invalid_refresh' };
  if (row.revoked_at) {
    // استفادهٔ مجدد از توکن باطل‌شده → احتمال سرقت → ابطال همهٔ توکن‌های زنجیره
    db.prepare("UPDATE refresh_tokens SET revoked_at=datetime('now') WHERE family=? AND revoked_at IS NULL").run(row.family);
    return { error: 'refresh_reused' };
  }
  if (db.prepare("SELECT datetime('now') > ? AS exp").get(row.expires_at).exp) return { error: 'refresh_expired' };
  const user = db.prepare('SELECT * FROM admins WHERE id=?').get(row.user_id);
  if (!user || user.status !== 'active') return { error: 'account_inactive' };
  const next = insertRefresh(user.id, row.family, device || row.device);
  db.prepare("UPDATE refresh_tokens SET revoked_at=datetime('now'), replaced_by=? WHERE id=?").run(next.id, row.id);
  return { user, tokens: { accessToken: signAccess(user), refreshToken: next.token, expiresIn: ACCESS_TTL_SEC, tokenType: 'Bearer' } };
}

function revoke(refreshToken) {
  db.prepare("UPDATE refresh_tokens SET revoked_at=datetime('now') WHERE token_hash=? AND revoked_at IS NULL").run(sha(String(refreshToken || '')));
}
function revokeAll(userId) {
  db.prepare("UPDATE refresh_tokens SET revoked_at=datetime('now') WHERE user_id=? AND revoked_at IS NULL").run(userId);
}
// پاک‌سازی دوره‌ای ردیف‌های قدیمی
function prune() {
  db.prepare("DELETE FROM refresh_tokens WHERE expires_at < datetime('now','-7 days')").run();
  db.prepare("DELETE FROM idempotency_keys WHERE created_at < datetime('now','-1 day')").run();
}

module.exports = { issue, rotate, revoke, revokeAll, prune, ACCESS_TTL_SEC };
