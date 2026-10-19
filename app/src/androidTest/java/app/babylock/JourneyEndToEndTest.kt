package app.babylock

import android.app.UiAutomation
import android.os.SystemClock
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import app.babylock.lock.LockController
import app.babylock.settings.Prefs
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Jornadas completas como a mãe ou o pai usam o app: abrir pela primeira vez, ligar a permissão,
 * travar pelo botão grande, a tela apagar no meio do vídeo, esquecer a tela de desbloqueio aberta
 * e usar o modo básico (sem acessibilidade).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class JourneyEndToEndTest : EndToEndBase() {

    @Before
    fun preparar() {
        prepareDevice()
        setOverlayPermission(false)
        enableAccessibilityService()
    }

    @Test
    fun primeiraAberturaMostraBoasVindasUmaVezSo() {
        Prefs.onboardingDone = false
        openApp()
        assertThat(device.wait(Until.hasObject(By.text(text(R.string.onboarding_skip))), 5_000)).isTrue()
        repeat(2) { byTag("onboarding_next").click() }
        assertThat(device.wait(Until.hasObject(By.text(text(R.string.onboarding_start))), 3_000)).isTrue()
        byTag("onboarding_next").click()

        byTag("lock_button")
        assertThat(Prefs.onboardingDone).isTrue()

        // Ao abrir de novo, vai direto para o início.
        openApp()
        byTag("lock_button")
        assertThat(device.hasObject(By.res("onboarding_next"))).isFalse()
    }

    @Test
    fun ajudaMostraAsBoasVindasDeNovo() {
        openApp()
        byTag("lock_button")
        val tour = device.findObject(By.res("help_tour"))
            ?: device.findObject(By.scrollable(true))?.scrollUntil(Direction.DOWN, Until.findObject(By.res("help_tour")))
            ?: throw AssertionError("Não achei a ajuda no fim da tela")
        tour.click()
        assertThat(device.wait(Until.hasObject(By.res("onboarding_skip")), 3_000)).isTrue()
        byTag("onboarding_skip").click()
        byTag("lock_button")
    }

    @Test
    fun ligarAcessibilidadeMostraOAvisoEDepoisAbreAsConfiguracoes() {
        disableAccessibilityService()
        Prefs.disclosureAccepted = false
        openApp()
        byTag("enable_accessibility").click()

        // Divulgação exigida pelo Google Play: explica o uso antes de abrir as configurações.
        assertThat(device.wait(Until.hasObject(By.text(text(R.string.disclosure_title))), 3_000)).isTrue()
        assertThat(device.hasObject(By.text(text(R.string.disclosure_next_step)))).isTrue()
        device.findObject(By.text(text(R.string.disclosure_accept))).click()

        assertThat(device.wait(Until.hasObject(By.pkg("com.android.settings").depth(0)), 5_000)).isTrue()
        assertThat(Prefs.disclosureAccepted).isTrue()
        device.pressBack()
    }

    @Test
    fun botaoTravarContaEAtravaPorCimaDoVideo() {
        openApp()
        byTag("lock_button").click()
        assertThat(device.wait(Until.hasObject(By.text(text(R.string.countdown_title))), 3_000)).isTrue()

        // A mãe abre o vídeo durante a contagem.
        openProbe()
        waitUntil("travar sozinho depois da contagem", timeoutMs = 8_000) { LockController.locked.value }
        SystemClock.sleep(500)
        tapCenterTimes(5)
        assertThat(TouchProbeActivity.touches.get()).isEqualTo(0)

        unlockWithPin()
        waitUntil("destravar") { !LockController.locked.value }
        device.wait(Until.gone(By.res("unlock_answer")), 3_000)
        tapCenterTimes(1)
        assertThat(TouchProbeActivity.touches.get()).isGreaterThan(0)
    }

    @Test
    fun cancelarAContagemNaoTrava() {
        openApp()
        byTag("lock_button").click()
        byTag("cancel_countdown").click()
        SystemClock.sleep(6_000)
        assertThat(LockController.locked.value).isFalse()
        assertThat(LockController.countdown.value).isNull()
    }

    @Test
    fun telaApagaEAcendeEContinuaTravado() {
        openProbe()
        lockNow()
        device.sleep()
        SystemClock.sleep(1_500)
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        SystemClock.sleep(1_500)

        assertThat(LockController.locked.value).isTrue()
        val windows = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).windows
        assertWithMessage("A camada de bloqueio sumiu depois de apagar a tela")
            .that(windows.any { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }).isTrue()
        tapCenterTimes(3)
        assertThat(TouchProbeActivity.touches.get()).isEqualTo(0)
    }

    @Test
    fun telaDeDesbloqueioEsquecidaVoltaATravarSozinha() {
        openProbe()
        lockNow()
        holdCorner()
        byTag("unlock_answer")
        // Ninguém confirma: depois de 20 s a tela some e a camada volta.
        assertThat(device.wait(Until.gone(By.res("unlock_answer")), 26_000)).isTrue()
        assertThat(LockController.locked.value).isTrue()
        SystemClock.sleep(500)
        tapCenterTimes(3)
        assertThat(TouchProbeActivity.touches.get()).isEqualTo(0)
    }

    @Test
    fun modoBasicoSemAcessibilidadeTambemTravaEDestrava() {
        disableAccessibilityService()
        setOverlayPermission(true)
        waitUntil("a permissão de sobreposição valer") { LockController.availableEngine(context) == LockController.Engine.OVERLAY }

        openProbe()
        lockNow()
        waitUntil("a camada básica aparecer") { LockController.activeSurface != null }
        SystemClock.sleep(500)
        tapCenterTimes(5)
        assertThat(TouchProbeActivity.touches.get()).isEqualTo(0)

        unlockWithPin()
        waitUntil("destravar") { !LockController.locked.value }
        device.wait(Until.gone(By.res("unlock_answer")), 3_000)
        tapCenterTimes(1)
        waitUntil("o toque chegar depois de destravar") { TouchProbeActivity.touches.get() > 0 }
        setOverlayPermission(false)
    }
}
