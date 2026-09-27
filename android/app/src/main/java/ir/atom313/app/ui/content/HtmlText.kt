package ir.atom313.app.ui.content

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

/**
 * نمایش بومی متن HTML مقاله‌های پنل مدیریت (بدون WebView).
 * تگ‌های اجرایی حذف می‌شوند و فقط قالب‌بندی متنی نگه داشته می‌شود.
 */
@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val annotated = remember(html) { html.toAnnotatedString() }
    Text(
        annotated,
        modifier = modifier,
        style = LocalTextStyle.current.merge(MaterialTheme.typography.bodyLarge),
    )
}

private val SCRIPT_LIKE = Regex(
    "<(script|style|iframe|object|embed|form)[^>]*>.*?</\\1>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)

private fun String.toAnnotatedString(): AnnotatedString {
    val sanitized = SCRIPT_LIKE.replace(this, "")
        // تگ‌های بلوکی به خط جدید تبدیل می‌شوند تا ساختار مقاله حفظ شود
        .replace(Regex("<(h[1-6])[^>]*>", RegexOption.IGNORE_CASE), "<br/><b>")
        .replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), "</b><br/>")
        .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "<br/>• ")
    val spanned = HtmlCompat.fromHtml(sanitized, HtmlCompat.FROM_HTML_MODE_COMPACT)
    return buildAnnotatedString {
        append(spanned.toString().trim())
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span).coerceAtMost(length)
            val end = spanned.getSpanEnd(span).coerceAtMost(length)
            if (start >= end) return@forEach
            when (span) {
                is android.text.style.StyleSpan -> when (span.style) {
                    android.graphics.Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    android.graphics.Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start, end)
                }
                is android.text.style.UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
        }
    }
}
