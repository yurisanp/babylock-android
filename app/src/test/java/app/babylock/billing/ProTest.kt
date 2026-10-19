package app.babylock.billing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.settings.Prefs
import com.android.billingclient.api.Purchase
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun depois() = Pro.setForTest(owned = false, price = null)

    private fun purchase(product: String, state: Int) =
        Purchase("""{"productIds":["$product"],"purchaseState":$state,"purchaseToken":"t","acknowledged":false}""", "")

    @Test
    fun `compra paga do Pro libera`() {
        assertThat(Pro.isPro(purchase(Pro.PRODUCT_ID, 0))).isTrue()
    }

    @Test
    fun `compra pendente nao libera ainda`() {
        // Pagamento em dinheiro ou boleto: só libera quando a Play confirmar.
        assertThat(Pro.isPro(purchase(Pro.PRODUCT_ID, 4))).isFalse()
    }

    @Test
    fun `outro produto nao libera`() {
        assertThat(Pro.isPro(purchase("outro", 0))).isFalse()
    }

    @Test
    fun `abre com a ultima resposta guardada`() {
        context.getSharedPreferences("babylock", Context.MODE_PRIVATE).edit().clear().commit()
        Prefs.init(context)
        Prefs.pro = true
        Pro.init()
        assertThat(Pro.owned.value).isTrue()
    }
}
