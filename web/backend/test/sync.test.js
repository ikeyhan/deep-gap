/* تست‌های همگام‌سازی سایت و اپ روی یک سرور واقعی:
   هر تغییر در یک سمت باید بی‌درنگ در سمت دیگر دیده شود.
   اجرا: npm test */
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const PORT = 4000 + Math.floor(Math.random() * 90);
const BASE = `http://127.0.0.1:${PORT}`;
const DATA = fs.mkdtempSync(path.join(os.tmpdir(), 'atom-sync-'));
let server;

async function call(p, { method = 'GET', body, token, headers = {} } = {}) {
  const h = { ...headers };
  if (body) h['Content-Type'] = 'application/json';
  if (token) h.Authorization = 'Bearer ' + token;
  const r = await fetch(BASE + p, { method, headers: h, body: body ? JSON.stringify(body) : undefined });
  return { status: r.status, body: await r.json().catch(() => null), headers: r.headers };
}

before(async () => {
  server = spawn(process.execPath, ['server.js'], {
    cwd: path.join(__dirname, '..'),
    env: { ...process.env, PORT: String(PORT), ATOM_DATA_DIR: DATA, JWT_SECRET: 'sync-secret-'.padEnd(48, 'y'), SEED_DEMO: '1' },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  for (let i = 0; i < 50; i++) {
    try { if ((await fetch(BASE + '/api/health')).ok) return; } catch (e) { /* wait */ }
    await new Promise(r => setTimeout(r, 100));
  }
  throw new Error('server did not start');
});
after(() => { server.kill(); fs.rmSync(DATA, { recursive: true, force: true }); });

const S = {}; // وضعیت مشترک بین مراحل

test('an account created on the website works in the app', async () => {
  S.u = 'sync' + Date.now().toString().slice(-6);
  S.phone = '0913' + String(Date.now()).slice(-7);
  const web = await call('/api/auth/register-customer', { method: 'POST', body: { name: 'کاربر همگام', phone: S.phone, username: S.u, password: 'syncpass123', city: 'شیراز' } });
  assert.equal(web.status, 201);
  S.webTok = web.body.token;
  const app = await call('/api/v1/auth/login', { method: 'POST', body: { username: S.u, password: 'syncpass123' } });
  assert.equal(app.status, 200);
  assert.equal(app.body.user.username, S.u);
  S.appTok = app.body.accessToken;
  S.products = (await call('/api/v1/products?limit=3')).body.items;
});

test('a wishlist item added in the app is visible on the website', async () => {
  await call(`/api/v1/wishlist/${S.products[0].id}`, { method: 'PUT', token: S.appTok });
  const webWish = await call('/api/v1/wishlist', { token: S.webTok });
  assert.ok(webWish.body.ids.includes(S.products[0].id));
});

test('a profile edited in the app appears on the website', async () => {
  await call('/api/v1/me', { method: 'PUT', token: S.appTok, body: { name: 'نام تازه', phone: S.phone, email: 'sync@example.com', city: 'اصفهان', address: 'اصفهان، چهارباغ، پلاک ۵' } });
  const webProfile = await call('/api/my/profile', { token: S.webTok });
  assert.equal(webProfile.body.user.name, 'نام تازه');
  assert.equal(webProfile.body.user.city, 'اصفهان');
});

test('an order placed in the app shows on the website and in My Products', async () => {
  const placed = await call('/api/v1/orders', {
    method: 'POST', token: S.appTok, headers: { 'Idempotency-Key': 'sync-test-' + Date.now() },
    body: { items: [{ productId: S.products[0].id, qty: 2 }], name: 'کاربر همگام', phone: S.phone, city: 'اصفهان', address: 'اصفهان، چهارباغ، پلاک ۵' },
  });
  assert.equal(placed.status, 201);
  S.code = placed.body.order.code;
  const webOrders = await call('/api/my/orders', { token: S.webTok });
  assert.ok(webOrders.body.items.some(o => o.code === S.code));
  const myProducts = await call('/api/v1/my/products', { token: S.appTok });
  assert.ok(myProducts.body.items.some(i => i.orderCode === S.code));
});

test('a status change in the admin panel notifies the app and updates everywhere', async () => {
  const admin = await call('/api/auth/login', { method: 'POST', body: { username: 'admin', password: 'atom313@' } });
  S.adminTok = admin.body.token;
  const rows = (await call('/api/orders?limit=200', { token: S.adminTok })).body.items.filter(o => o.code === S.code);
  for (const r of rows) await call('/api/orders/' + r.id, { method: 'PUT', token: S.adminTok, body: { status: 'shipping' } });
  const notifs = await call('/api/v1/notifications', { token: S.appTok });
  assert.ok(notifs.body.items.some(n => n.type === 'order_status' && n.ref === S.code));
  const refreshed = await call('/api/v1/orders/' + S.code, { token: S.appTok });
  assert.equal(refreshed.body.order.status, 'shipping');
});

test('a support reply from the panel reaches the user', async () => {
  const ticket = await call('/api/v1/support/messages', { method: 'POST', token: S.appTok, body: { subject: 'تست همگام', body: 'سؤال آزمایشی برای بررسی همگام‌سازی.' } });
  await call('/api/messages/' + ticket.body.item.id, { method: 'PUT', token: S.adminTok, body: { reply: 'پاسخ تیم پشتیبانی.', status: 'closed' } });
  const appTickets = await call('/api/v1/support/messages', { token: S.appTok });
  assert.equal(appTickets.body.items[0].reply, 'پاسخ تیم پشتیبانی.');
});

test('resubmitting with the same key does not create a duplicate order', async () => {
  const key = 'dupe-' + Date.now();
  const payload = { items: [{ productId: S.products[0].id, qty: 1 }], name: 'کاربر همگام', phone: S.phone, city: 'اصفهان', address: 'اصفهان، چهارباغ، پلاک ۵' };
  const o1 = await call('/api/v1/orders', { method: 'POST', token: S.appTok, headers: { 'Idempotency-Key': key }, body: payload });
  const o2 = await call('/api/v1/orders', { method: 'POST', token: S.appTok, headers: { 'Idempotency-Key': key }, body: payload });
  assert.equal(o1.body.order.code, o2.body.order.code);
  assert.equal(o2.headers.get('idempotent-replay'), 'true');
});

test('admin panel accounts can never reach the app API', async () => {
  assert.equal((await call('/api/v1/me', { token: S.adminTok })).status, 403);
  assert.equal((await call('/api/v1/auth/login', { method: 'POST', body: { username: 'admin', password: 'atom313@' } })).status, 403);
});
