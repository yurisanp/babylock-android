package app.babylock.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.settings.Prefs
import app.babylock.ui.theme.BabyLockTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Temporário: isola o que deixa o Compose sem ficar ocioso com o diálogo do PIN aberto. */
@RunWith(AndroidJUnit4::class)
class DiagnosticoPinTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun preparar() {
        context.getSharedPreferences("babylock", Context.MODE_PRIVATE).edit().clear().commit()
        Prefs.init(context)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @Test
    fun `a dialogo sozinho no tema do app`() {
        compose.setContent { BabyLockTheme { PinDialog(false, {}, {}, {}) } }
        compose.onNodeWithTag("pin_save").assertIsNotEnabled()
    }

    @Test
    fun `b dialogo sozinho no tema padrao`() {
        compose.setContent { MaterialTheme { PinDialog(false, {}, {}, {}) } }
        compose.onNodeWithTag("pin_save").assertIsNotEnabled()
    }

    @Test
    fun `c home rolada sem abrir o dialogo`() {
        compose.setContent { BabyLockTheme { HomeScreen() } }
        compose.onNodeWithTag("pin_button").performScrollTo()
        compose.onNodeWithTag("pin_button").assertExists()
    }

    @Test
    @Config(qualifiers = "w320dp-h470dp")
    fun `d home na tela pequena abrindo o dialogo`() {
        compose.setContent { BabyLockTheme { HomeScreen() } }
        compose.onNodeWithTag("pin_button").performScrollTo().performClick()
        compose.onNodeWithTag("pin_save").assertIsNotEnabled()
    }

    @Test
    fun `e home sem rolar abrindo o dialogo`() {
        compose.setContent { BabyLockTheme { HomeScreen() } }
        compose.onNodeWithTag("pin_button").performClick()
        compose.onNodeWithTag("pin_save").assertIsNotEnabled()
    }
}
