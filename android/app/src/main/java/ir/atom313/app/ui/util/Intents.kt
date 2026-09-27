package ir.atom313.app.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** اشتراک‌گذاری متن با اپ‌های دیگر (اشتراک محصول، کد سفارش). */
fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
}

/** باز کردن پیوند بیرونی (تلگرام، اینستاگرام، تماس، ایمیل) با پیام مناسب در نبود اپ. */
fun openExternal(context: Context, uri: String) {
    val normalized = when {
        uri.startsWith("http://") || uri.startsWith("https://") || uri.contains(':') -> uri
        else -> "https://$uri"
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "برنامه‌ای برای باز کردن این پیوند یافت نشد.", Toast.LENGTH_SHORT).show()
    }
}

fun dial(context: Context, phone: String) = openExternal(context, "tel:" + phone.filter { it.isDigit() || it == '+' })

fun email(context: Context, address: String) = openExternal(context, "mailto:$address")
