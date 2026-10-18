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
import app.babylock.settings.Prefs
import app.babylock.ui.theme.BabyLockTheme
import app.babylock.ui.theme.Motion

class MainActivity : ComponentActivity() {
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
                AnimatedContent(
                    targetState = onboarded,
                    transitionSpec = {
                        (fadeIn(tween(Motion.ENTER_MS, 120)) + scaleIn(tween(Motion.ENTER_MS, 120, Motion.Emphasized), initialScale = 0.97f)) togetherWith
                            fadeOut(tween(240))
                    },
                    label = "root",
                ) { done ->
                    if (done) {
                        HomeScreen()
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
