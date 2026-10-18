package app.babylock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta do plano: azul-noite, lilás suave, creme, grafite e verde menta para "protegido".
val NightSky = Color(0xFF121833) // fundo das telas escuras e do cartão principal
val Night = Color(0xFF2E3A6E)
val NightLine = Color(0xFF3A4372) // contornos sobre o NightSky
val NightMuted = Color(0xFF9DA3C4) // texto secundário sobre o NightSky
val Lilac = Color(0xFF9C8CD9)
val LilacLight = Color(0xFFC9BEF5)
val Cream = Color(0xFFF6EFE3)
val Paper = Color(0xFFFFFBF5)
val Sand = Color(0xFFEDE5D8)
val SandLine = Color(0xFFE4DACA)
val Graphite = Color(0xFF1E2128)
val Mint = Color(0xFF5BC8A8)
val MintDeep = Color(0xFF0E2A22) // texto sobre o Mint
val MintSoft = Color(0xFFBDEBDC)
val Danger = Color(0xFFB3261E)

private val Light = lightColorScheme(
    primary = Night,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1F7),
    onPrimaryContainer = Night,
    secondary = Lilac,
    onSecondary = Color.White,
    tertiary = Mint,
    onTertiary = MintDeep,
    background = Cream,
    onBackground = Graphite,
    surface = Paper,
    onSurface = Graphite,
    surfaceVariant = Sand,
    onSurfaceVariant = Color(0xFF555866),
    outline = Color(0xFFC9C0B0),
    outlineVariant = SandLine,
    error = Danger,
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB9C3F5),
    onPrimary = Color(0xFF17204A),
    primaryContainer = Night,
    onPrimaryContainer = Color(0xFFDDE1F7),
    secondary = Lilac,
    tertiary = Mint,
    onTertiary = MintDeep,
    background = Color(0xFF0D1126),
    onBackground = Color(0xFFECE6DC),
    surface = Color(0xFF1A2040),
    onSurface = Color(0xFFECE6DC),
    surfaceVariant = Color(0xFF232A4D),
    onSurfaceVariant = Color(0xFFB8BBC6),
    outline = Color(0xFF4A5384),
    outlineVariant = Color(0xFF2C3359),
    error = Color(0xFFF2B8B5),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun BabyLockTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
