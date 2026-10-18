package app.babylock.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.babylock.lock.LockAccessibilityService
import app.babylock.lock.LockController
import app.babylock.settings.Prefs
import app.babylock.ui.theme.Mint

private data class VideoApp(val name: String, val pkg: String)

private val VIDEO_APPS = listOf(
    VideoApp("YouTube", "com.google.android.youtube"),
    VideoApp("YouTube Kids", "com.google.android.apps.youtube.kids"),
    VideoApp("Netflix", "com.netflix.mediaclient"),
    VideoApp("Disney+", "com.disney.disneyplus"),
    VideoApp("Prime Video", "com.amazon.avod.thirdpartyclient"),
)

private const val OPEN_AND_LOCK_SECONDS = 5

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var accessibilityOn by remember { mutableStateOf(false) }
    var overlayOn by remember { mutableStateOf(false) }
    var showDisclosure by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var lockVolume by remember { mutableStateOf(Prefs.lockVolume) }
    var closeShade by remember { mutableStateOf(Prefs.closeShade) }
    var holdSeconds by remember { mutableIntStateOf(Prefs.holdSeconds) }
    var hasPin by remember { mutableStateOf(Prefs.hasPin) }
    val countdown by LockController.countdown.collectAsStateWithLifecycle()

    // Reavalia as permissões sempre que o adulto volta das Configurações.
    LifecycleResumeEffect(Unit) {
        accessibilityOn = LockAccessibilityService.instance != null
        overlayOn = Settings.canDrawOverlays(context)
        onPauseOrDispose { }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun startLock(seconds: Int, app: VideoApp? = null) {
        if (!accessibilityOn && Build.VERSION.SDK_INT >= 33) {
            // O modo básico mostra uma notificação fixa enquanto trava.
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        app?.let { context.packageManager.getLaunchIntentForPackage(it.pkg) }
            ?.let { context.startActivity(it) }
        LockController.scheduleLock(context, seconds)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()

            HealthCard(
                accessibilityOn = accessibilityOn,
                overlayOn = overlayOn,
                onEnableAccessibility = {
                    if (Prefs.disclosureAccepted) openAccessibilitySettings(context) else showDisclosure = true
                },
                onEnableOverlay = { openOverlaySettings(context) },
            )

            val ready = accessibilityOn || overlayOn
            Button(
                onClick = { startLock(OPEN_AND_LOCK_SECONDS) },
                enabled = ready && countdown == null,
                modifier = Modifier.fillMaxWidth().height(64.dp).testTag("lock_button"),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Filled.Lock, null)
                Spacer(Modifier.width(10.dp))
                Text(
                    countdown?.let { "Travando em $it s… abra o vídeo" } ?: "Travar em $OPEN_AND_LOCK_SECONDS segundos",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (countdown != null) {
                TextButton(
                    onClick = { LockController.cancelScheduled() },
                    modifier = Modifier.testTag("cancel_countdown"),
                ) { Text("Cancelar") }
            }

            val installed = VIDEO_APPS.filter { context.packageManager.getLaunchIntentForPackage(it.pkg) != null }
            if (installed.isNotEmpty()) {
                SectionTitle("Abrir e travar")
                installed.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { app ->
                            OutlinedButton(
                                onClick = { startLock(OPEN_AND_LOCK_SECONDS, app) },
                                enabled = ready && countdown == null,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.PlayCircle, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(app.name)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            SectionTitle("Como destravar")
            Text(
                "Segure o canto superior direito da tela com um dedo por $holdSeconds segundos. " +
                    "Depois confirme com digital, rosto, senha do celular ou PIN. " +
                    "Se algo der errado, reiniciar o celular sempre desfaz o bloqueio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("Configurações")
            Card {
                Column(Modifier.padding(vertical = 8.dp)) {
                    SettingSwitch("Travar botões de volume", "Só com a acessibilidade ligada", lockVolume) {
                        lockVolume = it; Prefs.lockVolume = it
                    }
                    HorizontalDivider()
                    SettingSwitch("Fechar a barra de notificações", "Só com a acessibilidade ligada", closeShade) {
                        closeShade = it; Prefs.closeShade = it
                    }
                    HorizontalDivider()
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Segurar o canto por $holdSeconds s")
                        Slider(
                            value = holdSeconds.toFloat(),
                            onValueChange = { holdSeconds = it.toInt(); Prefs.holdSeconds = holdSeconds },
                            valueRange = 2f..6f,
                            steps = 3,
                        )
                    }
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("PIN do BabyLock")
                            Text(
                                if (hasPin) "Definido" else "Sem PIN: usa biometria ou uma conta de multiplicar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { showPinDialog = true }, modifier = Modifier.testTag("pin_button")) {
                            Text(if (hasPin) "Alterar" else "Definir")
                        }
                    }
                }
            }

            DeviceTips.current()?.let { tip ->
                SectionTitle("Dicas para ${tip.brand}")
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tip.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Uso da acessibilidade") },
            text = {
                Text(
                    "O BabyLock usa o serviço de acessibilidade do Android apenas para bloquear toques, gestos e " +
                        "botões de volume enquanto o bloqueio está ativo e para fechar a barra de notificações.\n\n" +
                        "Ele não lê o conteúdo da tela, não coleta dados pessoais e não envia nada para a internet.",
                )
            },
            confirmButton = {
                Button(modifier = Modifier.testTag("disclosure_accept"), onClick = {
                    Prefs.disclosureAccepted = true
                    showDisclosure = false
                    openAccessibilitySettings(context)
                }) { Text("Aceitar e ativar") }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }, modifier = Modifier.testTag("disclosure_dismiss")) {
                    Text("Agora não")
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

@Composable
private fun Header() {
    Column {
        Text("BabyLock", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "O vídeo continua, os dedinhos não atrapalham.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HealthCard(
    accessibilityOn: Boolean,
    overlayOn: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val (title, sub) = when {
                accessibilityOn -> "Proteção completa" to "Toques, barra de notificações e volume bloqueados."
                overlayOn -> "Proteção básica" to "Toques bloqueados. Ligue a acessibilidade para travar também a barra e o volume."
                else -> "Falta uma permissão" to "Ligue a acessibilidade (recomendado) ou a sobreposição de tela."
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (accessibilityOn || overlayOn) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                    null,
                    tint = if (accessibilityOn) Mint else MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!accessibilityOn) {
                Button(
                    onClick = onEnableAccessibility,
                    modifier = Modifier.fillMaxWidth().testTag("enable_accessibility"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("Ligar acessibilidade (recomendado)") }
            }
            if (!accessibilityOn && !overlayOn) {
                OutlinedButton(onClick = onEnableOverlay, modifier = Modifier.fillMaxWidth().testTag("enable_overlay")) {
                    Text("Usar só a sobreposição (básico)")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PinDialog(hasPin: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit, onRemove: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.length in 4..6 && pin == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasPin) "Alterar PIN" else "Definir PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    label = { Text("PIN (4 a 6 dígitos)") },
                    modifier = Modifier.testTag("pin_new"),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
                    label = { Text("Repita o PIN") },
                    modifier = Modifier.testTag("pin_confirm"),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(pin) }, enabled = valid, modifier = Modifier.testTag("pin_save")) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (hasPin) TextButton(onClick = onRemove) { Text("Remover") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun openOverlaySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
