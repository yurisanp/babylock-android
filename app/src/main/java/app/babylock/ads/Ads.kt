package app.babylock.ads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import app.babylock.billing.Pro
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Anúncios do AdMob, só nas telas do adulto e nunca durante o bloqueio.
 *
 * Regras do plano (ECA Digital): anúncios não personalizados ("npa") e só conteúdo livre (classificação G).
 * Onde a lei pede (Europa, Reino Unido etc.), o Google mostra antes o pedido de consentimento (UMP).
 */
object Ads {

    /** Os testes no emulador desligam os anúncios para as telas ficarem iguais a cada execução. */
    @Volatile
    var suppressed = false

    private val _ready = MutableStateFlow(false)

    /** O SDK está pronto e o consentimento (quando exigido) permite pedir anúncios. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)

    /** Em alguns países a pessoa precisa conseguir rever a escolha de consentimento a qualquer hora. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val started = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)

    /** Chamado quando a tela inicial aparece (depois das boas-vindas). Seguro de chamar várias vezes. */
    fun start(activity: Activity) {
        // Quem comprou o Pro não vê anúncio nem o pedido de consentimento.
        if (suppressed || Pro.owned.value || !started.compareAndSet(false, true)) return
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    afterConsent(activity, consent)
                }
            },
            { afterConsent(activity, consent) },
        )
        // Quem já respondeu em outra abertura não precisa esperar a rede.
        if (consent.canRequestAds()) initialize(activity)
    }

    /** Abre de novo o formulário do Google para a pessoa mudar a escolha sobre anúncios. */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    /** Pedido de anúncio sempre não personalizado. */
    fun request(): AdRequest =
        AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply { putString("npa", "1") })
            .build()

    private fun afterConsent(context: Context, consent: ConsentInformation) {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consent.canRequestAds()) initialize(context)
    }

    private fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        val app = context.applicationContext
        // O Google recomenda iniciar fora da linha principal; a resposta volta na principal.
        Thread { MobileAds.initialize(app) { _ready.value = true } }.start()
    }
}
