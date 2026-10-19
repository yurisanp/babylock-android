package app.babylock.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.babylock.R
import app.babylock.lock.LockAccessibilityService
import app.babylock.lock.LockController
import app.babylock.settings.Prefs
import app.babylock.ui.components.Haptics
import app.babylock.ui.components.LockGlyph
import app.babylock.ui.components.ProgressRing
import app.babylock.ui.components.enterUp
import app.babylock.ui.components.pressScale
import app.babylock.ui.components.rememberLoop
import app.babylock.ui.components.rememberPulse
import app.babylock.ui.theme.Cream
import app.babylock.ui.theme.Graphite
import app.babylock.ui.theme.Lilac
import app.babylock.ui.theme.LilacLight
import app.babylock.ui.theme.Mint
import app.babylock.ui.theme.MintDeep
import app.babylock.ui.theme.MintSoft
import app.babylock.ui.theme.Motion
import app.babylock.ui.theme.NightLine
import app.babylock.ui.theme.NightMuted
import app.babylock.ui.theme.NightSky

private data class VideoApp(val name: String, val pkg: String, val tint: Color, val ink: Color)

private val VIDEO_APPS = listOf(
    VideoApp("YouTube", "com.google.android.youtube", Color(0xFFF3D9D2), Color(0xFFA23A28)),
    VideoApp("YouTube Kids", "com.google.android.apps.youtube.kids", Color(0xFFF8E3C4), Color(0xFF8A5A12)),
    VideoApp("Netflix", "com.netflix.mediaclient", Color(0xFFE3DEF6), Color(0xFF4B3D96)),
    VideoApp("Disney+", "com.disney.disneyplus", Color(0xFFD4E6F1), Color(0xFF1F5578)),
    VideoApp("Prime Video", "com.amazon.avod.thirdpartyclient", Color(0xFFD6EDE5), Color(0xFF1D5E4C)),
)

private const val OPEN_AND_LOCK_SECONDS = 5

