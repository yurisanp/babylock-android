package app.babylock.unlock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.babylock.ui.components.FingerprintGlyph
import app.babylock.ui.components.Haptics
import app.babylock.ui.components.pressScale
import app.babylock.ui.theme.Cream
import app.babylock.ui.theme.Lilac
import app.babylock.ui.theme.Motion
import app.babylock.ui.theme.Night
import app.babylock.ui.theme.NightSky
import kotlinx.coroutines.awaitCancellation

/** Mesmo valor de UnlockActivity.TIMEOUT_MS: a barra mostra quanto falta para travar de novo. */
private const val TIMEOUT_MS = 20_000

/**
 * Folha de confirmação do adulto, que sobe por cima do vídeo.
 * A resposta é digitada num teclado próprio (o teclado do sistema não aparece),
 * mas o campo continua sendo um campo de texto de verdade para leitores de tela e testes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UnlockScreen(
    canUseSystemAuth: Boolean,
    hasPin: Boolean,
    challenge: Pair<Int, Int>,
    error: String?,
    onSystemAuth: () -> Unit,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    var attempts by remember { mutableIntStateOf(0) }
    val view = LocalView.current
    val showAnswer = hasPin || !canUseSystemAuth

    val sheetState = remember { MutableTransitionState(false).apply { targetState = true } }
    val scrim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scrim.animateTo(0.45f, tween(Motion.SCRIM_MS)) }

    // Tremida quando a resposta não confere.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(attempts) {
        if (attempts > 0 && error != null) {
            Haptics.reject(view)
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = 420
                    -9f at 60
                    9f at 120
                    -9f at 180
                    9f at 240
                    -5f at 300
                    3f at 360
                },
            )
        }
    }
    val shakePx = with(LocalDensity.current) { 1.dp.toPx() }

    fun type(d: String) {
        Haptics.tick(view)
        answer = (answer + d).take(6)
    }

    fun submit() {
        onSubmit(answer)
        answer = ""
        attempts++
    }

    Box(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .background(NightSky.copy(alpha = scrim.value)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visibleState = sheetState,
            enter = slideInVertically(tween(Motion.SHEET_MS, easing = Motion.Sheet)) { it } + fadeIn(tween(Motion.SCRIM_MS)),
        ) {
            Column(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .semantics { paneTitle = "Confirmar que é um adulto" }
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(width = 40.dp, height = 5.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                TimeoutBar()
                Text("Confirme que é você", style = MaterialTheme.typography.headlineSmall)

                if (canUseSystemAuth && !showAnswer) {
                    BigFingerprint(onSystemAuth)
                    Text(
                        "Toque para usar a digital, o rosto ou a senha do celular",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (canUseSystemAuth && showAnswer) {
                    BiometricPill(onSystemAuth)
                }

                if (showAnswer) {
                    Text(
                        if (hasPin) "PIN do BabyLock" else "Quanto é ${challenge.first} × ${challenge.second}?",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
                        BasicTextField(
                            value = answer,
                            onValueChange = { v -> answer = v.filter(Char::isDigit).take(6) },
                            singleLine = true,
                            visualTransformation = if (hasPin) PasswordVisualTransformation() else VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .graphicsLayer { translationX = shake.value * shakePx }
                                .testTag("unlock_answer"),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.Center) {
                                    // O campo real fica invisível; o desenho abaixo é o que aparece.
                                    Box(Modifier.size(1.dp).graphicsLayer { alpha = 0f }) { inner() }
                                    if (hasPin) PinDots(answer.length) else ChallengeAnswer(answer)
                                }
                            },
                        )
                    }
                    AnimatedVisibility(
                        visible = error != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Text(
                            error.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Keypad(
                        canConfirm = answer.isNotEmpty(),
                        onDigit = ::type,
                        onBackspace = {
                            Haptics.tick(view)
                            answer = answer.dropLast(1)
                        },
                        onConfirm = ::submit,
                    )
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("unlock_cancel"),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text("Continuar bloqueado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Barra fina que esvazia em 20 s: o tempo até travar de novo sozinho. */
@Composable
private fun TimeoutBar() {
    val t = rememberInfiniteTransition(label = "timeout")
    val left by t.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(TIMEOUT_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "timeout",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(left)
                .height(4.dp)
                .background(Lilac, CircleShape),
        )
    }
}

@Composable
private fun BigFingerprint(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val t = rememberInfiniteTransition(label = "fp")
    val r1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = Motion.Emphasized)), label = "r1")
    val r2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, 1100, Motion.Emphasized)), label = "r2")
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        listOf(r1, r2).forEach { r ->
            Box(
                Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        scaleX = 1f + r * 0.6f
                        scaleY = 1f + r * 0.6f
                        alpha = 0.45f * (1f - r)
                    }
                    .background(Lilac, CircleShape),
            )
        }
        Box(
            Modifier
                .size(112.dp)
                .pressScale(interaction, pressed = 0.92f)
                .clip(CircleShape)
                .background(Night)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "Usar digital ou rosto" }
                .testTag("unlock_biometric"),
            contentAlignment = Alignment.Center,
        ) { FingerprintGlyph(Cream) }
    }
}

@Composable
private fun BiometricPill(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .height(52.dp)
            .pressScale(interaction)
            .clip(CircleShape)
            .background(Night)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp)
            .testTag("unlock_biometric"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FingerprintGlyph(Cream, Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text("Usar digital ou rosto", style = MaterialTheme.typography.labelLarge, color = Cream)
    }
}

/** Bolinhas do PIN: cada dígito "salta" ao entrar. */
@Composable
private fun PinDots(filled: Int) {
    val slots = maxOf(4, filled)
    Row(
        Modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(slots) { i ->
            val on = i < filled
            val scale by animateFloatAsState(if (on) 1f else 0.75f, Motion.bouncy(), label = "dot")
            val empty = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            val color by animateColorAsState(if (on) Night else empty, tween(120), label = "dot-color")
            Box(
                Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
private fun ChallengeAnswer(answer: String) {
    Text(
        answer.ifEmpty { "?" },
        style = MaterialTheme.typography.displaySmall,
        color = if (answer.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun Keypad(canConfirm: Boolean, onDigit: (String) -> Unit, onBackspace: () -> Unit, onConfirm: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { d -> Key(label = d, tag = "unlock_key_$d", onClick = { onDigit(d) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Key(tag = "unlock_backspace", description = "Apagar", onClick = onBackspace) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Key(label = "0", tag = "unlock_key_0", onClick = { onDigit("0") })
            val bg by animateColorAsState(
                if (canConfirm) Night else MaterialTheme.colorScheme.surfaceVariant,
                Motion.firm(),
                label = "ok",
            )
            Key(
                tag = "unlock_confirm",
                description = "Confirmar",
                enabled = canConfirm,
                background = bg,
                onClick = onConfirm,
            ) {
                Icon(Icons.Filled.Check, null, tint = if (canConfirm) Cream else MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun Key(
    tag: String,
    onClick: () -> Unit,
    label: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    background: Color = MaterialTheme.colorScheme.surfaceVariant,
    icon: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(68.dp)
            .pressScale(interaction, pressed = 0.9f)
            .clip(CircleShape)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
        }
        icon?.invoke()
    }
}
