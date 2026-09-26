/* اعلان‌های حساب کاربری — یک منبع مشترک برای سایت و اپ اندروید.
   اعلان‌ها در جدول notifications ذخیره می‌شوند؛ اپ آن‌ها را با همگام‌سازی
   دوره‌ای (یا در آینده با پوش FCM از روی جدول devices) دریافت می‌کند.
   ثبت اعلان هرگز نباید جریان اصلی درخواست را متوقف کند. */
const db = require('./db');

const ORDER_STATUS_FA = {
  pending: 'در انتظار تأیید',
  review: 'در حال بررسی',
  shipping: 'در حال ارسال',
  delivered: 'تحویل شد',
  returned: 'مرجوع شد',
};

function userIdByUsername(username) {
  if (!username) return null;
  const r = db.prepare("SELECT id FROM admins WHERE username=? AND status='active'").get(username);
  return r ? r.id : null;
}

function push(userId, type, title, body, ref) {
  if (!userId) return;
  try {
    // سفارش‌های چندقلمی ردیف‌به‌ردیف به‌روز می‌شوند؛ اعلان تکراری در ۱۰ دقیقه ثبت نمی‌شود
    const dup = db.prepare("SELECT id FROM notifications WHERE user_id=? AND type=? AND ref=? AND title=? AND created_at > datetime('now','-10 minutes')")
      .get(userId, type, ref == null ? '' : String(ref), title);
    if (dup) return;
    db.prepare('INSERT INTO notifications (user_id,type,title,body,ref) VALUES (?,?,?,?,?)')
      .run(userId, type, title, body || '', ref == null ? '' : String(ref));
  } catch (e) { /* اعلان اختیاری است */ }
}

// صاحبان یک سفارش (بر اساس حساب؛ سفارش‌های مهمان صاحب ندارند)
function orderOwners(code) {
  return db.prepare("SELECT DISTINCT owner FROM orders WHERE code=? AND owner<>''").all(String(code))
    .map(r => userIdByUsername(r.owner)).filter(Boolean);
}

function orderPlaced(code, owner, total) {
  push(userIdByUsername(owner), 'order_placed', 'سفارش #' + code + ' ثبت شد',
    'مبلغ ' + Number(total || 0).toLocaleString('fa-IR') + ' تومان — هماهنگی پرداخت و ارسال به‌زودی با شما انجام می‌شود.', code);
}

function orderStatusChanged(code, prev, next) {
  if (!code || prev === next || !ORDER_STATUS_FA[next]) return;
  orderOwners(code).forEach(uid =>
    push(uid, 'order_status', 'سفارش #' + code + ': ' + ORDER_STATUS_FA[next], 'وضعیت سفارش شما به‌روزرسانی شد.', code));
}

function supportReplied(msg) {
  if (!msg || !msg.owner) return;
  push(userIdByUsername(msg.owner), 'support_reply', 'پاسخ پشتیبانی: ' + (msg.subject || 'پیام شما'),
    String(msg.reply || '').slice(0, 160), msg.id);
}

function officeReplied(msg) {
  if (!msg || !msg.sender_owner) return;
  push(userIdByUsername(msg.sender_owner), 'office_reply', 'پاسخ ' + (msg.office || 'دفتر محله'),
    String(msg.reply || '').slice(0, 160), msg.id);
}

module.exports = { push, orderPlaced, orderStatusChanged, supportReplied, officeReplied, ORDER_STATUS_FA };
