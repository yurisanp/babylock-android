package app.babylock.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.ui.theme.BabyLockTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
// Tela de um celular comum: no tamanho padrão do Robolectric (320x470 dp) o botão fica fora da tela.
@Config(qualifiers = "w411dp-h891dp")
class OnboardingScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var done = 0

    private fun show() = compose.setContent { BabyLockTheme { OnboardingScreen(onDone = { done++ }) } }

    @Test
    fun `passa pelas tres paginas e termina em Comecar`() {
        show()
        compose.onNodeWithText("Só você destrava.").assertDoesNotExist()
        compose.onNodeWithTag("onboarding_next").performClick()
        compose.onNodeWithText("Só você destrava.").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_next").performClick()
        compose.onNodeWithText("Nada sai do seu celular.").assertIsDisplayed()
        assertThat(done).isEqualTo(0)
        compose.onNodeWithText("Começar").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_next").performClick()
        assertThat(done).isEqualTo(1)
    }

    @Test
    fun `pular termina na hora`() {
        show()
        compose.onNodeWithTag("onboarding_skip").performClick()
        assertThat(done).isEqualTo(1)
    }
}
