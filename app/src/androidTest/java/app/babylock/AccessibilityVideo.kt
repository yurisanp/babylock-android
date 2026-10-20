package app.babylock

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import app.babylock.lock.LockAccessibilityService
import app.babylock.lock.LockController
import app.babylock.settings.Prefs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Roteiro do vídeo exigido pela Play para o uso da acessibilidade ("divulgação em destaque").
 * Roda só no workflow "Vídeo da acessibilidade", que grava a tela com screenrecord enquanto este teste anda.
 * As pausas são de propósito: dão tempo de quem revisa ler cada tela.
 *
 * 1. App aberto, sem a permissão, toque em "Ligar".
 * 2. Aviso de uso da acessibilidade, e o aceite.
 * 3. Configurações do Android: o adulto liga o BabyLock e confirma.
 * 4. De volta ao app, trava com o botão grande e abre o "vídeo".
 * 5. Toques não fazem nada e a barra de notificações fecha sozinha.
 * 6. Segura o canto e destrava com o PIN.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AccessibilityVideo : EndToEndBase() {

    @Before
    fun preparar() {
        // Com as animações ligadas (para o vídeo ficar natural) a tela nunca fica "parada":
        // sem isto cada busca do UiAutomator espera 10 s pelo ocioso e perde a tela de desbloqueio.
        Configurator.getInstance().waitForIdleTimeout = 500
        prepareDevice()
        setOverlayPermission(false)
        device.executeShellCommand("settings delete secure enabled_accessibility_services")
        waitUntil("o serviço começar desligado", timeoutMs = 15_000) { LockAccessibilityService.instance == null }
        Prefs.disclosureAccepted = false
        device.executeShellCommand("settings put system show_touches 1")
        listOf(
            "settings put global sysui_demo_allowed 1",
            "am broadcast -a com.android.systemui.demo -e command enter",
            "am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200",
            "am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false",
            "am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4",
        ).forEach { device.executeShellCommand(it) }
    }

    @After
    fun depois() {
        device.executeShellCommand("settings put system show_touches 0")
        device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command exit")
    }

    @Test
    fun roteiro() {
        // 1. O app ainda sem a permissão.
        openApp()
        pause(3_000)
        byTag("enable_accessibility").click()

        // 2. Divulgação em destaque, antes de qualquer tela do Android.
        check(device.wait(Until.hasObject(By.text(text(R.string.disclosure_title))), 5_000)) { "O aviso não apareceu. ${screenSummary()}" }
        pause(7_000)
        device.findObject(By.text(text(R.string.disclosure_accept))).click()

        // 3. Configurações de acessibilidade do Android.
        check(device.wait(Until.hasObject(By.pkg("com.android.settings").depth(0)), 8_000)) { "As configurações não abriram. ${screenSummary()}" }
        pause(2_000)
        val label = text(R.string.accessibility_service_label)
        findScrolling(By.text(label)).click()
        pause(2_500)
        val toggle = device.wait(Until.findObject(By.checkable(true)), 5_000)
            ?: throw AssertionError("Não achei a chave do BabyLock. ${screenSummary()}")
        toggle.click()
        pause(2_500)
        val allow = device.wait(Until.findObject(By.text(Pattern.compile("(?i)allow|permitir"))), 5_000)
            ?: throw AssertionError("Não achei o botão Permitir. ${screenSummary()}")
        allow.click()
        waitUntil("o serviço ligar", timeoutMs = 15_000) { LockAccessibilityService.instance != null }
        // Alguns Androids mostram uma dica do atalho depois de ligar.
        device.wait(Until.findObject(By.text(Pattern.compile("(?i)got it|entendi|ok"))), 1_500)?.click()
        pause(2_500)

        // 4. De volta ao app: agora com a proteção completa.
        openApp()
        pause(3_000)
        byTag("lock_button").click()
        pause(1_500)
        val video = Intent(context, TouchProbeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(TouchProbeActivity.EXTRA_CARTOON, true)
        context.startActivity(video)
        waitUntil("travar depois da contagem", timeoutMs = 10_000) { LockController.locked.value }
        pause(2_000)

        // 5. A criança toca na tela e puxa a barra: nada muda.
        repeat(6) {
            device.click(device.displayWidth * (2 + it % 3) / 6, device.displayHeight * (3 + it % 2) / 6)
            pause(600)
        }
        pause(1_500)
        device.swipe(device.displayWidth / 2, 5, device.displayWidth / 2, device.displayHeight * 2 / 3, 40)
        pause(3_000)
        device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 3, device.displayHeight / 4, 30)
        pause(2_500)
        check(LockController.locked.value) { "Destravou sozinho durante o vídeo" }

        // 6. O adulto segura o canto e confirma com o PIN.
        // Segura bem mais que o tempo do canto (2 s), para dar para ver o anel enchendo.
        val (x, y) = corner()
        val before = diagnostics()
        device.executeShellCommand("logcat -c")
        device.swipe(x, y, x, y, 900)
        // Só confere que a tela abriu: com animações a árvore muda e referências guardadas ficam velhas.
        if (!device.wait(Until.hasObject(By.res("unlock_answer")), 8_000)) {
            val after = diagnostics()
            // Segunda tentativa, igual aos testes de ponta a ponta, para saber se foi o gesto.
            holdCorner()
            if (!device.wait(Until.hasObject(By.res("unlock_answer")), 8_000)) {
                throw AssertionError(
                    "A tela de desbloqueio não abriu. canto=($x,$y) tela=${device.displayWidth}x${device.displayHeight}\n" +
                        "ANTES: $before\nDEPOIS: $after\nDEPOIS DA 2ª: ${diagnostics()}",
                )
            }
        }
        pause(1_500)
        // Digita no teclado da tela, como o adulto faria: aparece no vídeo.
        pin.forEach { d ->
            clickFresh(By.res("unlock_key_$d"), "a tecla $d")
            pause(450)
        }
        pause(1_000)
        clickFresh(By.res("unlock_confirm").enabled(true), "o Confirmar ativo")
        waitUntil("destravar") { !LockController.locked.value }
        pause(2_000)
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        pause(2_000)
        TouchProbeActivity.current?.finish()
        openApp()
        pause(3_000)
    }

    private fun pause(ms: Long) = SystemClock.sleep(ms)

    /** Acha e toca na hora; se o elemento for redesenhado no meio, procura de novo. */
    private fun clickFresh(selector: androidx.test.uiautomator.BySelector, what: String) {
        repeat(3) {
            val obj = device.wait(Until.findObject(selector), 3_000)
                ?: throw AssertionError("Não achei $what. ${screenSummary()}")
            try {
                obj.click()
                return
            } catch (_: androidx.test.uiautomator.StaleObjectException) {
                pause(200)
            }
        }
        throw AssertionError("$what sumia toda vez. ${screenSummary()}")
    }

    /** Estado do bloqueio e das janelas, para entender uma falha só pelo log do CI. */
    private fun diagnostics(): String {
        val windows = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            .windows.joinToString { "${it.type}:${it.title}:${it.isFocused}" }
        val top = device.executeShellCommand("dumpsys activity activities").lines()
            .filter { "ResumedActivity" in it }.take(2).joinToString(" | ") { it.trim() }
        val keys = listOf("BabyLockHold", "UnlockActivity", "START u0", "Background activity", "AndroidRuntime", "BAL")
        val log = device.executeShellCommand("logcat -d").lines()
            .filter { line -> keys.any { it in line } }
            .takeLast(25).joinToString("\n")
        return "locked=${LockController.locked.value} surface=${LockController.activeSurface?.javaClass?.simpleName} " +
            "service=${LockAccessibilityService.instance != null}\njanelas=[$windows]\ntopo=$top\nlog:\n$log"
    }

    /** Procura um item rolando a lista das configurações, que muda de ordem entre versões do Android. */
    private fun findScrolling(selector: androidx.test.uiautomator.BySelector): UiObject2 {
        device.wait(Until.findObject(selector), 2_000)?.let { return it }
        val list = device.findObject(By.scrollable(true))
            ?: throw AssertionError("A tela não rola. ${screenSummary()}")
        return list.scrollUntil(Direction.DOWN, Until.findObject(selector))
            ?: throw AssertionError("Não achei o BabyLock na lista. ${screenSummary()}")
    }
}
