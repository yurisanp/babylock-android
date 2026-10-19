package app.babylock.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.R
import app.babylock.settings.Prefs
import app.babylock.ui.theme.BabyLockTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

/** O app fala português, inglês e espanhol. Idiomas sem tradução caem no inglês. */
@RunWith(AndroidJUnit4::class)
class LocalizationTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun limpar() {
        context.getSharedPreferences("babylock", Context.MODE_PRIVATE).edit().clear().commit()
        Prefs.init(context)
    }

    private fun inLocale(tag: String): Context {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        return context.createConfigurationContext(config)
    }

    @Test
    @Config(qualifiers = "en-w411dp-h891dp")
    fun `tela inicial em ingles`() {
        compose.setContent { BabyLockTheme { HomeScreen() } }
        compose.onNodeWithText("One permission missing").assertIsDisplayed()
        compose.onNodeWithText("How to unlock").assertExists()
    }

    @Test
    @Config(qualifiers = "es-w411dp-h891dp")
    fun `tela inicial em espanhol`() {
        compose.setContent { BabyLockTheme { HomeScreen() } }
        compose.onNodeWithText("Falta un permiso").assertIsDisplayed()
        compose.onNodeWithText("Cómo desbloquear").assertExists()
    }

    @Test
    @Config(qualifiers = "es-w411dp-h891dp")
    fun `boas-vindas em espanhol`() {
        compose.setContent { BabyLockTheme { OnboardingScreen(onDone = {}) } }
        compose.onNodeWithText("Omitir").assertIsDisplayed()
    }

    @Test
    fun `idioma sem traducao usa ingles`() {
        assertThat(inLocale("fr-FR").getString(R.string.unlock_cancel)).isEqualTo("Keep locked")
        assertThat(inLocale("de").getString(R.string.howto_title)).isEqualTo("How to unlock")
    }

    @Test
    fun `portugues de Portugal e espanhol da America Latina tambem sao atendidos`() {
        assertThat(inLocale("pt-PT").getString(R.string.unlock_cancel)).isEqualTo("Continuar bloqueado")
        assertThat(inLocale("es-MX").getString(R.string.unlock_cancel)).isEqualTo("Seguir bloqueado")
    }

    @Test
    fun `plurais e numeros formatados em cada idioma`() {
        val cases = mapOf("pt-BR" to "1 segundo", "en" to "1 second", "es" to "1 segundo")
        cases.forEach { (tag, expected) ->
            val text = inLocale(tag).resources.getQuantityString(R.plurals.countdown_hint, 1, 1)
            assertThat(text).contains(expected)
        }
        assertThat(inLocale("en").getString(R.string.unlock_math, 7, 8)).isEqualTo("What is 7 × 8?")
        assertThat(inLocale("pt-BR").getString(R.string.notification_text, 3)).contains("3 segundos")
    }

    @Test
    fun `dicas por fabricante seguem o idioma`() {
        org.robolectric.shadows.ShadowBuild.setManufacturer("samsung")
        val en = DeviceTips.current(inLocale("en"))!!
        val es = DeviceTips.current(inLocale("es"))!!
        assertThat(en.steps.first()).contains("Unrestricted")
        assertThat(es.steps.first()).contains("Sin restricciones")
    }
}
