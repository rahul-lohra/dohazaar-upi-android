package rahul.lohra.upisplit.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class SplitUpiSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val section: Dp = 32.dp,
    val screenHorizontal: Dp = 20.dp,
    val screenVertical: Dp = 20.dp
)

@Immutable
data class SplitUpiDimensions(
    val minimumTouchTarget: Dp = 48.dp,
    val actionHeight: Dp = 52.dp,
    val iconSmall: Dp = 18.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 48.dp,
    val appMarkSize: Dp = 56.dp,
    val scannerSize: Dp = 292.dp,
    val resultIconSize: Dp = 72.dp,
    val progressHeight: Dp = 8.dp,
    val borderWidth: Dp = 1.dp,
    val selectedBorderWidth: Dp = 2.dp
)

internal val LocalSplitUpiSpacing = staticCompositionLocalOf { SplitUpiSpacing() }
internal val LocalSplitUpiDimensions = staticCompositionLocalOf { SplitUpiDimensions() }

val MaterialTheme.spacing: SplitUpiSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSplitUpiSpacing.current

val MaterialTheme.dimensions: SplitUpiDimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalSplitUpiDimensions.current
