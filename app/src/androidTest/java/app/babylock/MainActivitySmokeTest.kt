package app.babylock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun telaInicialAbre() {
        compose.onNodeWithText("BabyLock").assertIsDisplayed()
        // Na primeira abertura aparecem as boas-vindas; "Pular" leva ao início.
        if (compose.onAllNodesWithTag("onboarding_skip").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("onboarding_skip").performClick()
        }
        compose.onNodeWithText("Como destravar").assertExists()
    }
}
