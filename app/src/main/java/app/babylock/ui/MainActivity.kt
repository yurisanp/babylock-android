package app.babylock.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import app.babylock.ads.Ads
import app.babylock.billing.Pro
import app.babylock.settings.Prefs
import app.babylock.ui.theme.BabyLockTheme
import app.babylock.ui.theme.Motion

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BabyLockTheme {
                var onboarded by rememberSaveable { mutableStateOf(Prefs.onboardingDone) }
                val dark = isSystemInDarkTheme()
                // Boas-vindas têm fundo escuro: ícones da barra de status claros.
                LaunchedEffect(onboarded, dark) {
                    val style = if (!onboarded || dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                // Anúncios e o pedido de consentimento só depois das boas-vindas, já na tela do adulto.
                LaunchedEffect(onboarded) {
                    if (onboarded) {
                        Pro.start(this@MainActivity)
                        Ads.start(this@MainActivity)
                    }
                }
                AnimatedContent(
                    targetState = onboarded,
                    transitionSpec = {
                        (fadeIn(tween(Motion.ENTER_MS, 120)) + scaleIn(tween(Motion.ENTER_MS, 120, Motion.Emphasized), initialScale = 0.97f)) togetherWith
                            fadeOut(tween(240))
                    },
                    label = "root",
                    // Expõe as testTags como resource-id para os testes no emulador (UiAutomator).
                    modifier = Modifier.semantics { testTagsAsResourceId = true },
                ) { done ->
                    if (done) {
                        HomeScreen(onShowTour = {
                            Prefs.onboardingDone = false
                            onboarded = false
                        })
                    } else {
                        OnboardingScreen(onDone = {
                            Prefs.onboardingDone = true
                            onboarded = true
                        })
                    }
                }
            }
        }
    }
}
