package app.babylock.unlock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.ui.theme.BabyLockTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnlockScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var submitted: String? = null
    private var cancelled = 0
    private var biometric = 0

    private fun show(canUseSystemAuth: Boolean, hasPin: Boolean, error: String? = null) {
        compose.setContent {
            BabyLockTheme {
                UnlockScreen(
                    canUseSystemAuth = canUseSystemAuth,
                    hasPin = hasPin,
                    challenge = 7 to 8,
                    error = error,
                    onSystemAuth = { biometric++ },
                    onSubmit = { submitted = it },
                    onCancel = { cancelled++ },
                )
            }
        }
    }

    @Test
    fun `sem biometria e sem PIN pede a conta de multiplicar`() {
        show(canUseSystemAuth = false, hasPin = false)
        compose.onNodeWithText("Quanto é 7 × 8?").assertIsDisplayed()
        compose.onNodeWithTag("unlock_biometric").assertDoesNotExist()
        compose.onNodeWithTag("unlock_confirm").assertIsNotEnabled()
        compose.onNodeWithTag("unlock_answer").performTextInput("56")
        compose.onNodeWithTag("unlock_confirm").assertIsEnabled().performClick()
        assertThat(submitted).isEqualTo("56")
    }

    @Test
    fun `campo aceita so digitos e no maximo 6`() {
        show(canUseSystemAuth = false, hasPin = true)
        compose.onNodeWithTag("unlock_answer").performTextInput("12a34567")
        compose.onNodeWithTag("unlock_confirm").performClick()
        assertThat(submitted).isEqualTo("123456")
    }

    @Test
    fun `com biometria e sem PIN mostra so o botao de biometria`() {
        show(canUseSystemAuth = true, hasPin = false)
        compose.onNodeWithTag("unlock_biometric").assertIsDisplayed().performClick()
        compose.onNodeWithTag("unlock_answer").assertDoesNotExist()
        assertThat(biometric).isEqualTo(1)
    }

    @Test
    fun `com biometria e PIN mostra os dois`() {
        show(canUseSystemAuth = true, hasPin = true)
        compose.onNodeWithTag("unlock_biometric").assertIsDisplayed()
        compose.onNodeWithText("PIN do BabyLock").assertIsDisplayed()
    }

    @Test
    fun `mostra a mensagem de erro`() {
        show(canUseSystemAuth = false, hasPin = true, error = "Não confere. Tente de novo.")
        compose.onNodeWithText("Não confere. Tente de novo.").assertIsDisplayed()
    }

    @Test
    fun `continuar bloqueado chama cancelar`() {
        show(canUseSystemAuth = false, hasPin = false)
        compose.onNodeWithTag("unlock_cancel").performClick()
        assertThat(cancelled).isEqualTo(1)
    }
}
