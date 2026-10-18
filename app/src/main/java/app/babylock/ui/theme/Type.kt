package app.babylock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.babylock.R

/** Títulos e números grandes. Licença OFL (ver licenses/). */
val Display = FontFamily(
    Font(R.font.bricolage_medium, FontWeight.Medium),
    Font(R.font.bricolage_bold, FontWeight.Bold),
)

/** Texto, botões e rótulos. Licença OFL (ver licenses/). */
val Body = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

private fun display(size: Int, line: Int, tracking: Double) = TextStyle(
    fontFamily = Display,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Body,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
)

val AppTypography = Typography(
    displayLarge = display(112, 112, -2.0),
    displayMedium = display(56, 60, -1.0),
    displaySmall = display(40, 44, -0.8),
    headlineLarge = display(34, 38, -0.6),
    headlineMedium = display(30, 34, -0.5),
    headlineSmall = display(24, 30, -0.3),
    titleLarge = display(20, 26, -0.2),
    titleMedium = body(17, 24, FontWeight.Bold),
    titleSmall = body(15, 20, FontWeight.Bold),
    bodyLarge = body(17, 26),
    bodyMedium = body(15, 22),
    bodySmall = body(13, 18),
    labelLarge = body(16, 20, FontWeight.Bold),
    labelMedium = body(14, 18, FontWeight.SemiBold),
    labelSmall = body(12, 16, FontWeight.Bold),
)
