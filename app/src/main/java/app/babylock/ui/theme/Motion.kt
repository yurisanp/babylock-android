package app.babylock.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Tokens de movimento do guia de design (artboard "Guia de movimento").
 * Toda animação do app sai daqui para o ritmo ficar igual em todas as telas.
 */
object Motion {
    /** Desaceleração enfática: conteúdo entrando. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Curva da folha de desbloqueio subindo. */
    val Sheet = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /** Vai e volta suave, para loops (respiração, flutuar). */
    val Breath = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

    const val ENTER_MS = 520
    const val STAGGER_MS = 40
    const val SHEET_MS = 560
    const val SCRIM_MS = 320
    const val BREATH_MS = 3600

    /** Mola com quique: botões, check, cadeado. */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 400f)

    /** Mola firme, sem quique: trocas de cor e tamanho. */
    fun <T> firm(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> enter(delayMs: Int = 0): FiniteAnimationSpec<T> = tween(ENTER_MS, delayMs, Emphasized)
}
