package ir.atom313.app.core.common

/** اعتبارسنجی سمت اپ — فقط برای بازخورد سریع؛ مرجع نهایی همیشه سرور است. */
object Validators {
    private const val FA = "۰۱۲۳۴۵۶۷۸۹"
    private const val AR = "٠١٢٣٤٥٦٧٨٩"

    /** ارقام فارسی/عربی → لاتین */
    fun latinDigits(s: String): String = buildString(s.length) {
        for (c in s) {
            val fa = FA.indexOf(c)
            val ar = AR.indexOf(c)
            append(
                when {
                    fa >= 0 -> ('0' + fa)
                    ar >= 0 -> ('0' + ar)
                    else -> c
                },
            )
        }
    }

    fun normalizeMobile(s: String): String = latinDigits(s).filter(Char::isDigit)

    /** موبایل ایران: 09xxxxxxxxx */
    fun isMobile(s: String): Boolean = Regex("^09\\d{9}$").matches(normalizeMobile(s))

    fun isEmail(s: String): Boolean = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(s.trim())

    /** نام کاربری سایت: حروف کوچک/عدد انگلیسی، _ و . — ۳ تا ۳۰ نویسه */
    fun isUsername(s: String): Boolean = Regex("^[a-z0-9_.]{3,30}$").matches(s.trim().lowercase())

    const val MIN_PASSWORD = 8
}
