/* سرویس مشترک سفارش — قیمت‌گذاری، کد تخفیف، هزینهٔ ارسال و ثبت سفارش.
   هم مسیر قدیمی سایت (/api/public/order) و هم API اپ (/api/v1/orders) از همین
   منطق استفاده می‌کنند تا مبلغ نهایی همیشه فقط سمت سرور محاسبه شود. */
const db = require('./db');
const { str, int } = require('./validate');
const notify = require('./notify');

const FA_DIGITS = '۰۱۲۳۴۵۶۷۸۹';

function setting(key, def) {
  const r = db.prepare('SELECT value FROM settings WHERE key=?').get(key);
  return r && r.value !== '' ? r.value : def;
}
function numSetting(key, def) {
  const n = parseInt(String(setting(key, def)).replace(/[۰-۹]/g, d => FA_DIGITS.indexOf(d)).replace(/[^0-9]/g, ''), 10);
  return isNaN(n) ? def : n;
}

function findOffer(code) {
  code = str(code, 40).replace(/[۰-۹]/g, d => FA_DIGITS.indexOf(d)).trim().toUpperCase();
  if (!code) return null;
  const o = db.prepare('SELECT code,title,kind,amount,used,quota,status FROM offers WHERE UPPER(code)=?').get(code);
  if (!o || o.status !== 'active' || (o.quota > 0 && o.used >= o.quota)) return null;
  return o;
}

class OrderError extends Error {
  constructor(code, message, status = 400) { super(message); this.code = code; this.status = status; }
}

/* اقلام را با قیمت پایگاه داده قیمت‌گذاری می‌کند.
   strict=true (اپ): فقط محصولات موجود در پایگاه داده پذیرفته می‌شوند.
   strict=false (سایت قدیمی): محصولات ثابت کاتالوگ HTML با قیمت ارسالی پذیرفته
   می‌شوند اما «unverified» علامت می‌خورند تا سفارش برای بررسی مدیر برود. */
function priceLines(rawItems, { strict }) {
  const items = Array.isArray(rawItems) ? rawItems.slice(0, 50) : [];
  if (!items.length) throw new OrderError('cart_empty', 'سبد خرید خالی است.');
  const byId = db.prepare("SELECT id,title,price,seller,stock,image,status FROM products WHERE id=?");
  const byTitle = db.prepare("SELECT id,title,price,seller,stock,image,status FROM products WHERE title=? AND status='active'");
  return items.map(it => {
    const qty = Math.max(1, Math.min(99, int(it.qty, 1)));
    const rawId = it.productId != null ? it.productId : it.id;
    let p = rawId != null && /^\d+$/.test(String(rawId)) ? byId.get(+rawId) : null;
    if (!p && !strict && it.title) p = byTitle.get(str(it.title, 300).trim());
    if (p) {
      const available = p.status === 'active';
      return {
        productId: p.id, title: p.title, seller: p.seller || 'اتم ۳۱۳', image: p.image || null,
        price: p.price, qty, amount: qty * p.price, stock: p.stock, available,
        inStock: available && p.stock >= qty, verified: true,
      };
    }
    if (strict) {
      return { productId: rawId != null ? +rawId || null : null, title: str(it.title, 300) || 'محصول نامشخص', seller: '', image: null,
        price: 0, qty, amount: 0, stock: 0, available: false, inStock: false, verified: true };
    }
    const price = Math.max(0, int(it.price, 0));
    return { productId: null, title: str(it.title, 300), seller: str(it.seller, 120) || 'اتم ۳۱۳', image: null,
      price, qty, amount: qty * price, stock: 1, available: true, inStock: true, verified: false };
  });
}

