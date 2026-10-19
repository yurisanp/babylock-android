package app.babylock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.ads.Ads
import app.babylock.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    init {
        // Sem anúncios nos testes: a tela fica igual a cada execução.
        Ads.suppressed = true
    }

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun telaInicialAbre() {
        compose.onNodeWithText("BabyLock").assertIsDisplayed()
        // Na primeira abertura aparecem as boas-vindas; "Pular" leva ao início.
        if (compose.onAllNodesWithTag("onboarding_skip").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("onboarding_skip").performClick()
        }
        compose.onNodeWithText(compose.activity.getString(R.string.howto_title)).assertExists()
    }
}
