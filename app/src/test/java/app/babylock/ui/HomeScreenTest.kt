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
    }

    @After
    fun depois() {
        LockController.cancelScheduled()
        LockController.unlock(context)
        ShadowSettings.setCanDrawOverlays(false)
    }

    private fun show() = compose.setContent { BabyLockTheme { HomeScreen() } }

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
        compose.onNodeWithText("Travando em 5 s… abra o vídeo").assertIsDisplayed()
        assertThat(LockController.countdown.value).isEqualTo(5)
        compose.onNodeWithTag("cancel_countdown").performClick()
        assertThat(LockController.countdown.value).isNull()
        compose.onNodeWithText("Travando em 5 s… abra o vídeo").assertDoesNotExist()
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
        compose.onNodeWithText("Segure o canto superior direito", substring = true).assertExists()
        compose.onNodeWithText("por 4 segundos", substring = true).assertExists()
    }
}
