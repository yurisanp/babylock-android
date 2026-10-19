package app.babylock

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import app.babylock.settings.Prefs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tira as capturas de tela do app de verdade para a ficha da Play.
 * Roda só no job "Capturas para a Play" do CI (Android 14, tela 1080x1920), uma vez por idioma:
 * o script .github/scripts/play_screenshots.sh troca o idioma do emulador e passa o argumento "locale".
 * De quebra é mais um teste de ponta a ponta: cada tela precisa abrir em cada idioma.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PlayScreenshots : EndToEndBase() {

    private val dir = "/data/local/tmp/babylock-shots"
    private val locale: String =
        InstrumentationRegistry.getArguments().getString("locale")
            ?: context.resources.configuration.locales[0].toLanguageTag()

    @Before
    fun preparar() {
        prepareDevice()
        enableAccessibilityService()
        device.executeShellCommand("mkdir -p $dir")
        // Barra de status limpa: 12:00, bateria cheia, sem ícones de notificação.
        listOf(
            "settings put global sysui_demo_allowed 1",
            "am broadcast -a com.android.systemui.demo -e command enter",
            "am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200",
            "am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false",
            "am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile show -e level 4",
            "am broadcast -a com.android.systemui.demo -e command notifications -e visible false",
        ).forEach { device.executeShellCommand(it) }
    }

    @After
    fun depois() {
        device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command exit")
    }

    @Test
    fun capturas() {
        val language = locale.substringBefore('-')
        check(context.resources.configuration.locales[0].language == language) {
            "O aparelho não está em $locale (está ${context.resources.configuration.locales[0]})"
        }

        // 1. Boas-vindas
        Prefs.onboardingDone = false
        openApp()
        byTag("onboarding_next")
        shot("1-boas-vindas")

        // 2. Início com a proteção completa
        Prefs.onboardingDone = true
        openApp()
        byTag("lock_button")
        shot("2-inicio")

        // 3. Contagem para abrir o vídeo
        byTag("lock_button").click()
        byTag("cancel_countdown")
        SystemClock.sleep(300)
        shot("3-contagem")
        byTag("cancel_countdown").click()

        // 4. Confirmação do adulto por cima do "vídeo"
        openProbe()
        lockNow()
        holdCorner()
        byTag("unlock_answer")
        SystemClock.sleep(800)
        shot("4-desbloqueio")
        byTag("unlock_cancel").click()
        device.wait(Until.gone(By.res("unlock_cancel")), 3_000)
    }

    private fun shot(name: String) {
        device.waitForIdle()
        device.executeShellCommand("screencap -p $dir/$locale-$name.png")
    }
}