// محاسبهٔ کامل صورت‌حساب (بدون ثبت)
function quote(rawItems, couponCode, { strict }) {
  const lines = priceLines(rawItems, { strict });
  const billable = lines.filter(l => l.available);
  const subtotal = billable.reduce((s, l) => s + l.amount, 0);
  let offer = null, couponError = null;
  if (couponCode && String(couponCode).trim()) {
    offer = findOffer(couponCode);
    if (!offer) couponError = 'کد تخفیف نامعتبر یا منقضی است.';
  }
  let discount = 0;
  if (offer) discount = offer.kind === 'percent' ? Math.round(subtotal * Math.min(100, offer.amount) / 100) : Math.min(subtotal, offer.amount);
  const afterDisc = subtotal - discount;
  const freeShippingMin = numSetting('free_shipping_min', 500000);
  const shippingCost = numSetting('shipping_cost', 0);
  const shipping = billable.length === 0 ? 0 : (afterDisc >= freeShippingMin ? 0 : shippingCost);
  const minOrder = numSetting('min_order', 0);
  return {
    lines, subtotal, discount, shipping, total: afterDisc + shipping,
    minOrder, freeShippingMin,
    coupon: offer ? { code: offer.code, title: offer.title || '', kind: offer.kind, amount: offer.amount } : null,
    couponError,
  };
}

/* ثبت سفارش در یک تراکنش. خروجی: { code, subtotal, discount, shipping, total } */
function place({ items, coupon, name, phone, address, city, note, owner, ip, strict }) {
  const q = quote(items, coupon, { strict });
  if (q.couponError) throw new OrderError('coupon_invalid', q.couponError);
  const bad = q.lines.find(l => !l.available);
  if (bad) throw new OrderError('product_unavailable', '«' + bad.title + '» دیگر در دسترس نیست. آن را از سبد خرید حذف کنید.', 409);
  if (strict) {
    const low = q.lines.find(l => !l.inStock);
    if (low) throw new OrderError('out_of_stock', 'موجودی «' + low.title + '» کافی نیست' + (low.stock > 0 ? ' (موجودی: ' + low.stock + ')' : '') + '.', 409);
  }
  if (q.subtotal < q.minOrder)
    throw new OrderError('min_order', 'حداقل مبلغ سفارش ' + q.minOrder.toLocaleString('fa-IR') + ' تومان است.');

  const lines = q.lines.map(l => ({ ...l }));
  // تخفیف به نسبت مبلغ روی اقلام پخش می‌شود تا جمع ردیف‌ها = مبلغ پرداختی
  let left = q.discount;
  lines.forEach((l, i) => {
    const share = i === lines.length - 1 ? left : Math.round(q.discount * l.amount / (q.subtotal || 1));
    l.amount -= share; left -= share;
  });
  const noteText = [
    q.coupon ? 'کد تخفیف ' + q.coupon.code + ' (−' + q.discount + ' ت)' : '',
    q.shipping ? 'هزینهٔ ارسال ' + q.shipping + ' ت' : 'ارسال رایگان',
    str(note, 300),
  ].filter(Boolean).join(' · ');
  // اقلامی که قیمتشان از پایگاه داده تأیید نشده، ابتدا توسط مدیر بررسی می‌شوند
  const status = lines.some(l => !l.verified) ? 'review' : 'pending';

  const ins = db.prepare("INSERT INTO orders (code,customer,product,seller,amount,status,phone,address,owner,note,product_id,qty) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
  let code;
  db.transaction(() => {
    const last = db.prepare('SELECT MAX(CAST(code AS INTEGER)) m FROM orders').get().m || 1000;
    code = String(Math.max(1000, last) + 1);
    lines.forEach((l, i) => {
      ins.run(code, name, l.title + (l.qty > 1 ? ' ×' + l.qty : ''), l.seller, l.amount + (i === 0 ? q.shipping : 0),
        status, phone, address + (city ? '، ' + city : ''), owner || '', noteText, l.productId, l.qty);
    });
    if (q.coupon) db.prepare('UPDATE offers SET used=used+1 WHERE code=?').run(q.coupon.code);
    const c = db.prepare('SELECT id FROM customers WHERE phone=?').get(phone);
    if (c) db.prepare('UPDATE customers SET orders_count=orders_count+1, total_spent=total_spent+? WHERE id=?').run(q.total, c.id);
    else db.prepare('INSERT INTO customers (name,phone,email,city,total_spent,orders_count) VALUES (?,?,?,?,?,1)').run(name, phone, '', city || '', q.total);
    db.prepare('INSERT INTO activity_log (actor,action,target,ip) VALUES (?,?,?,?)').run(name, 'ثبت سفارش', '#' + code, ip || '');
  })();
  notify.orderPlaced(code, owner, q.total);
  return { code, status, subtotal: q.subtotal, discount: q.discount, shipping: q.shipping, total: q.total };
}

module.exports = { quote, place, findOffer, setting, numSetting, OrderError };
