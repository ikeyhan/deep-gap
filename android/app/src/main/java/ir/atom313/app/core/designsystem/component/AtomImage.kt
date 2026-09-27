package ir.atom313.app.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import ir.atom313.app.BuildConfig
import ir.atom313.app.R
import ir.atom313.app.core.common.ImageUrls
import ir.atom313.app.core.designsystem.theme.AtomTheme

/** پس‌زمینه‌های ملایم برای محصولات بدون تصویر (مانند کلاس‌های ph-1..8 سایت). */
private val placeholderTints = listOf(
    Color(0xFF149B3E), Color(0xFF0E7A38), Color(0xFFF0A020), Color(0xFF1BB24B),
    Color(0xFF2F8F83), Color(0xFF6B8E23), Color(0xFFB5651D), Color(0xFF4A7C59),
)

/**
 * تصویر راه دور با کش حافظه/دیسک (Coil)، اسکلتون هنگام بارگذاری و جای‌نگهدار برند
 * برای تصویر خالی یا خطا. مسیرهای نسبی سرور به آدرس کامل تبدیل می‌شوند.
 */
@Composable
fun AtomImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    seed: Long = 0,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIconSize: Dp = 36.dp,
) {
    val url = remember(path) { ImageUrls.resolve(BuildConfig.API_BASE_URL, path) }
    if (url == null) {
        BrandPlaceholder(seed, modifier, placeholderIconSize)
        return
    }
    val context = LocalContext.current
    val request = remember(url) { ImageRequest.Builder(context).data(url).crossfade(200).build() }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { Box(Modifier.fillMaxSize().shimmer()) },
        error = { BrandPlaceholder(seed, Modifier.fillMaxSize(), placeholderIconSize) },
    )
}

@Composable
fun BrandPlaceholder(seed: Long, modifier: Modifier = Modifier, iconSize: Dp = 36.dp) {
    val tint = placeholderTints[(seed.mod(placeholderTints.size.toLong())).toInt()]
    val alpha = if (AtomTheme.colors.isDark) 0.22f else 0.12f
    Box(
        modifier.background(Brush.linearGradient(listOf(tint.copy(alpha = alpha), tint.copy(alpha = alpha * 0.4f)))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo_mark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(iconSize).alpha(0.55f),
        )
    }
}

@Composable
fun LogoMark(modifier: Modifier = Modifier, tint: Color? = null) {
    Image(
        painter = painterResource(R.drawable.ic_logo_mark),
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier,
    )
}

