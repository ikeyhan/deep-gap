package ir.atom313.app.core.common

import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.ULocale
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * قالب‌بندی‌های فارسی. فونت Vazirmatn-FD ارقام لاتین را فارسی نمایش می‌دهد،
 * بنابراین اعداد با ارقام لاتین و جداکنندهٔ فارسی «٬» ساخته می‌شوند.
 */
object Formatters {
    private val moneyFormat = ThreadLocal.withInitial {
        DecimalFormat("#,###", DecimalFormatSymbols(Locale.US).apply { groupingSeparator = '٬' })
    }

    /** ۲٬۴۵۰٬۰۰۰ */
    fun number(value: Long): String = moneyFormat.get()!!.format(value)

    /** ۲٬۴۵۰٬۰۰۰ تومان */
    fun toman(value: Long): String = number(value) + " تومان"

    private val PERSIAN = ULocale("fa_IR@calendar=persian")

    /** تاریخ‌های سرور (SQLite) به‌صورت «yyyy-MM-dd HH:mm:ss» و به وقت UTC هستند. */
    fun parseServerDate(raw: String?): Date? {
        if (raw.isNullOrBlank()) return null
        val patterns = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd")
        for (p in patterns) {
            try {
                return SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(raw)
            } catch (_: Exception) { /* next */ }
        }
        return null
    }

    // قالب‌بندی با «اسکلتون» ICU: ترتیب و جداکننده‌ها را خود تقویم فارسی تعیین می‌کند
    private fun skeleton(pattern: String) = android.icu.text.DateFormat.getInstanceForSkeleton(pattern, PERSIAN)

    /** ۱۴ مهر ۱۴۰۵ */
    fun persianDate(raw: String?): String {
        val d = parseServerDate(raw) ?: return ""
        return skeleton("dMMMMy").format(d)
    }

    /** ۱۴۰۵/۰۷/۱۴ */
    fun persianShortDate(raw: String?): String {
        val d = parseServerDate(raw) ?: return ""
        return skeleton("yMMdd").format(d)
    }

    /** «۳ ساعت پیش» — برای اعلان‌ها و پیام‌ها */
    fun relative(raw: String?, now: Long = System.currentTimeMillis()): String {
        val d = parseServerDate(raw) ?: return ""
        val diffSec = (now - d.time) / 1000
        if (diffSec < 60) return "همین حالا"
        val f = RelativeDateTimeFormatter.getInstance(ULocale("fa"))
        val (amount, unit) = when {
            diffSec < 3600 -> diffSec / 60 to RelativeDateTimeFormatter.RelativeUnit.MINUTES
            diffSec < 86_400 -> diffSec / 3600 to RelativeDateTimeFormatter.RelativeUnit.HOURS
            diffSec < 7 * 86_400 -> diffSec / 86_400 to RelativeDateTimeFormatter.RelativeUnit.DAYS
            else -> return persianDate(raw)
        }
        return f.format(abs(amount).toDouble(), RelativeDateTimeFormatter.Direction.LAST, unit)
    }
}
