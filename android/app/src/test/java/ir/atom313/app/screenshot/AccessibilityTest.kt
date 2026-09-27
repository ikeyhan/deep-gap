package ir.atom313.app.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import ir.atom313.app.core.datastore.ThemeMode
import ir.atom313.app.core.designsystem.component.ProductCard
import ir.atom313.app.core.designsystem.component.QuantityStepper
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.Product
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * نگهبان دسترس‌پذیری تعامل:
 *  - هر عنصر قابل‌لمس باید دست‌کم ۴۸×۴۸ dp باشد (راهنمای متریال و WCAG 2.5.8).
 *  - هر عنصر قابل‌لمس باید برچسبی برای صفحه‌خوان داشته باشد (متن یا contentDescription).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "fa-rXX-w411dp-h891dp-xhdpi", application = android.app.Application::class)
class AccessibilityTest {

    @get:Rule val compose = createComposeRule()

    private val sample = Product(
        id = 1, title = "کیف چرم طبیعی دست‌دوز مدل رویا", category = "پوشاک", seller = "آترا چرم",
        sellerId = 1, price = 2_450_000, stock = 4, inStock = true, image = null,
        rating = 4.7, reviewCount = 12,
    )

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            AtomTheme(ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
    }

    /** همهٔ گره‌های معنایی که کاربر می‌تواند لمس کند. */
    private fun clickableNodes(): List<SemanticsNode> {
        val all = mutableListOf<SemanticsNode>()
        fun walk(node: SemanticsNode) {
            all += node
            node.children.forEach(::walk)
        }
        walk(compose.onRoot().fetchSemanticsNode())
        return all.filter {
            it.config.contains(SemanticsActions.OnClick) && !it.config.contains(SemanticsProperties.Disabled)
        }
    }

    private fun label(node: SemanticsNode): String {
        val config = node.config
        val description = if (config.contains(SemanticsProperties.ContentDescription)) {
            config[SemanticsProperties.ContentDescription].joinToString()
        } else ""
        val text = if (config.contains(SemanticsProperties.Text)) {
            config[SemanticsProperties.Text].joinToString { it.text }
        } else ""
        val action = if (config.contains(SemanticsActions.OnClick)) {
            config[SemanticsActions.OnClick].label.orEmpty()
        } else ""
        return (description + text + action).trim()
    }

    /**
     * ناحیهٔ لمس سنجیده می‌شود، نه اندازهٔ بصری: Compose هدف لمسی دکمه‌های کوچک‌تر را
     * تا حداقل ۴۸dp گسترش می‌دهد، پس این همان چیزی است که انگشت کاربر با آن سروکار دارد.
     */
    private fun assertTouchTargets(screen: String) {
        val density = compose.density
        val nodes = clickableNodes()
        // اگر درخت معنایی خالی باشد تست نباید الکی سبز شود
        assertTrue("$screen: no touch targets were found — the test would pass vacuously", nodes.isNotEmpty())
        val tooSmall = nodes.mapNotNull { node ->
            val bounds = node.touchBoundsInRoot
            val width = with(density) { bounds.width.toDp() }
            val height = with(density) { bounds.height.toDp() }
            if (width <= 0.dp || height <= 0.dp) null
            else if (width < 48.dp || height < 48.dp) {
                "«${label(node).ifBlank { "unlabelled" }}» = ${width.value.toInt()}×${height.value.toInt()} dp"
            } else null
        }
        assertTrue(
            "$screen: these touch targets are smaller than 48×48 dp:\n  " + tooSmall.joinToString("\n  "),
            tooSmall.isEmpty(),
        )
    }

    private fun assertLabels(screen: String) {
        val nodes = clickableNodes()
        assertTrue("$screen: no touch targets were found — the test would pass vacuously", nodes.isNotEmpty())
        val unlabelled = nodes.filter { label(it).isBlank() }
        assertTrue(
            "$screen: these touch targets have no screen-reader label: " +
                unlabelled.joinToString { it.config.toString().take(40) },
            unlabelled.isEmpty(),
        )
    }

    @Test fun productCardIsReachable() {
        show {
            Row(Modifier.padding(Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ProductCard(sample, wished = true, onClick = {}, onToggleWish = {}, onAddToCart = {}, modifier = Modifier.width(168.dp))
            }
        }
        assertTouchTargets("product card")
        assertLabels("product card")
    }

    @Test fun quantityStepperIsReachable() {
        show {
            Column(Modifier.padding(Spacing.screen)) {
                QuantityStepper(qty = 2, onChange = {})
                QuantityStepper(qty = 1, onChange = {})
            }
        }
        assertTouchTargets("quantity stepper")
        assertLabels("quantity stepper")
    }

    @Test fun homeScreenIsReachable() {
        show {
            ir.atom313.app.ui.home.HomeContent(
                home = homeSample, wished = emptySet(), contentPadding = PaddingValues(),
                onProduct = {}, onCategory = {}, onSeller = {}, onOffice = {}, onArticle = {},
                onAllProducts = {}, onAllSellers = {}, onAllOffices = {}, onAllArticles = {},
                onToggleWish = {}, onAddToCart = {},
            )
        }
        assertTouchTargets("home")
        assertLabels("home")
    }

    private val homeSample = ir.atom313.app.domain.model.Home(
        slides = emptyList(),
        announcement = null,
        categories = listOf(ir.atom313.app.domain.model.Category("پوشاک", 31)),
        newest = listOf(sample),
        popular = emptyList(),
        sellers = emptyList(),
        offices = emptyList(),
        articles = emptyList(),
    )
}
