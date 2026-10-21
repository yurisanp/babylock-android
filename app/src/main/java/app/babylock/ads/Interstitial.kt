package app.babylock.ads

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import app.babylock.BuildConfig
import app.babylock.billing.Pro
import app.babylock.lock.LockController
import app.babylock.settings.Prefs
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Anúncio de tela cheia (intersticial). Só aparece numa pausa natural: quando o adulto volta para a
 * tela inicial do BabyLock depois de um bloqueio que terminou. Nunca ao travar, nunca com a criança
 * usando o celular, nunca para quem tem o Pro, e no máximo um a cada [MIN_GAP_MS].
 * O primeiro bloqueio de quem acabou de instalar fica sem anúncio.
 */
object Interstitial {

    /** Intervalo mínimo entre dois anúncios de tela cheia. */
    const val MIN_GAP_MS = 15 * 60_000L

    /** Quantos bloqueios terminam sem anúncio logo depois de instalar. */
    const val FREE_SESSIONS = 1

    /** Sem ID (release antes de criar o bloco no AdMob), o anúncio de tela cheia fica desligado. */
    val enabled: Boolean get() = BuildConfig.ADMOB_INTERSTITIAL_ID.isNotBlank()

    @Volatile
    private var pending = false
    private var ad: InterstitialAd? = null
    private var loading = false

    private val _offerPro = MutableStateFlow(false)

    /** O anúncio acabou de fechar: a tela inicial lembra, uma vez, que o Pro tira os anúncios. */
    val offerPro: StateFlow<Boolean> = _offerPro.asStateFlow()

    fun proOffered() {
        _offerPro.value = false
    }

    /** Um bloqueio terminou com o desbloqueio do adulto. */
    fun onSessionEnded() {
        Prefs.sessionsDone += 1
        pending = true
    }

    /** Regra de frequência, separada para poder testar. */
    @VisibleForTesting
    internal fun allowed(pending: Boolean, sessionsDone: Int, lastShownAt: Long, now: Long): Boolean =
        pending && sessionsDone > FREE_SESSIONS && (now - lastShownAt >= MIN_GAP_MS || now < lastShownAt)

    private fun blocked(): Boolean =
        !enabled || Ads.suppressed || Pro.owned.value || !Ads.ready.value ||
            LockController.locked.value || LockController.countdown.value != null

    /** Deixa um anúncio carregado, para ele abrir na hora certa sem espera. */
    fun preload(context: Context) {
        if (blocked() || ad != null || loading) return
        loading = true
        InterstitialAd.load(
            context.applicationContext,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            Ads.request(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            },
        )
    }

    /**
     * Chamado quando a tela inicial volta para a frente. Mostra o anúncio só se um bloqueio acabou de
     * terminar e as regras deixam. Se não der agora, não guarda para depois (nada de anúncio surpresa).
     */
    fun maybeShow(activity: Activity): Boolean {
        val wasPending = pending
        pending = false
        if (blocked()) return false
        val now = System.currentTimeMillis()
        val loaded = ad
        if (loaded == null || !allowed(wasPending, Prefs.sessionsDone, Prefs.lastInterstitialAt, now)) {
            preload(activity)
            return false
        }
        ad = null
        val app = activity.applicationContext
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                _offerPro.value = true
                preload(app)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preload(app)
            }
        }
        Prefs.lastInterstitialAt = now
        loaded.show(activity)
        return true
    }

    @VisibleForTesting
    internal fun setOfferProForTest(value: Boolean) {
        _offerPro.value = value
    }

    @VisibleForTesting
    internal val pendingForTest: Boolean get() = pending

    @VisibleForTesting
    internal fun resetForTest() {
        pending = false
        ad = null
        loading = false
        _offerPro.value = false
    }
}
