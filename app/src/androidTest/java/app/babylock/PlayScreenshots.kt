package app.babylock

import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import app.babylock.settings.Prefs
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tira as capturas de tela do app de verdade, em português, inglês e espanhol, para a ficha da Play.
 * Roda só no job "Capturas para a Play" do CI (Android 14, tela 1080x1920) e as imagens viram artefato.
 * De quebra é mais um teste de ponta a ponta: cada tela precisa abrir em cada idioma.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PlayScreenshots : EndToEndBase() {

    private val dir = "/data/local/tmp/babylock-shots"

    @Before
    fun preparar() {
        // Idioma por app (cmd locale) só existe a partir do Android 13.
        assumeTrue(Build.VERSION.SDK_INT >= 33)
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
        if (Build.VERSION.SDK_INT >= 33) {
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command exit")
            device.executeShellCommand("cmd locale set-app-locales ${context.packageName} --locales en-US")
        }
    }

    @Test
    fun capturasEmTresIdiomas() {
        listOf("pt-BR", "en-US", "es-419").forEach { locale ->
            device.executeShellCommand("cmd locale set-app-locales ${context.packageName} --locales $locale")
            SystemClock.sleep(1_000)
            shootLocale(locale)
        }
    }

    private fun shootLocale(locale: String) {
        // 1. Boas-vindas. A troca de idioma recria a tela logo depois de abrir:
        // espera um pouco para a captura não pegar a tela antiga.
        Prefs.onboardingDone = false
        openApp()
        SystemClock.sleep(2_500)
        byTag("onboarding_next")
        shot(locale, "1-boas-vindas")

        // 2. Início com a proteção completa
        Prefs.onboardingDone = true
        openApp()
        byTag("lock_button")
        SystemClock.sleep(800)
        shot(locale, "2-inicio")

        // 3. Contagem para abrir o vídeo
        byTag("lock_button").click()
        byTag("cancel_countdown")
        SystemClock.sleep(300)
        shot(locale, "3-contagem")
        byTag("cancel_countdown").click()

        // 4. Confirmação do adulto por cima do "vídeo"
        openProbe()
        lockNow()
        holdCorner()
        byTag("unlock_answer")
        SystemClock.sleep(800)
        shot(locale, "4-desbloqueio")
        byTag("unlock_cancel").click()
        device.wait(Until.gone(By.res("unlock_cancel")), 3_000)
    }

    private fun shot(locale: String, name: String) {
        device.waitForIdle()
        device.executeShellCommand("screencap -p $dir/$locale-$name.png")
    }
}
