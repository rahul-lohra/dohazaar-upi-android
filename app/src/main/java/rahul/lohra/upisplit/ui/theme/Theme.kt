package rahul.lohra.upisplit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreenDark,
    onPrimary = OnBrandGreenDark,
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = OnGreenContainerDark,
    secondary = BrandGreenDark,
    onSecondary = OnBrandGreenDark,
    secondaryContainer = GreenContainerDark,
    onSecondaryContainer = OnGreenContainerDark,
    background = AppBackgroundDark,
    onBackground = AppOnSurfaceDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    outline = AppOutlineDark,
    error = AppErrorDark,
    onError = AppErrorContainerDark,
    errorContainer = AppErrorContainerDark,
    onErrorContainer = AppErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = OnBrandGreen,
    primaryContainer = GreenContainer,
    onPrimaryContainer = OnGreenContainer,
    secondary = BrandGreen,
    onSecondary = OnBrandGreen,
    secondaryContainer = GreenContainer,
    onSecondaryContainer = OnGreenContainer,
    background = AppBackground,
    onBackground = AppOnSurface,
    surface = AppSurface,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppOnSurfaceVariant,
    outline = AppOutline,
    error = AppError,
    onError = OnBrandGreen,
    errorContainer = AppErrorContainer,
    onErrorContainer = AppError
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun UPISplitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSplitUpiSpacing provides SplitUpiSpacing(),
        LocalSplitUpiDimensions provides SplitUpiDimensions()
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
