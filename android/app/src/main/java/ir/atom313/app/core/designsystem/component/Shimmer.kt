package ir.atom313.app.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing

/** افکت شیمر سبک برای اسکلتون‌ها (یک انیمیشن تکرارشونده، بدون بار اضافه روی چیدمان). */
fun Modifier.shimmer(): Modifier = composed {
    val base = AtomTheme.colors.surface2
    val highlight = AtomTheme.colors.border
    val t by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "x",
    )
    background(
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(-600f + 1600f * t, 0f),
            end = Offset(-200f + 1600f * t, 300f),
        ),
    )
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, radius: Dp = Radius.xs) {
    Box(modifier.clip(RoundedCornerShape(radius)).shimmer())
}

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.semantics { contentDescription = "در حال بارگذاری" }) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(1f), Radius.md)
        Spacer(Modifier.height(Spacing.sm))
        SkeletonBox(Modifier.fillMaxWidth(0.9f).height(14.dp))
        Spacer(Modifier.height(6.dp))
        SkeletonBox(Modifier.fillMaxWidth(0.5f).height(12.dp))
        Spacer(Modifier.height(Spacing.sm))
        SkeletonBox(Modifier.fillMaxWidth(0.6f).height(16.dp))
    }
}

@Composable
fun ListItemSkeleton(modifier: Modifier = Modifier, avatar: Boolean = true) {
    Row(modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        if (avatar) {
            SkeletonBox(Modifier.size(56.dp), Radius.sm)
            Spacer(Modifier.width(Spacing.md))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.7f).height(14.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.45f).height(12.dp))
        }
    }
}

@Composable
fun CircleSkeleton(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).shimmer())
}
