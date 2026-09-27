package ir.atom313.app.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.component.AtomImage
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.RatingBadge
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.ui.component.AtomTopBar
import ir.atom313.app.ui.util.LocalWindowWidth

/** ویترین یک فروشگاه — معرفی فروشنده + محصولات همان فروشگاه. */
@Composable
fun SellerScreen(
    onBack: () -> Unit,
    onProduct: (Long) -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: SellerViewModel = hiltViewModel(),
) {
    val state by viewModel.seller.collectAsStateWithLifecycle()
    val wished by viewModel.wishedIds.collectAsStateWithLifecycle()
    val products = viewModel.products.collectAsLazyPagingItems()
    val seller = state.data

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = seller?.name ?: "فروشگاه", onBack = onBack)
        if (state.isFullError) {
            ErrorState(state.error!!, onRetry = viewModel::retry, onHome = onBack, modifier = Modifier.fillMaxSize())
            return@Column
        }
        ProductGrid(
            items = products,
            wished = wished,
            onProduct = onProduct,
            onToggleWish = { viewModel.toggleWish(it, onMessage) },
            onAddToCart = { viewModel.addToCart(it, onMessage) },
            columns = gridColumnsFor(LocalWindowWidth.current),
            contentPadding = contentPadding,
            emptyTitle = "این فروشگاه هنوز محصولی ندارد",
            emptyMessage = "به‌زودی محصولات این فروشگاه اینجا نمایش داده می‌شود.",
            header = {
                if (seller != null) {
                    Column(Modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md))
                                .background(MaterialTheme.colorScheme.surface).padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(64.dp).clip(CircleShape)) {
                                AtomImage(seller.avatar, seller.name, Modifier.fillMaxSize(), seed = seller.id, placeholderIconSize = 28.dp)
                            }
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(seller.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    listOf(seller.category, seller.city).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RatingBadge(seller.rating, seller.sales.coerceAtLeast(1))
                                    if (seller.productCount > 0) {
                                        Spacer(Modifier.width(Spacing.sm))
                                        Text(
                                            "${Formatters.number(seller.productCount.toLong())} محصول",
                                            style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary,
                                        )
                                    }
                                }
                            }
                        }
                        if (seller.bio.isNotBlank()) {
                            Spacer(Modifier.height(Spacing.sm))
                            Text(seller.bio, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary)
                        }
                    }
                }
            },
        )
    }
}
