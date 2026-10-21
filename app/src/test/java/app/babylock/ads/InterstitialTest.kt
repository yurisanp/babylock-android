package app.babylock.ads

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.BuildConfig
import app.babylock.settings.Prefs
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class InterstitialTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val now = 1_800_000_000_000L

    @Before
    fun limpar() {
        context.getSharedPreferences("babylock", Context.MODE_PRIVATE).edit().clear().commit()
        Prefs.init(context)
        Interstitial.resetForTest()
    }

    @After
    fun depois() = Interstitial.resetForTest()

    @Test
    fun `debug usa o intersticial de teste do Google`() {
        assertThat(BuildConfig.ADMOB_INTERSTITIAL_ID).isEqualTo("ca-app-pub-3940256099942544/1033173712")
        assertThat(Interstitial.enabled).isTrue()
    }

    @Test
    fun `primeiro bloqueio de quem instalou fica sem anuncio`() {
        assertThat(Interstitial.allowed(pending = true, sessionsDone = 1, lastShownAt = 0L, now = now)).isFalse()
        assertThat(Interstitial.allowed(pending = true, sessionsDone = 2, lastShownAt = 0L, now = now)).isTrue()
    }

    @Test
    fun `so aparece depois que um bloqueio terminou`() {
        assertThat(Interstitial.allowed(pending = false, sessionsDone = 5, lastShownAt = 0L, now = now)).isFalse()
    }

    @Test
    fun `respeita o intervalo minimo entre anuncios`() {
        val pouco = now - Interstitial.MIN_GAP_MS + 1
        val suficiente = now - Interstitial.MIN_GAP_MS
        assertThat(Interstitial.allowed(pending = true, sessionsDone = 5, lastShownAt = pouco, now = now)).isFalse()
        assertThat(Interstitial.allowed(pending = true, sessionsDone = 5, lastShownAt = suficiente, now = now)).isTrue()
    }

    @Test
    fun `relogio que voltou no tempo nao trava o anuncio para sempre`() {
        assertThat(Interstitial.allowed(pending = true, sessionsDone = 5, lastShownAt = now + 60_000, now = now)).isTrue()
    }

    @Test
    fun `fim do bloqueio conta a sessao e marca a pausa`() {
        Interstitial.onSessionEnded()
        assertThat(Prefs.sessionsDone).isEqualTo(1)
        assertThat(Interstitial.pendingForTest).isTrue()
    }

    @Test
    fun `sem anuncio carregado nao mostra nada e nao guarda para depois`() {
        Prefs.sessionsDone = 3
        Interstitial.onSessionEnded()
        val activity = Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        assertThat(Interstitial.maybeShow(activity)).isFalse()
        assertThat(Interstitial.pendingForTest).isFalse()
        assertThat(Prefs.lastInterstitialAt).isEqualTo(0L)
    }
}