@Composable
fun HomeScreen(onShowTour: () -> Unit = {}) {
    val context = LocalContext.current
    val view = LocalView.current
    var accessibilityOn by remember { mutableStateOf(false) }
    var overlayOn by remember { mutableStateOf(false) }
    var showDisclosure by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var lockVolume by remember { mutableStateOf(Prefs.lockVolume) }
    var closeShade by remember { mutableStateOf(Prefs.closeShade) }
    var showCornerHint by remember { mutableStateOf(Prefs.showCornerHint) }
    var accessibilityButton by remember { mutableStateOf(Prefs.accessibilityButton) }
    var shadeEscaped by remember { mutableStateOf(Prefs.shadeEscaped) }
    var holdSeconds by remember { mutableIntStateOf(Prefs.holdSeconds) }
    var hasPin by remember { mutableStateOf(Prefs.hasPin) }
    val countdown by LockController.countdown.collectAsStateWithLifecycle()

    // Reavalia as permissões sempre que o adulto volta das Configurações.
    LifecycleResumeEffect(Unit) {
        accessibilityOn = LockAccessibilityService.instance != null
        overlayOn = Settings.canDrawOverlays(context)
        shadeEscaped = Prefs.shadeEscaped
        onPauseOrDispose { }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun startLock(seconds: Int, app: VideoApp? = null) {
        Haptics.confirm(view)
        if (!accessibilityOn && Build.VERSION.SDK_INT >= 33) {
            // O modo básico mostra uma notificação fixa enquanto trava.
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        app?.let { context.packageManager.getLaunchIntentForPackage(it.pkg) }
            ?.let { context.startActivity(it) }
        LockController.scheduleLock(context, seconds)
    }

    val ready = accessibilityOn || overlayOn

    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.enterUp(0).semantics { heading() },
                )

                HeroCard(
                    accessibilityOn = accessibilityOn,
                    overlayOn = overlayOn,
                    enabled = ready && countdown == null,
                    onLock = { startLock(OPEN_AND_LOCK_SECONDS) },
                    modifier = Modifier.enterUp(1),
                )

                if (shadeEscaped) {
                    ShadeHelpCard(
                        highlight = true,
                        onOpenSettings = { openSettings(context) },
                        onDismiss = { shadeEscaped = false; Prefs.shadeEscaped = false },
                        modifier = Modifier.enterUp(2),
                    )
                }

                if (!accessibilityOn) {
                    PermissionCard(
                        title = stringResource(R.string.permission_accessibility_title),
                        badge = stringResource(R.string.permission_badge),
                        body = stringResource(R.string.permission_accessibility_body),
                        onClick = {
                            if (Prefs.disclosureAccepted) openAccessibilitySettings(context) else showDisclosure = true
                        },
                        modifier = Modifier.enterUp(2).testTag("enable_accessibility"),
                    )
                }
                if (!accessibilityOn && !overlayOn) {
                    PermissionCard(
                        title = stringResource(R.string.permission_overlay_title),
                        badge = null,
                        body = stringResource(R.string.permission_overlay_body),
                        onClick = { openOverlaySettings(context) },
                        modifier = Modifier.enterUp(3).testTag("enable_overlay"),
                    )
                }

                val installed = VIDEO_APPS.filter { context.packageManager.getLaunchIntentForPackage(it.pkg) != null }
                if (installed.isNotEmpty()) {
                    Row(Modifier.enterUp(4), verticalAlignment = Alignment.Bottom) {
                        SectionTitle(stringResource(R.string.open_and_lock_title), Modifier.weight(1f))
                        Text(
                            stringResource(R.string.open_and_lock_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    installed.chunked(2).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEachIndexed { i, app ->
                                AppTile(
                                    app = app,
                                    enabled = ready && countdown == null,
                                    onClick = { startLock(OPEN_AND_LOCK_SECONDS, app) },
                                    modifier = Modifier.weight(1f).enterUp(5 + r * 2 + i),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                UnlockHowTo(holdSeconds, Modifier.enterUp(9))

                SectionTitle(stringResource(R.string.settings_title))
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        SettingSwitch(stringResource(R.string.setting_volume), stringResource(R.string.setting_needs_accessibility), lockVolume) {
                            lockVolume = it; Prefs.lockVolume = it
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingSwitch(stringResource(R.string.setting_shade), stringResource(R.string.setting_needs_accessibility), closeShade) {
                            closeShade = it; Prefs.closeShade = it
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingSwitch(
                            stringResource(R.string.setting_a11y_button),
                            stringResource(R.string.setting_a11y_button_hint),
                            accessibilityButton,
                        ) {
                            accessibilityButton = it; Prefs.accessibilityButton = it
                            LockAccessibilityService.instance?.refreshButton()
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingSwitch(stringResource(R.string.setting_corner_hint), stringResource(R.string.setting_corner_hint_hint), showCornerHint) {
                            showCornerHint = it; Prefs.showCornerHint = it
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(pluralStringResource(R.plurals.setting_hold, holdSeconds, holdSeconds), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.setting_hold_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = holdSeconds.toFloat(),
                                onValueChange = {
                                    if (it.toInt() != holdSeconds) Haptics.tick(view)
                                    holdSeconds = it.toInt(); Prefs.holdSeconds = holdSeconds
                                },
                                valueRange = 2f..6f,
                                steps = 3,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.setting_pin), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(if (hasPin) R.string.setting_pin_on else R.string.setting_pin_off),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { showPinDialog = true }, modifier = Modifier.testTag("pin_button")) {
                                Text(stringResource(if (hasPin) R.string.action_change else R.string.action_set))
                            }
                        }
                    }
                }

                if (!shadeEscaped) {
                    ShadeHelpCard(
                        highlight = false,
                        onOpenSettings = { openSettings(context) },
                        onDismiss = null,
                    )
                }

                DeviceTips.current(context)?.let { tip ->
                    SectionTitle(stringResource(R.string.tips_title, tip.brand))
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tip.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
                SectionTitle(stringResource(R.string.help_title))
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        HelpRow(stringResource(R.string.help_tour), onShowTour, Modifier.testTag("help_tour"))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        HelpRow(stringResource(R.string.help_privacy), { showPrivacy = true }, Modifier.testTag("help_privacy"))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        // Contagem em tela cheia por cima do início, enquanto o adulto abre o vídeo.
        AnimatedVisibility(
            visible = countdown != null,
            enter = fadeIn(tween(Motion.SCRIM_MS)) + scaleIn(tween(Motion.ENTER_MS, easing = Motion.Emphasized), initialScale = 1.04f),
            exit = fadeOut(tween(Motion.SCRIM_MS)),
        ) {
            // Guarda o último número para a saída animar sem piscar.
            var last by remember { mutableIntStateOf(OPEN_AND_LOCK_SECONDS) }
            val current = countdown
            if (current != null) SideEffect { last = current }
            CountdownOverlay(seconds = current ?: last, total = OPEN_AND_LOCK_SECONDS, onCancel = { LockController.cancelScheduled() })
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            icon = { Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.disclosure_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.disclosure_body))
                    Text(
                        stringResource(R.string.disclosure_next_step),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            confirmButton = {
                Button(modifier = Modifier.testTag("disclosure_accept"), onClick = {
                    Prefs.disclosureAccepted = true
                    showDisclosure = false
                    openAccessibilitySettings(context)
                }) { Text(stringResource(R.string.disclosure_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }, modifier = Modifier.testTag("disclosure_dismiss")) {
                    Text(stringResource(R.string.disclosure_dismiss))
                }
            },
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            icon = { Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.privacy_title)) },
            text = { Text(stringResource(R.string.privacy_body), Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }, modifier = Modifier.testTag("privacy_ok")) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }

    if (showPinDialog) {
        PinDialog(
            hasPin = hasPin,
            onDismiss = { showPinDialog = false },
            onSave = { pin -> Prefs.setPin(pin); hasPin = true; showPinDialog = false },
            onRemove = { Prefs.clearPin(); hasPin = false; showPinDialog = false },
        )
    }
}

/** Cartão noturno do topo: estado da proteção e o botão grande de travar, que "respira". */
@Composable
private fun HeroCard(
    accessibilityOn: Boolean,
    overlayOn: Boolean,
    enabled: Boolean,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, sub) = when {
        accessibilityOn -> stringResource(R.string.status_full_title) to stringResource(R.string.status_full_body)
        overlayOn -> stringResource(R.string.status_basic_title) to stringResource(R.string.status_basic_body)
        else -> stringResource(R.string.status_missing_title) to stringResource(R.string.status_missing_body)
    }
    val dot by animateColorAsState(
        when {
            accessibilityOn -> Mint
            overlayOn -> LilacLight
            else -> Color(0xFFF2B76B)
        },
        Motion.firm(),
        label = "status",
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(NightSky)
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            StatusDot(dot, pulsing = accessibilityOn || overlayOn)
            Spacer(Modifier.width(12.dp))
            Column {
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
                    label = "status-title",
                ) { Text(it, style = MaterialTheme.typography.titleMedium, color = MintSoft) }
                Text(sub, style = MaterialTheme.typography.bodySmall, color = NightMuted)
            }
        }

        LockOrb(enabled = enabled, onClick = onLock)

        Text(
            stringResource(R.string.hero_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = NightMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusDot(color: Color, pulsing: Boolean) {
    val ring = if (pulsing) rememberLoop(2400) else 0f
    Box(Modifier.padding(top = 5.dp).size(12.dp), contentAlignment = Alignment.Center) {
        if (pulsing) {
            Box(
                Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = 1f + ring * 1.4f
                        scaleY = 1f + ring * 1.4f
                        alpha = (1f - ring) * 0.6f
                    }
                    .background(color, CircleShape),
            )
        }
        Box(Modifier.size(10.dp).background(color, CircleShape))
    }
}

/** O botão redondo de travar com dois halos lilás respirando atrás dele. */
@Composable
private fun LockOrb(enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val breath = if (enabled) rememberPulse() else 0f
    val breath2 = if (enabled) rememberPulse(delayMs = Motion.BREATH_MS / 3) else 0f
    val face by animateColorAsState(if (enabled) Cream else Color(0xFF3A4060), Motion.firm(), label = "orb")
    val ink by animateColorAsState(if (enabled) NightSky else NightMuted, Motion.firm(), label = "orb-ink")

    Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) {
        if (enabled) {
            listOf(breath, breath2).forEach { b ->
                Box(
                    Modifier
                        .size(196.dp)
                        .graphicsLayer {
                            scaleX = 1f + b * 0.12f
                            scaleY = 1f + b * 0.12f
                            alpha = 0.35f - b * 0.23f
                        }
                        .background(Lilac, CircleShape),
                )
            }
        }
        Column(
            Modifier
                .size(156.dp)
                .pressScale(interaction, pressed = 0.92f)
                .shadow(if (enabled) 18.dp else 0.dp, CircleShape)
                .clip(CircleShape)
                .background(face)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .testTag("lock_button"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LockGlyph(Modifier.size(width = 36.dp, height = 42.dp), body = ink, hole = face)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.lock_button), style = MaterialTheme.typography.titleLarge, color = ink)
            Text(
                pluralStringResource(R.plurals.lock_button_delay, OPEN_AND_LOCK_SECONDS, OPEN_AND_LOCK_SECONDS),
                style = MaterialTheme.typography.bodySmall,
                color = ink.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun PermissionCard(title: String, badge: String?, body: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(interaction, pressed = 0.98f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.large)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1D5E4C),
                        modifier = Modifier
                            .background(Color(0xFFD4F1E7), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.permission_action), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AppTile(app: VideoApp, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .height(64.dp)
            .pressScale(interaction)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(app.tint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, null, tint = app.ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(app.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

/** Mini demonstração do gesto: um dedo segura o canto e o anel enche. */
@Composable
private fun UnlockHowTo(holdSeconds: Int, modifier: Modifier = Modifier) {
    val t = rememberLoop(3400)
    val fill = ((t - 0.08f) / 0.72f).coerceIn(0f, 1f)
    val fade = if (t > 0.85f) 1f - (t - 0.85f) / 0.15f else 1f
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(width = 64.dp, height = 76.dp).clip(RoundedCornerShape(14.dp)).background(Graphite),
            contentAlignment = Alignment.TopEnd,
        ) {
            Box(Modifier.size(34.dp).graphicsLayer { alpha = fade }, contentAlignment = Alignment.Center) {
                ProgressRing(fill, Modifier.size(28.dp), color = Mint, track = Color(0xFF3B4050), strokeWidth = 3.dp)
                Box(Modifier.size(12.dp).background(Cream, CircleShape))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.howto_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            Text(
                pluralStringResource(R.plurals.howto_body, holdSeconds, holdSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.howto_safety),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Tela da contagem: anel que esvazia continuamente e número que "salta" a cada segundo. */
@Composable
private fun CountdownOverlay(seconds: Int, total: Int, onCancel: () -> Unit) {
    val ring = remember { Animatable(1f) }
    LaunchedEffect(seconds) {
        ring.animateTo((seconds - 1).coerceAtLeast(0) / total.toFloat(), tween(1000, easing = LinearEasing))
    }
    val view = LocalView.current
    LaunchedEffect(seconds) { Haptics.tick(view) }

    Column(
        Modifier
            .fillMaxSize()
            .background(NightSky)
            .pointerInput(Unit) { detectTapGestures { } } // nada passa para a tela de baixo
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.countdown_label), style = MaterialTheme.typography.labelMedium, color = Lilac)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.countdown_title), style = MaterialTheme.typography.headlineMedium, color = Cream, textAlign = TextAlign.Center)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(Modifier.size(252.dp), contentAlignment = Alignment.Center) {
                ProgressRing(ring.value, Modifier.size(252.dp), color = Lilac, track = Color(0xFF232B52))
                AnimatedContent(
                    targetState = seconds,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(Motion.bouncy(), initialScale = 1.45f)) togetherWith
                            (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.7f))
                    },
                    label = "countdown",
                ) { n ->
                    Text("$n", style = MaterialTheme.typography.displayLarge, color = Cream)
                }
            }
        }
        Text(
            pluralStringResource(R.plurals.countdown_hint, seconds, seconds),
            style = MaterialTheme.typography.bodyMedium,
            color = NightMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("cancel_countdown"),
            border = BorderStroke(2.dp, NightLine),
        ) { Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge, color = Cream) }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.semantics { heading() })
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // A linha inteira liga e desliga (alvo de toque maior e um só item para o leitor de tela).
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            thumbContent = if (checked) {
                { Icon(Icons.Filled.Check, null, Modifier.size(SwitchDefaults.IconSize), tint = MintDeep) }
            } else {
                null
            },
            colors = SwitchDefaults.colors(checkedTrackColor = Mint, checkedThumbColor = Color.White),
        )
    }
}

@Composable
private fun PinDialog(hasPin: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit, onRemove: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.length in 4..6 && pin == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (hasPin) R.string.pin_title_change else R.string.pin_title_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pin_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    label = { Text(stringResource(R.string.pin_field)) },
                    modifier = Modifier.testTag("pin_new"),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
                    label = { Text(stringResource(R.string.pin_repeat)) },
                    modifier = Modifier.testTag("pin_confirm"),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(pin) }, enabled = valid, modifier = Modifier.testTag("pin_save")) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (hasPin) TextButton(onClick = onRemove) { Text(stringResource(R.string.action_remove)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

@Composable
private fun HelpRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Como barrar a barra de notificações de vez: fixar o app do vídeo.
 * Destacado quando a barra chegou a abrir durante um bloqueio; senão fica fechado, como uma dúvida comum.
 */
@Composable
private fun ShadeHelpCard(
    highlight: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(highlight) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (highlight) Color(0xFFFFF1DC) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (highlight) Color(0xFF4A3110) else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().testTag("shade_help"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand)) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(if (highlight) R.string.shade_help_alert else R.string.shade_help_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleMedium)
            }
            if (expanded) {
                Text(
                    stringResource(R.string.shade_help_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                DeviceTips.pinSteps(LocalContext.current).forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenSettings, modifier = Modifier.testTag("shade_help_settings")) {
                        Text(stringResource(R.string.shade_help_settings))
                    }
                    if (onDismiss != null) {
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("shade_help_dismiss")) {
                            Text(stringResource(R.string.shade_help_dismiss))
                        }
                    }
                }
            }
        }
    }
}

private fun openSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Abre as configurações e deixa um lembrete curto do que fazer lá (o Toast aparece por cima delas). */
private fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    Toast.makeText(context, R.string.guide_accessibility, Toast.LENGTH_LONG).show()
}

private fun openOverlaySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    Toast.makeText(context, R.string.guide_overlay, Toast.LENGTH_LONG).show()
}
