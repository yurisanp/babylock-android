package app.babylock.billing

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import app.babylock.settings.Prefs
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BabyLock Pro: compra única e vitalícia (produto "babylock_pro" na Play) que tira os anúncios.
 * O preço vem da Play, já na moeda de cada país. A compra é conferida a cada abertura,
 * então trocar de celular ou reinstalar devolve o Pro sozinho, e um reembolso o tira.
 */
object Pro {
    const val PRODUCT_ID = "babylock_pro"

    private val _owned = MutableStateFlow(false)

    /** A pessoa comprou o Pro: nada de anúncios. */
    val owned: StateFlow<Boolean> = _owned.asStateFlow()

    private val _price = MutableStateFlow<String?>(null)

    /** Preço formatado pela Play (ex.: "R$ 14,99"); nulo enquanto a Play não respondeu. */
    val price: StateFlow<String?> = _price.asStateFlow()

    private var client: BillingClient? = null
    private var details: ProductDetails? = null

    /** Usa a última resposta guardada, para o anúncio não piscar antes da Play responder. */
    fun init() {
        _owned.value = Prefs.pro
    }

    /** Conecta na Play, confere compras antigas e busca o preço. Seguro de chamar várias vezes. */
    fun start(context: Context) {
        if (client != null) return
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener { result, purchases ->
                if (result.responseCode == BillingResponseCode.OK && purchases != null) handle(purchases, complete = false)
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) refresh(c)
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Abre a tela de pagamento da Play. Falso se a Play ainda não respondeu. */
    fun buy(activity: Activity): Boolean {
        val c = client ?: return false
        val d = details ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        return c.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    private fun refresh(c: BillingClient) {
        c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) handle(purchases, complete = true)
        }
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(ProductType.INAPP)
            .build()
        c.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { result, found ->
            if (result.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            details = found.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
            @Suppress("DEPRECATION")
            _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    /**
     * [complete] = a lista é tudo o que a pessoa tem (consulta de compras); sem o Pro nela, ele foi reembolsado.
     * Compras novas precisam ser confirmadas em até 3 dias, senão a Play devolve o dinheiro.
     */
    private fun handle(purchases: List<Purchase>, complete: Boolean) {
        val bought = purchases.filter(::isPro)
        if (bought.isNotEmpty() || complete) setOwned(bought.isNotEmpty())
        bought.filterNot { it.isAcknowledged }.forEach { p ->
            client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
        }
    }

    /** Compra paga do Pro. Uma compra pendente (boleto, por exemplo) ainda não libera. */
    @VisibleForTesting
    internal fun isPro(purchase: Purchase): Boolean =
        PRODUCT_ID in purchase.products && purchase.purchaseState == Purchase.PurchaseState.PURCHASED

    private fun setOwned(value: Boolean) {
        Prefs.pro = value
        _owned.value = value
    }

    @VisibleForTesting
    internal fun setForTest(owned: Boolean, price: String?) {
        _owned.value = owned
        _price.value = price
    }
}
