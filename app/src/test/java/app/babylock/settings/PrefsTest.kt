package app.babylock.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrefsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun limpar() {
        context.getSharedPreferences("babylock", Context.MODE_PRIVATE).edit().clear().commit()
        Prefs.init(context)
    }

    @Test
    fun `valores padrao sao os do plano`() {
        assertThat(Prefs.lockVolume).isTrue()
        assertThat(Prefs.closeShade).isTrue()
        assertThat(Prefs.holdSeconds).isEqualTo(3)
        assertThat(Prefs.disclosureAccepted).isFalse()
        assertThat(Prefs.hasPin).isFalse()
        assertThat(Prefs.showCornerHint).isTrue()
        assertThat(Prefs.shadeEscaped).isFalse()
        assertThat(Prefs.accessibilityButton).isFalse()
    }

    @Test
    fun `tempo de segurar o canto fica entre 2 e 6 segundos`() {
        Prefs.holdSeconds = 0
        assertThat(Prefs.holdSeconds).isEqualTo(2)
        Prefs.holdSeconds = 99
        assertThat(Prefs.holdSeconds).isEqualTo(6)
        Prefs.holdSeconds = 4
        assertThat(Prefs.holdSeconds).isEqualTo(4)
    }

    @Test
    fun `PIN confere so com o valor certo`() {
        Prefs.setPin("2468")
        assertThat(Prefs.hasPin).isTrue()
        assertThat(Prefs.checkPin("2468")).isTrue()
        assertThat(Prefs.checkPin("2469")).isFalse()
        assertThat(Prefs.checkPin("")).isFalse()
    }

    @Test
    fun `PIN nunca fica guardado em texto puro`() {
        Prefs.setPin("2468")
        val all = context.getSharedPreferences("babylock", Context.MODE_PRIVATE).all.values.map { it.toString() }
        assertThat(all).doesNotContain("2468")
    }

    @Test
    fun `mesmo PIN gera hash diferente a cada vez (sal aleatorio)`() {
        val sp = context.getSharedPreferences("babylock", Context.MODE_PRIVATE)
        Prefs.setPin("2468")
        val first = sp.getString("pin_hash", null)
        Prefs.setPin("2468")
        val second = sp.getString("pin_hash", null)
        assertThat(first).isNotEqualTo(second)
        assertThat(Prefs.checkPin("2468")).isTrue()
    }

    @Test
    fun `remover o PIN`() {
        Prefs.setPin("2468")
        Prefs.clearPin()
        assertThat(Prefs.hasPin).isFalse()
        assertThat(Prefs.checkPin("2468")).isFalse()
    }

    @Test
    fun `configuracoes sao salvas`() {
        Prefs.lockVolume = false
        Prefs.closeShade = false
        Prefs.disclosureAccepted = true
        Prefs.init(context) // simula reabrir o app
        assertThat(Prefs.lockVolume).isFalse()
        assertThat(Prefs.closeShade).isFalse()
        assertThat(Prefs.disclosureAccepted).isTrue()
    }
}
