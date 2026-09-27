package ir.atom313.app.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import ir.atom313.app.core.common.Validators
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.component.SecondaryButton
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.ProductFilter
import ir.atom313.app.domain.model.ProductSort
import ir.atom313.app.ui.component.AtomBottomSheet

private val sortLabels = mapOf(
    ProductSort.NEWEST to "جدیدترین",
    ProductSort.POPULAR to "پرفروش‌ترین",
    ProductSort.PRICE_ASC to "ارزان‌ترین",
    ProductSort.PRICE_DESC to "گران‌ترین",
    ProductSort.RATING to "بیشترین امتیاز",
)

/** فیلتر و مرتب‌سازی در شیت پایین — الگوی موبایل به‌جای ستون فیلتر سایت. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    filter: ProductFilter,
    categories: List<String>,
    onApply: (ProductFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(filter) }
    var minText by remember { mutableStateOf(filter.minPrice?.toString().orEmpty()) }
    var maxText by remember { mutableStateOf(filter.maxPrice?.toString().orEmpty()) }

    fun currentDraft(): ProductFilter = draft.copy(
        minPrice = Validators.latinDigits(minText).filter(Char::isDigit).toLongOrNull(),
        maxPrice = Validators.latinDigits(maxText).filter(Char::isDigit).toLongOrNull(),
    )

    AtomBottomSheet(onDismiss = onDismiss, title = "فیلتر و مرتب‌سازی") {
        Column(Modifier.padding(horizontal = Spacing.screen)) {
            Text("مرتب‌سازی", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ProductSort.entries.forEach { sort ->
                    FilterChip(
                        selected = draft.sort == sort,
                        onClick = { draft = draft.copy(sort = sort) },
                        label = { Text(sortLabels.getValue(sort)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AtomTheme.colors.brandSoft,
                            selectedLabelColor = AtomTheme.colors.brandOnSoft,
                        ),
                    )
                }
            }

            if (categories.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.lg))
                Text("دسته‌بندی", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.sm))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), maxLines = 4) {
                    categories.take(14).forEach { c ->
                        FilterChip(
                            selected = draft.category == c,
                            onClick = { draft = draft.copy(category = if (draft.category == c) null else c) },
                            label = { Text(c, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AtomTheme.colors.brandSoft,
                                selectedLabelColor = AtomTheme.colors.brandOnSoft,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            Text("محدودهٔ قیمت (تومان)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                AtomTextField(
                    value = minText, onValueChange = { minText = it }, label = "از", kind = FieldKind.NUMBER,
                    maxLength = 12, modifier = Modifier.weight(1f),
                )
                AtomTextField(
                    value = maxText, onValueChange = { maxText = it }, label = "تا", kind = FieldKind.NUMBER,
                    maxLength = 12, modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(Spacing.md))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("فقط کالاهای موجود", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = draft.inStockOnly, onCheckedChange = { draft = draft.copy(inStockOnly = it) })
            }

            Spacer(Modifier.height(Spacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SecondaryButton(
                    "حذف فیلترها",
                    onClick = { draft = ProductFilter(query = filter.query); minText = ""; maxText = "" },
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton("اعمال", onClick = { onApply(currentDraft()) }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
