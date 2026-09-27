package ir.atom313.app.core.common

/**
 * تصاویر سرور به‌صورت مسیر نسبی ذخیره می‌شوند (مثل «/uploads/x.jpg» یا
 * «assets/img/products/x.jpg»). این تابع آن‌ها را به آدرس کامل تبدیل می‌کند.
 */
object ImageUrls {
    fun resolve(baseUrl: String, path: String?): String? {
        val p = path?.trim().orEmpty()
        if (p.isEmpty()) return null
        if (p.startsWith("https://") || p.startsWith("http://")) return p
        // data: و javascript: و … پذیرفته نمی‌شوند
        if (p.contains(':')) return null
        return baseUrl.trimEnd('/') + "/" + p.trimStart('/', '.')
    }
}
