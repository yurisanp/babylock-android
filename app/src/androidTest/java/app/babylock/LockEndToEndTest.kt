package app.babylock

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import app.babylock.lock.LockAccessibilityService
import app.babylock.lock.LockController
import app.babylock.settings.Prefs
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de ponta a ponta no emulador: liga o serviço de acessibilidade de verdade,
 * abre uma tela que conta toques (no lugar do YouTube) e confere o bloqueio.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class LockEndToEndTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    // Sem esta flag, o UiAutomator desliga os outros serviços de acessibilidade
    // (inclusive o do BabyLock) assim que se conecta.
    private val device: UiDevice = run {
        Configurator.getInstance().setUiAutomationFlags(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        UiDevice.getInstance(instrumentation)
    }
    private val pin = "2468"

    @Before
    fun preparar() {
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        Prefs.init(context)
        Prefs.setPin(pin)
        Prefs.holdSeconds = 2
        Prefs.lockVolume = true
        Prefs.closeShade = true
        enableAccessibilityService()
        openProbe()
    }

    @After
    fun limpar() {
        Prefs.shadeEscaped = false
        Prefs.accessibilityButton = false
        LockAccessibilityService.instance?.let { s -> instrumentation.runOnMainSync { s.refreshButton() } }
        instrumentation.runOnMainSync { LockController.unlock(context) }
        Prefs.clearPin()
        TouchProbeActivity.current?.finish()
    }

    @Test
    fun toquesNaoChegamAoAppDeVideoEnquantoTravado() {
        lock()
        repeat(5) { device.click(device.displayWidth / 2, device.displayHeight / 2) }
        SystemClock.sleep(500)
        assertThat(TouchProbeActivity.touches.get()).isEqualTo(0)

        instrumentation.runOnMainSync { LockController.unlock(context) }
        device.waitForIdle()
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        waitUntil("o toque chegar depois de destravar") { TouchProbeActivity.touches.get() > 0 }
    }

    @Test
    fun botoesDeVolumeFicamTravados() {
        // Teclas injetadas por software (UiAutomator ou `input keyevent`) não passam pelo filtro
        // de teclas da acessibilidade; só botões físicos passam. Por isso o teste confere
        // a configuração que o sistema aplicou ao serviço e a resposta dele a cada tecla.
        val service = LockAccessibilityService.instance!!
        val info = service.serviceInfo
        assertThat(info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS).isNotEqualTo(0)
        assertThat(info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS)
            .isNotEqualTo(0)

        val keys = listOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE)
        fun consumed(code: Int): Boolean {
            var result = false
            instrumentation.runOnMainSync { result = service.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code)) }
            return result
        }

        // Sem bloqueio as teclas seguem normalmente.
        keys.forEach { assertThat(consumed(it)).isFalse() }
        lock()
        keys.forEach { assertThat(consumed(it)).isTrue() }
        // Outras teclas nunca são seguradas.
        assertThat(consumed(KeyEvent.KEYCODE_BACK)).isFalse()
    }

    @Test
    fun botaoDeAcessibilidadeTravaEIgnoraToqueEnquantoTravado() {
        val service = LockAccessibilityService.instance!!
        val flag = AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
        fun buttonOn() = (service.serviceInfo.flags and flag) != 0

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Até o Android 10 o app mostra e esconde o botão. Desligado por padrão.
            Prefs.accessibilityButton = false
            instrumentation.runOnMainSync { service.refreshButton() }
            assertThat(buttonOn()).isFalse()
        }
        // Do Android 11 em diante quem põe o botão na tela é o adulto, nas Configurações.
        Prefs.accessibilityButton = true
        instrumentation.runOnMainSync { service.refreshButton() }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) assertThat(buttonOn()).isTrue()

        // Tocar no botão trava.
        instrumentation.runOnMainSync { service.onButtonClicked() }
        assertThat(LockController.locked.value).isTrue()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) assertThat(buttonOn()).isFalse()

        // Travado, tocar de novo no botão não muda nada e a tela continua bloqueada.
        instrumentation.runOnMainSync { service.onButtonClicked() }
        assertThat(LockController.locked.value).isTrue()
        repeat(3) { device.click(device.displayWidth / 2, device.displayHeight / 2) }
        SystemClock.sleep(500)
        assertThat(TouchProbeActivity.touches.get()).isEqualTo(0)

        instrumentation.runOnMainSync { LockController.unlock(context) }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) assertThat(buttonOn()).isTrue()
    }

    @Test
    fun barraDeNotificacoesFechaSozinha() {
        lock()
        waitUntil("a tela de vídeo ter o foco") { TouchProbeActivity.focused }
        device.openNotification()
        // A camada de bloqueio fica sempre por cima da barra: mesmo aberta, a criança não mexe nela.
        SystemClock.sleep(500)
        val top = automation().windows.firstOrNull()
        assertWithMessage(windowsSummary()).that(top?.type)
            .isEqualTo(AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY)

        // O serviço deve fechar a barra (pedido oficial e, se o sistema recusar, "voltar")
        // e devolver o foco para o vídeo.
        waitUntil("a barra de notificações fechar", timeoutMs = 10_000, details = ::windowsSummary) {
            SystemClock.sleep(300)
            TouchProbeActivity.focused
        }
        // O vídeo continua lá: o "voltar" do plano B foi para a barra, não para o app.
        assertThat(TouchProbeActivity.current).isNotNull()
    }

    @Test
    fun gestoDoCantoMaisPinDestrava() {
        lock()
        holdCorner()
        val answer = device.wait(Until.findObject(By.res("unlock_answer")), 5_000)
        assertThat(answer).isNotNull()
        answer.text = pin
        confirmButton().click()
        waitUntil("destravar") { !LockController.locked.value }

        // Depois de destravar, o toque volta a chegar no vídeo.
        device.wait(Until.gone(By.res("unlock_answer")), 3_000)
        val before = TouchProbeActivity.touches.get()
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        waitUntil("o toque chegar") { TouchProbeActivity.touches.get() > before }
    }

    @Test
    fun cancelarODesbloqueioVoltaATravar() {
        lock()
        holdCorner()
        device.wait(Until.findObject(By.res("unlock_cancel")), 5_000).click()
        device.wait(Until.gone(By.res("unlock_cancel")), 3_000)
        assertThat(LockController.locked.value).isTrue()

        val before = TouchProbeActivity.touches.get()
        repeat(3) { device.click(device.displayWidth / 2, device.displayHeight / 2) }
        SystemClock.sleep(500)
        assertThat(TouchProbeActivity.touches.get()).isEqualTo(before)
    }

    @Test
    fun pinErradoNaoDestrava() {
        lock()
        holdCorner()
        val answer = device.wait(Until.findObject(By.res("unlock_answer")), 5_000)
        answer.text = "0000"
        confirmButton().click()
        assertThat(device.wait(Until.hasObject(By.text(context.getString(R.string.unlock_wrong))), 3_000)).isTrue()
        assertThat(LockController.locked.value).isTrue()
    }

    @Test
    fun tocarRapidoNoCantoNaoDestrava() {
        lock()
        val (x, y) = corner()
        repeat(10) { device.click(x, y) }
        SystemClock.sleep(3_000)
        assertThat(device.hasObject(By.res("unlock_answer"))).isFalse()
        assertThat(LockController.locked.value).isTrue()
    }

    // --- ajudantes ---

    private fun enableAccessibilityService() {
        val component = "${context.packageName}/${LockAccessibilityService::class.java.name}"
        device.executeShellCommand("settings put secure enabled_accessibility_services $component")
        device.executeShellCommand("settings put secure accessibility_enabled 1")
        waitUntil("o serviço de acessibilidade ligar", timeoutMs = 15_000) { LockAccessibilityService.instance != null }
    }

    private fun openProbe() {
        TouchProbeActivity.touches.set(0)
        val intent = Intent(context, TouchProbeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        waitUntil("a tela de teste abrir") { TouchProbeActivity.current != null }
        device.waitForIdle()
        TouchProbeActivity.touches.set(0)
    }

    private fun lock() {
        var ok = false
        instrumentation.runOnMainSync { ok = LockController.lockNow(context) }
        assertThat(ok).isTrue()
        assertThat(LockController.availableEngine(context)).isEqualTo(LockController.Engine.ACCESSIBILITY)
        device.waitForIdle()
        SystemClock.sleep(500)
    }

    /** O botão só fica ativo depois que o campo recebe o texto; clicar antes não faz nada. */
    private fun confirmButton() =
        device.wait(Until.findObject(By.res("unlock_confirm").enabled(true)), 3_000)
            ?: throw AssertionError("O botão Confirmar não ficou ativo")

    private fun corner(): Pair<Int, Int> = device.displayWidth - 30 to 30

    /** Segura o canto superior direito com um dedo por mais que [Prefs.holdSeconds]. */
    private fun holdCorner() {
        val (x, y) = corner()
        // Cada passo do swipe leva ~5 ms: 600 passos ≈ 3 s.
        device.swipe(x, y, x, y, 600)
    }

    private fun waitUntil(
        what: String,
        timeoutMs: Long = 5_000,
        details: (() -> String)? = null,
        condition: () -> Boolean,
    ) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        val extra = details?.let { " (${runCatching(it).getOrElse { e -> e.toString() }})" }.orEmpty()
        throw AssertionError("Tempo esgotado esperando $what$extra")
    }

    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

    /** Janelas na tela, para entender uma falha sem precisar do logcat. */
    private fun windowsSummary(): String =
        "travado=${LockController.locked.value}, fechamentos=${LockAccessibilityService.dismissCalls}, " +
            "resultado=${LockAccessibilityService.lastDismissResult}, " +
            "eventosSystemUi=${LockAccessibilityService.systemUiEvents}, " +
            "barraAberta=${LockAccessibilityService.shadeOpenings}, voltar=${LockAccessibilityService.backPresses}, janelas=" +
            automation().windows.joinToString { w ->
                "${w.title}[tipo=${w.type}, foco=${w.isFocused}, ativa=${w.isActive}]"
            }
}
