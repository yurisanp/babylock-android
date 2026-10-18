package app.babylock.unlock

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.lock.LockController
import app.babylock.lock.LockSurface
import app.babylock.settings.Prefs
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.shadows.ShadowSettings

@RunWith(AndroidJUnit4::class)
class UnlockActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val surface = FakeSurface()

    // Antes de abrir a Activity: PIN definido e o app "travado" com uma camada falsa.
    private val setup = object : TestRule {
        override fun apply(base: Statement, description: Description) = object : Statement() {
            override fun evaluate() {
                context.getSharedPreferences("babylock", Context.MODE_PRIVATE).edit().clear().commit()
                Prefs.init(context)
                Prefs.setPin("2468")
                ShadowSettings.setCanDrawOverlays(true)
                LockController.lockNow(context)
                LockController.activeSurface = surface
                base.evaluate()
            }
        }
    }

    private val compose = createAndroidComposeRule<UnlockActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(setup).around(compose)

    @After
    fun depois() {
        LockController.unlock(context)
        ShadowSettings.setCanDrawOverlays(false)
    }

    @Test
    fun `PIN certo destrava e fecha a tela`() {
        compose.onNodeWithTag("unlock_answer").performTextInput("2468")
        compose.onNodeWithTag("unlock_confirm").performClick()
        assertThat(LockController.locked.value).isFalse()
        assertThat(compose.activity.isFinishing).isTrue()
    }

    @Test
    fun `PIN errado mostra erro e continua travado`() {
        compose.onNodeWithTag("unlock_answer").performTextInput("1111")
        compose.onNodeWithTag("unlock_confirm").performClick()
        compose.onNodeWithText("Não confere. Tente de novo.").assertExists()
        assertThat(LockController.locked.value).isTrue()
        assertThat(compose.activity.isFinishing).isFalse()
    }

    @Test
    fun `continuar bloqueado volta a camada e fecha a tela`() {
        compose.onNodeWithTag("unlock_cancel").performClick()
        assertThat(LockController.locked.value).isTrue()
        assertThat(surface.shown).isAtLeast(1)
        assertThat(compose.activity.isFinishing).isTrue()
    }

    @Test
    fun `voltar tambem volta a travar`() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        assertThat(LockController.locked.value).isTrue()
        assertThat(surface.shown).isAtLeast(1)
    }

    private class FakeSurface : LockSurface {
        var shown = 0
        override fun showOverlay() { shown++ }
        override fun hideOverlay() = Unit
        override fun release() = Unit
    }
}
