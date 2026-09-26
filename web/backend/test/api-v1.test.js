/* تست‌های یکپارچهٔ API نسخهٔ ۱ — روی یک سرور واقعی با پایگاه دادهٔ موقت اجرا می‌شود.
   اجرا: npm test */
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const PORT = 3900 + Math.floor(Math.random() * 90);
const BASE = `http://127.0.0.1:${PORT}`;
const DATA = fs.mkdtempSync(path.join(os.tmpdir(), 'atom-test-'));
let server;

async function call(method, url, { body, token, headers } = {}) {
  const h = { ...(headers || {}) };
  if (body !== undefined) h['Content-Type'] = 'application/json';
  if (token) h.Authorization = 'Bearer ' + token;
  const r = await fetch(BASE + url, { method, headers: h, body: body !== undefined ? JSON.stringify(body) : undefined });
  const text = await r.text();
  let json = null; try { json = JSON.parse(text); } catch (e) { /* not json */ }
  return { status: r.status, body: json, headers: r.headers };
}

before(async () => {
  server = spawn(process.execPath, ['server.js'], {
    cwd: path.join(__dirname, '..'),
    env: { ...process.env, PORT: String(PORT), ATOM_DATA_DIR: DATA, JWT_SECRET: 'test-secret-'.padEnd(48, 'x'), SEED_DEMO: '1' },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  for (let i = 0; i < 50; i++) {
    try { const r = await fetch(BASE + '/api/health'); if (r.ok) return; } catch (e) { /* wait */ }
    await new Promise(r => setTimeout(r, 100));
  }
  throw new Error('server did not start');
});
after(() => { server.kill(); fs.rmSync(DATA, { recursive: true, force: true }); });

const state = {};

test('config is public and never exposes secrets', async () => {
  const r = await call('GET', '/api/v1/config');
  assert.equal(r.status, 200);
  assert.equal(r.body.site.name, 'اتم ۳۱۳');
  assert.equal(r.body.commerce.onlinePayment, false);
  assert.ok(!JSON.stringify(r.body).includes('ai_api_key'));
  assert.equal(r.headers.get('x-api-version'), '1');
});

test('home aggregates sections in one request', async () => {
  const r = await call('GET', '/api/v1/home');
  assert.equal(r.status, 200);
  for (const k of ['slides', 'categories', 'newest', 'popular', 'sellers', 'offices', 'articles']) assert.ok(Array.isArray(r.body[k]), k);
  assert.ok(r.body.newest.length > 0);
  assert.match(r.headers.get('cache-control'), /public/);
});

test('products: pagination, search, filters, detail', async () => {
  const p1 = await call('GET', '/api/v1/products?limit=3&page=1');
  assert.equal(p1.status, 200);
  assert.equal(p1.body.items.length, 3);
  assert.ok(p1.body.total >= 3);
  const s = await call('GET', '/api/v1/products?q=' + encodeURIComponent('كيف')); // حروف عربی نرمال می‌شوند
  assert.ok(s.body.items.every(p => /کیف/.test(p.title + p.category + p.seller)));
  const asc = await call('GET', '/api/v1/products?sort=price_asc&limit=50');
  const prices = asc.body.items.map(p => p.price);
  assert.deepEqual(prices, [...prices].sort((a, b) => a - b));
  const d = await call('GET', '/api/v1/products/' + p1.body.items[0].id);
  assert.equal(d.status, 200);
  assert.ok('description' in d.body.product);
  assert.equal(d.body.ratingDistribution.length, 5);
  state.product = d.body.product;
  const inStock = asc.body.items.filter(p => p.stock > 2);
  state.p1 = inStock[0]; state.p2 = inStock[1];
  const missing = await call('GET', '/api/v1/products/999999');
  assert.equal(missing.status, 404);
  assert.equal(missing.body.error.code, 'not_found');
});

test('register validates fields and creates a customer', async () => {
  const badReq = await call('POST', '/api/v1/auth/register', { body: { name: '', phone: '123', username: 'x', password: '1' } });
  assert.equal(badReq.status, 400);
  assert.deepEqual(Object.keys(badReq.body.error.fields).sort(), ['name', 'password', 'phone', 'username']);
  const ok = await call('POST', '/api/v1/auth/register', { body: { name: 'مینا تست', phone: '۰۹۱۲۰۰۰۱۱۲۲', username: 'mina', password: 'mina-pass-1' } });
  assert.equal(ok.status, 201);
  assert.ok(ok.body.accessToken && ok.body.refreshToken);
  assert.equal(ok.body.user.role, 'customer');
  assert.equal(ok.body.user.phone, '09120001122');
  assert.ok(!('password_hash' in ok.body.user));
  state.tok = ok.body.accessToken; state.refresh = ok.body.refreshToken;
});

test('staff accounts cannot use the customer app API', async () => {
  const r = await call('POST', '/api/v1/auth/login', { body: { username: 'admin', password: 'atom313@' } });
  assert.equal(r.status, 403);
  assert.equal(r.body.error.code, 'staff_not_allowed');
  // توکن پنل مدیریت هم در API اپ پذیرفته نمی‌شود
  const legacy = await call('POST', '/api/auth/login', { body: { username: 'admin', password: 'atom313@' } });
  state.adminTok = legacy.body.token;
  const me = await call('GET', '/api/v1/me', { token: state.adminTok });
  assert.equal(me.status, 403);
});

test('login by username or phone; wrong password rejected', async () => {
  const wrong = await call('POST', '/api/v1/auth/login', { body: { username: 'mina', password: 'nope-nope' } });
  assert.equal(wrong.status, 401);
  assert.equal(wrong.body.error.code, 'invalid_credentials');
  const byPhone = await call('POST', '/api/v1/auth/login', { body: { username: '09120001122', password: 'mina-pass-1' } });
  assert.equal(byPhone.status, 200);
  assert.equal(byPhone.body.user.username, 'mina');
});

test('refresh rotates and detects reuse', async () => {
  const r1 = await call('POST', '/api/v1/auth/refresh', { body: { refreshToken: state.refresh } });
  assert.equal(r1.status, 200);
  assert.notEqual(r1.body.refreshToken, state.refresh);
  // استفادهٔ مجدد از توکن قدیمی → ابطال کل زنجیره
  const reuse = await call('POST', '/api/v1/auth/refresh', { body: { refreshToken: state.refresh } });
  assert.equal(reuse.status, 401);
  assert.equal(reuse.body.error.code, 'refresh_reused');
  const r2 = await call('POST', '/api/v1/auth/refresh', { body: { refreshToken: r1.body.refreshToken } });
  assert.equal(r2.status, 401);
  const fresh = await call('POST', '/api/v1/auth/login', { body: { username: 'mina', password: 'mina-pass-1' } });
  state.tok = fresh.body.accessToken; state.refresh = fresh.body.refreshToken;
});

test('profile read/update with validation', async () => {
  const noAuth = await call('GET', '/api/v1/me');
  assert.equal(noAuth.status, 401);
  assert.equal(noAuth.body.error.code, 'auth_required');
  const bad = await call('PUT', '/api/v1/me', { token: state.tok, body: { name: 'مینا', phone: '12', email: 'x' } });
  assert.equal(bad.status, 400);
  assert.ok(bad.body.error.fields.phone && bad.body.error.fields.email);
  const ok = await call('PUT', '/api/v1/me', { token: state.tok, body: { name: 'مینا رضایی', phone: '09120001122', email: 'mina@example.com', city: 'تهران', address: 'تهران، خیابان آزادی، پلاک ۱۰' } });
  assert.equal(ok.status, 200);
  assert.equal(ok.body.user.city, 'تهران');
  assert.match((await call('GET', '/api/v1/me', { token: state.tok })).headers.get('cache-control'), /no-store/);
});

test('wishlist is stored server-side and merges', async () => {
  assert.equal((await call('PUT', `/api/v1/wishlist/${state.p1.id}`, { token: state.tok })).status, 200);
  const m = await call('POST', '/api/v1/wishlist/merge', { token: state.tok, body: { productIds: [state.p2.id, 999999] } });
  assert.deepEqual(m.body.ids.sort(), [state.p1.id, state.p2.id].sort());
  const l = await call('GET', '/api/v1/wishlist', { token: state.tok });
  assert.equal(l.body.items.length, 2);
  await call('DELETE', `/api/v1/wishlist/${state.p2.id}`, { token: state.tok });
  assert.equal((await call('GET', '/api/v1/wishlist', { token: state.tok })).body.items.length, 1);
});

test('cart quote is computed by the server (client prices ignored)', async () => {
  const q = await call('POST', '/api/v1/cart/quote', { body: { items: [{ productId: state.p1.id, qty: 2, price: 1 }], coupon: 'ATOM20' } });
  assert.equal(q.status, 200);
  assert.equal(q.body.lines[0].price, state.p1.price);
  assert.equal(q.body.subtotal, state.p1.price * 2);
  if (q.body.coupon) assert.ok(q.body.discount > 0);
  const badCoupon = await call('POST', '/api/v1/cart/quote', { body: { items: [{ productId: state.p1.id, qty: 1 }], coupon: 'NOPE' } });
  assert.ok(badCoupon.body.couponError);
  const unknown = await call('POST', '/api/v1/cart/quote', { body: { items: [{ productId: 999999, qty: 1 }] } });
  assert.equal(unknown.body.lines[0].available, false);
  assert.equal(unknown.body.total, 0);
});

test('place order requires idempotency key, validates, and replays duplicates', async () => {
  const body = { items: [{ productId: state.p1.id, qty: 1 }, { productId: state.p2.id, qty: 2 }], name: 'مینا رضایی', phone: '09120001122', city: 'تهران', address: 'تهران، خیابان آزادی، پلاک ۱۰' };
  const noKey = await call('POST', '/api/v1/orders', { token: state.tok, body });
  assert.equal(noKey.status, 400);
  const invalid = await call('POST', '/api/v1/orders', { token: state.tok, body: { ...body, phone: '1', address: '' }, headers: { 'Idempotency-Key': 'test-key-invalid' } });
  assert.equal(invalid.status, 400);
  assert.ok(invalid.body.error.fields.phone && invalid.body.error.fields.address);
  const key = { 'Idempotency-Key': 'test-key-0001' };
  const o1 = await call('POST', '/api/v1/orders', { token: state.tok, body, headers: key });
  assert.equal(o1.status, 201);
  assert.equal(o1.body.order.items.length, 2);
  assert.equal(o1.body.order.status, 'pending');
  const o2 = await call('POST', '/api/v1/orders', { token: state.tok, body, headers: key });
  assert.equal(o2.status, 201);
  assert.equal(o2.headers.get('idempotent-replay'), 'true');
  assert.equal(o2.body.order.code, o1.body.order.code);
  state.code = o1.body.order.code;
  const oos = await call('POST', '/api/v1/orders', { token: state.tok, body: { ...body, items: [{ productId: state.p1.id, qty: 99 }] }, headers: { 'Idempotency-Key': 'test-key-oos-1' } });
  assert.equal(oos.status, 409);
  assert.equal(oos.body.error.code, 'out_of_stock');
});

test('orders list/detail and My Products are scoped to the owner', async () => {
  const l = await call('GET', '/api/v1/orders', { token: state.tok });
  assert.equal(l.body.total, 1);
  assert.equal(l.body.items[0].code, state.code);
  const d = await call('GET', '/api/v1/orders/' + state.code, { token: state.tok });
  assert.equal(d.status, 200);
  assert.ok(d.body.order.address);
  const mp = await call('GET', '/api/v1/my/products', { token: state.tok });
  assert.equal(mp.body.items.length, 2);
  assert.ok(mp.body.items.every(i => i.orderCode === state.code && i.productId));
  // کاربر دیگری با همان شمارهٔ موبایل نمی‌تواند سفارش را ببیند
  const other = await call('POST', '/api/auth/register-customer', { body: { name: 'مهاجم', phone: '09120001122', username: 'evil', password: 'evil1234' } });
  const evil = await call('GET', '/api/v1/orders/' + state.code, { token: other.body.token });
  assert.equal(evil.status, 404);
  const legacy = await call('GET', '/api/my/orders', { token: other.body.token });
  assert.equal(legacy.body.items.length, 0);
  state.evilTok = other.body.token;
});

test('guest order via legacy website endpoint can be claimed with code + phone', async () => {
  const g = await call('POST', '/api/public/order', { body: { items: [{ id: state.p1.id, qty: 1, price: 1 }], name: 'مهمان', phone: '09350000000', address: 'شیراز، خیابان زند، کوچه ۵' } });
  assert.equal(g.status, 201);
  assert.ok(g.body.total >= state.p1.price, 'legacy endpoint must use DB price');
  const wrong = await call('POST', '/api/v1/orders/claim', { token: state.tok, body: { code: g.body.code, phone: '09350000001' } });
  assert.equal(wrong.status, 404);
  const ok = await call('POST', '/api/v1/orders/claim', { token: state.tok, body: { code: g.body.code, phone: '09350000000' } });
  assert.equal(ok.status, 200);
  const again = await call('POST', '/api/v1/orders/claim', { token: state.evilTok, body: { code: g.body.code, phone: '09350000000' } });
  assert.equal(again.status, 409);
});

test('legacy website order with a non-catalog item goes to review', async () => {
  const r = await call('POST', '/api/public/order', { body: { items: [{ title: 'محصول ثابت کاتالوگ', qty: 1, price: 100000 }], name: 'مهمان', phone: '09350000002', address: 'اصفهان، خیابان چهارباغ' } });
  assert.equal(r.status, 201);
  const t = await call('GET', `/api/v1/track?code=${r.body.code}&phone=09350000002`);
  assert.equal(t.body.order.status, 'review');
  assert.ok(!('address' in t.body.order), 'public tracking must not leak the address');
});

test('status changes from the admin panel create notifications', async () => {
  const rows = (await call('GET', '/api/orders?limit=200', { token: state.adminTok })).body.items.filter(o => o.code === state.code);
  for (const row of rows) await call('PUT', '/api/orders/' + row.id, { token: state.adminTok, body: { status: 'shipping' } });
  const n = await call('GET', '/api/v1/notifications', { token: state.tok });
  const types = n.body.items.map(i => i.type);
  assert.ok(types.includes('order_placed'));
  assert.equal(types.filter(t => t === 'order_status').length, 1, 'multi-line order produces one notification');
  assert.ok(n.body.unread >= 2);
  const since = await call('GET', '/api/v1/notifications?since=' + n.body.items[0].id, { token: state.tok });
  assert.equal(since.body.items.length, 0);
  await call('POST', '/api/v1/notifications/read', { token: state.tok, body: { all: true } });
  assert.equal((await call('GET', '/api/v1/notifications', { token: state.tok })).body.unread, 0);
  const d = await call('GET', '/api/v1/orders/' + state.code, { token: state.tok });
  assert.equal(d.body.order.status, 'shipping');
});

test('support tickets: user sees admin reply and gets notified', async () => {
  const t = await call('POST', '/api/v1/support/messages', { token: state.tok, body: { subject: 'سؤال', body: 'زمان ارسال سفارش من کی است؟' } });
  assert.equal(t.status, 201);
  await call('PUT', '/api/messages/' + t.body.item.id, { token: state.adminTok, body: { reply: 'فردا ارسال می‌شود.', status: 'closed', owner: 'evil' } });
  const l = await call('GET', '/api/v1/support/messages', { token: state.tok });
  assert.equal(l.body.items[0].reply, 'فردا ارسال می‌شود.');
  const n = await call('GET', '/api/v1/notifications', { token: state.tok });
  assert.equal(n.body.items[0].type, 'support_reply');
  // مالک پیام از پنل قابل تغییر نیست
  assert.equal((await call('GET', '/api/v1/support/messages', { token: state.evilTok })).body.items.length, 0);
});

test('reviews require login and are moderated', async () => {
  const anon = await call('POST', `/api/v1/products/${state.p1.id}/reviews`, { body: { body: 'عالی بود', rating: 5 } });
  assert.equal(anon.status, 401);
  const r = await call('POST', `/api/v1/products/${state.p1.id}/reviews`, { token: state.tok, body: { body: 'کیفیت عالی بود', rating: 5 } });
  assert.equal(r.status, 201);
  assert.equal(r.body.status, 'pending');
});

test('offices: list, detail and messaging', async () => {
  const l = await call('GET', '/api/v1/offices');
  assert.equal(l.status, 200);
  if (!l.body.items.length) return;
  const d = await call('GET', '/api/v1/offices/' + l.body.items[0].id);
  assert.ok(!('owner' in d.body.office), 'office account username must not leak');
  const m = await call('POST', `/api/v1/offices/${l.body.items[0].id}/messages`, { token: state.tok, body: { body: 'سلام، ساعت کاری دفتر؟' } });
  assert.equal(m.status, 201);
  const mine = await call('GET', '/api/v1/support/office-messages', { token: state.tok });
  assert.equal(mine.body.items.length, 1);
});

test('maintenance mode blocks the API except config', async () => {
  await call('PUT', '/api/settings', { token: state.adminTok, body: { maintenance: '1' } });
  const p = await call('GET', '/api/v1/products');
  assert.equal(p.status, 503);
  assert.equal(p.body.error.code, 'maintenance');
  const c = await call('GET', '/api/v1/config');
  assert.equal(c.body.maintenance, true);
  await call('PUT', '/api/settings', { token: state.adminTok, body: { maintenance: '0' } });
  assert.equal((await call('GET', '/api/v1/products')).status, 200);
});

test('unknown routes and malformed JSON return structured errors', async () => {
  const r = await call('GET', '/api/v1/nope');
  assert.equal(r.status, 404);
  assert.equal(r.body.error.code, 'not_found');
  const res = await fetch(BASE + '/api/v1/cart/quote', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{bad' });
  assert.equal(res.status, 400);
  assert.equal((await res.json()).error.code, 'invalid_json');
});

test('change password revokes other sessions', async () => {
  const r = await call('POST', '/api/v1/auth/change-password', { token: state.tok, body: { current: 'mina-pass-1', next: 'mina-pass-2' } });
  assert.equal(r.status, 200);
  const old = await call('POST', '/api/v1/auth/refresh', { body: { refreshToken: state.refresh } });
  assert.equal(old.status, 401);
  state.tok = r.body.accessToken;
});

test('account deletion anonymises and blocks login', async () => {
  const wrong = await call('DELETE', '/api/v1/me', { token: state.tok, body: { password: 'x' } });
  assert.equal(wrong.status, 400);
  const ok = await call('DELETE', '/api/v1/me', { token: state.tok, body: { password: 'mina-pass-2' } });
  assert.equal(ok.status, 200);
  assert.equal((await call('GET', '/api/v1/me', { token: state.tok })).status, 401);
  const login = await call('POST', '/api/v1/auth/login', { body: { username: 'mina', password: 'mina-pass-2' } });
  assert.equal(login.status, 401);
});
