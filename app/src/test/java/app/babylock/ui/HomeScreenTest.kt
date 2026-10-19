package app.babylock.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.lock.LockController
import app.babylock.settings.Prefs
import app.babylock.ui.theme.BabyLockTheme
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun limpar() {
        context.getSharedPreferences("babylock", Context.MODE_PRIVATE).edit().clear().commit()
        Prefs.init(context)
        // Como no emulador do CI: sem os loops decorativos (halo, pulsos), que nunca deixam a tela "parada"
        // e travam a espera do Espresso ao digitar no diálogo do PIN.
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @After
    fun depois() {
        LockController.cancelScheduled()
        LockController.unlock(context)
        ShadowSettings.setCanDrawOverlays(false)
    }

    private var tours = 0

    private fun show() = compose.setContent { BabyLockTheme { HomeScreen(onShowTour = { tours++ }) } }

    /** Ações de todas as telas que o app tentou abrir até agora. */
    private fun startedActions(): List<String?> {
        val app = shadowOf(context as android.app.Application)
        return generateSequence { app.nextStartedActivity }.map { it.action }.toList()
    }

    @Test
    fun `sem permissoes avisa e nao deixa travar`() {
        show()
        compose.onNodeWithText("Falta uma permissão").assertIsDisplayed()
        compose.onNodeWithTag("enable_accessibility").assertIsDisplayed()
        compose.onNodeWithTag("enable_overlay").assertIsDisplayed()
        compose.onNodeWithTag("lock_button").assertIsNotEnabled()
    }

    @Test
    fun `com sobreposicao mostra protecao basica e trava com contagem`() {
        ShadowSettings.setCanDrawOverlays(true)
        show()
        compose.onNodeWithText("Proteção básica").assertIsDisplayed()
        compose.onNodeWithTag("enable_overlay").assertDoesNotExist()
        compose.onNodeWithTag("lock_button").assertIsEnabled().performClick()
        compose.onNodeWithText("Travando em 5 segundos… abra o vídeo").assertIsDisplayed()
        assertThat(LockController.countdown.value).isEqualTo(5)
        compose.onNodeWithTag("cancel_countdown").performClick()
        assertThat(LockController.countdown.value).isNull()
        compose.onNodeWithText("Travando em 5 segundos… abra o vídeo").assertDoesNotExist()
        compose.onNodeWithTag("lock_button").assertIsEnabled()
    }

    @Test
    fun `ligar acessibilidade mostra a divulgacao antes (exigencia do Google Play)`() {
        show()
        compose.onNodeWithTag("enable_accessibility").performClick()
        compose.onNodeWithText("Uso da acessibilidade").assertIsDisplayed()
        compose.onNodeWithTag("disclosure_dismiss").performClick()
        compose.onNodeWithText("Uso da acessibilidade").assertDoesNotExist()
        assertThat(Prefs.disclosureAccepted).isFalse()
        assertThat(startedActions()).doesNotContain(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    @Test
    fun `aceitar a divulgacao abre as configuracoes de acessibilidade`() {
        show()
        compose.onNodeWithTag("enable_accessibility").performClick()
        compose.onNodeWithTag("disclosure_accept").performClick()
        assertThat(Prefs.disclosureAccepted).isTrue()
        assertThat(startedActions()).contains(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    @Test
    fun `divulgacao ja aceita vai direto para as configuracoes`() {
        Prefs.disclosureAccepted = true
        show()
        compose.onNodeWithTag("enable_accessibility").performClick()
        compose.onNodeWithText("Uso da acessibilidade").assertDoesNotExist()
        assertThat(startedActions()).contains(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    @Test
    // Com a tela grande dos outros testes, o AlertDialog com campos de texto nunca fica ocioso
    // no Robolectric (acontece até com o tema padrão do Material). Na tela padrão ele funciona.
    @Config(qualifiers = "pt-rBR-w320dp-h470dp")
    fun `definir PIN exige 4 digitos e confirmacao igual`() {
        show()
        compose.onNodeWithTag("pin_button").performScrollTo().performClick()
        compose.onNodeWithTag("pin_save").assertIsNotEnabled()
        compose.onNodeWithTag("pin_new").performTextInput("123")
        compose.onNodeWithTag("pin_confirm").performTextInput("123")
        compose.onNodeWithTag("pin_save").assertIsNotEnabled()
        compose.onNodeWithTag("pin_new").performTextInput("4")
        compose.onNodeWithTag("pin_save").assertIsNotEnabled()
        compose.onNodeWithTag("pin_confirm").performTextInput("4")
        compose.onNodeWithTag("pin_save").assertIsEnabled().performClick()
        assertThat(Prefs.hasPin).isTrue()
        assertThat(Prefs.checkPin("1234")).isTrue()
        compose.onNodeWithText("Definido").assertExists()
    }

    @Test
    fun `explica como destravar com o tempo configurado`() {
        Prefs.holdSeconds = 4
        show()
        compose.onNodeWithText("Segure o canto de cima, à direita", substring = true).assertExists()
        compose.onNodeWithText("com um dedo por 4 segundos", substring = true).assertExists()
        compose.onNodeWithText("Segurar o canto por 4 segundos").assertExists()
    }

    @Test
    fun `divulgacao explica o proximo passo nas configuracoes`() {
        show()
        compose.onNodeWithTag("enable_accessibility").performClick()
        compose.onNodeWithText("toque em BabyLock e ligue a chave", substring = true).assertIsDisplayed()
        compose.onNodeWithText("não lê a tela", substring = true).assertIsDisplayed()
    }

    @Test
    fun `ajuda reabre as boas-vindas e mostra a privacidade`() {
        show()
        compose.onNodeWithTag("help_tour").performScrollTo().performClick()
        assertThat(tours).isEqualTo(1)
        compose.onNodeWithTag("help_privacy").performScrollTo().performClick()
        compose.onNodeWithText("Sua privacidade").assertIsDisplayed()
        compose.onNodeWithText("não tem acesso à internet", substring = true).assertExists()
        compose.onNodeWithTag("privacy_ok").performClick()
        compose.onNodeWithText("Sua privacidade").assertDoesNotExist()
    }

    @Test
    fun `ajuda da barra fica fechada quando nada aconteceu`() {
        show()
        compose.onNodeWithText("A barra de notificações ainda abre?").assertExists()
        compose.onNodeWithTag("shade_help_settings").assertDoesNotExist()
        compose.onNodeWithText("A barra de notificações ainda abre?").performScrollTo().performClick()
        compose.onNodeWithText("procure por", substring = true).assertExists()
        compose.onNodeWithTag("shade_help_dismiss").assertDoesNotExist()
    }

    @Test
    fun `barra que abriu durante o bloqueio vira aviso com passo a passo`() {
        Prefs.shadeEscaped = true
        show()
        compose.onNodeWithText("A barra de notificações abriu durante o bloqueio").assertIsDisplayed()
        compose.onNodeWithTag("shade_help_settings").performScrollTo().performClick()
        assertThat(startedActions()).contains(Settings.ACTION_SETTINGS)
        compose.onNodeWithTag("shade_help_dismiss").performScrollTo().performClick()
        assertThat(Prefs.shadeEscaped).isFalse()
        compose.onNodeWithText("A barra de notificações abriu durante o bloqueio").assertDoesNotExist()
        compose.onNodeWithText("A barra de notificações ainda abre?").assertExists()
    }

    @Test
    fun `cadeado do canto pode ser desligado nas configuracoes`() {
        show()
        compose.onNodeWithText("Mostrar cadeado no canto").performScrollTo().performClick()
        assertThat(Prefs.showCornerHint).isFalse()
    }

    @Test
    fun `botao de travar na tela comeca desligado e pode ser ligado`() {
        show()
        assertThat(Prefs.accessibilityButton).isFalse()
        compose.onNodeWithText("Botão de travar na tela").performScrollTo().performClick()
        assertThat(Prefs.accessibilityButton).isTrue()
        // No Android 11+ o adulto põe o botão nas Configurações de acessibilidade.
        assertThat(startedActions()).contains(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
}
