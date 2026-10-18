package app.babylock.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.babylock.ui.theme.Motion

/** Entrada padrão: sobe 16 dp e aparece, com atraso para criar a cascata. */
fun Modifier.enterUp(index: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, Motion.enter(index * Motion.STAGGER_MS)) }
    val shift = with(LocalDensity.current) { 16.dp.toPx() }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * shift
    }
}

/** Encolhe com mola enquanto o dedo está pressionando. */
fun Modifier.pressScale(source: InteractionSource, pressed: Float = 0.95f): Modifier = composed {
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, Motion.bouncy(), label = "press")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Valor de 0 a 1 que vai e volta para sempre (respirar, flutuar, piscar). */
@Composable
fun rememberPulse(periodMs: Int = Motion.BREATH_MS, delayMs: Int = 0): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs / 2, delayMs, Motion.Breath),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    return value
}

/** Valor de 0 a 1 que recomeça do zero a cada volta (ondas, anéis). */
@Composable
fun rememberLoop(periodMs: Int, delayMs: Int = 0): Float {
    val transition = rememberInfiniteTransition(label = "loop")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, delayMs, Motion.Emphasized), RepeatMode.Restart),
        label = "loop",
    )
    return value
}

object Haptics {
    fun tick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun confirm(view: View) {
        val c = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(c)
    }

    fun reject(view: View) {
        val c = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(c)
    }
}
