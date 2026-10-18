package app.babylock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Paleta do plano: azul-noite, lilás suave, creme, grafite e verde menta para "protegido".
val Night = Color(0xFF2E3A6E)
val Lilac = Color(0xFF9C8CD9)
val Cream = Color(0xFFF6EFE3)
val Graphite = Color(0xFF1E2128)
val Mint = Color(0xFF5BC8A8)

private val Light = lightColorScheme(
    primary = Night,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1F7),
    onPrimaryContainer = Night,
    secondary = Lilac,
    onSecondary = Color.White,
    tertiary = Mint,
    background = Cream,
    onBackground = Graphite,
    surface = Color(0xFFFFFBF5),
    onSurface = Graphite,
    surfaceVariant = Color(0xFFEDE5D8),
    onSurfaceVariant = Color(0xFF555866),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB9C3F5),
    onPrimary = Color(0xFF17204A),
    primaryContainer = Night,
    onPrimaryContainer = Color(0xFFDDE1F7),
    secondary = Lilac,
    tertiary = Mint,
    background = Graphite,
    onBackground = Color(0xFFECE6DC),
    surface = Color(0xFF262A33),
    onSurface = Color(0xFFECE6DC),
    surfaceVariant = Color(0xFF30343F),
    onSurfaceVariant = Color(0xFFB8BBC6),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun BabyLockTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = AppShapes,
        content = content,
    )
}
