package app.babylock.ads

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.BuildConfig
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdsTest {

    @Test
    fun `pedido de anuncio e sempre nao personalizado`() {
        val extras = Ads.request().getNetworkExtrasBundle(AdMobAdapter::class.java)
        assertThat(extras?.getString("npa")).isEqualTo("1")
    }

    @Test
    fun `debug usa o banner de teste do Google`() {
        // Clicar em anúncio real do próprio app é proibido pelo AdMob: debug e testes nunca usam o ID real.
        assertThat(BuildConfig.ADMOB_BANNER_ID).isEqualTo("ca-app-pub-3940256099942544/9214589741")
    }

    @Test
    fun `sem iniciar o SDK nao ha anuncio nem opcoes de privacidade`() {
        assertThat(Ads.ready.value).isFalse()
        assertThat(Ads.privacyOptionsRequired.value).isFalse()
    }
}
