package app.babylock.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.babylock.R
import app.babylock.ui.components.Haptics
import app.babylock.ui.components.LockGlyph
import app.babylock.ui.components.pressScale
import app.babylock.ui.components.rememberLoop
import app.babylock.ui.components.rememberPulse
import app.babylock.ui.theme.Cream
import app.babylock.ui.theme.Lilac
import app.babylock.ui.theme.Mint
import app.babylock.ui.theme.MintDeep
import app.babylock.ui.theme.Motion
import app.babylock.ui.theme.Night
import app.babylock.ui.theme.NightSky

private data class Page(@StringRes val title: Int, @StringRes val body: Int)

private val PAGES = listOf(
    Page(R.string.onboarding_1_title, R.string.onboarding_1_body),
    Page(R.string.onboarding_2_title, R.string.onboarding_2_body),
    Page(R.string.onboarding_3_title, R.string.onboarding_3_body),
)

/** Três páginas de boas-vindas, mostradas só na primeira vez que o app abre. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val view = LocalView.current
    val last = page == PAGES.lastIndex

    Box(Modifier.fillMaxSize().background(NightSky)) {
        Stars()
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = Cream, modifier = Modifier.weight(1f))
                TextButton(onClick = onDone, modifier = Modifier.testTag("onboarding_skip")) {
                    Text(stringResource(R.string.onboarding_skip), style = MaterialTheme.typography.labelLarge, color = Lilac)
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Emblem() }

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(Motion.ENTER_MS, easing = Motion.Emphasized)) { it / 5 * dir } + fadeIn(tween(Motion.ENTER_MS))) togetherWith
                        (slideOutHorizontally(tween(240)) { -it / 6 * dir } + fadeOut(tween(200)))
                },
                label = "page",
                modifier = Modifier.heightIn(min = 200.dp),
            ) { i ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(PAGES[i].title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Cream,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(stringResource(PAGES[i].body), style = MaterialTheme.typography.bodyLarge, color = Color(0xFFC3C6DA))
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PAGES.indices.forEach { i ->
                        val w by animateDpAsState(if (i == page) 28.dp else 8.dp, Motion.bouncy(), label = "dot")
                        val c by animateColorAsState(if (i == page) Cream else Color(0xFF4A5384), tween(300), label = "dot-c")
                        Box(Modifier.height(8.dp).width(w).background(c, CircleShape))
                    }
                }
                val interaction = remember { MutableInteractionSource() }
                val bg by animateColorAsState(if (last) Mint else Cream, Motion.firm(), label = "cta")
                Button(
                    onClick = {
                        Haptics.tick(view)
                        if (last) onDone() else page++
                    },
                    interactionSource = interaction,
                    modifier = Modifier.height(56.dp).pressScale(interaction).testTag("onboarding_next"),
                    colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = if (last) MintDeep else NightSky),
                ) {
                    AnimatedContent(targetState = last, label = "cta-label") { l ->
                        Text(stringResource(if (l) R.string.onboarding_start else R.string.onboarding_next), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/** Cadeado com lua flutuando, halo respirando e uma órbita girando devagar. */
@Composable
private fun Emblem() {
    val float = rememberPulse(6000)
    val halo = rememberPulse(4000)
    val orbit = rememberLoop(18000)
    Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(240.dp)
                .graphicsLayer {
                    scaleX = 0.92f + halo * 0.14f
                    scaleY = 0.92f + halo * 0.14f
                    alpha = 0.55f + halo * 0.35f
                }
                .blur(40.dp)
                .background(Night, CircleShape),
        )
        Canvas(Modifier.size(260.dp)) {
            rotate(orbit * 360f) {
                val r = size.minDimension / 2f - 12.dp.toPx()
                drawCircle(
                    Lilac.copy(alpha = 0.35f),
                    radius = r,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 10.dp.toPx())),
                    ),
                )
                drawCircle(Mint, radius = 5.dp.toPx(), center = Offset(center.x, center.y - r))
            }
        }
        LockGlyph(
            Modifier
                .size(width = 128.dp, height = 146.dp)
                .graphicsLayer { translationY = -10.dp.toPx() * float },
            body = Lilac,
            shackle = Cream,
            hole = NightSky,
            moonHole = true,
        )
    }
}

/** Estrelinhas piscando em tempos diferentes. */
@Composable
private fun Stars() {
    val spots = listOf(
        0.10f to 0.11f, 0.82f to 0.08f, 0.24f to 0.25f, 0.77f to 0.30f,
        0.90f to 0.19f, 0.06f to 0.39f, 0.54f to 0.06f, 0.38f to 0.14f,
    )
    val phases = spots.indices.map { i -> rememberPulse(3200, delayMs = (i * 410) % 3200) }
    Canvas(Modifier.fillMaxSize()) {
        spots.forEachIndexed { i, (x, y) ->
            val p = phases[i]
            drawCircle(
                Cream.copy(alpha = 0.15f + 0.75f * p),
                radius = (1.1f + 0.5f * p).dp.toPx(),
                center = Offset(size.width * x, size.height * y),
            )
        }
    }
}
