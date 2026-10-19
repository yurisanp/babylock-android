package app.babylock.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowBuild

@RunWith(AndroidJUnit4::class)
class DeviceTipsTest {

    private fun tipFor(manufacturer: String): DeviceTips.Tip? {
        ShadowBuild.setManufacturer(manufacturer)
        return DeviceTips.current()
    }

    @Test
    fun `samsung tem dica de bateria e de fixar app`() {
        val tip = tipFor("samsung")!!
        assertThat(tip.brand).isEqualTo("Samsung")
        assertThat(tip.steps.joinToString()).contains("Sem restrição")
        assertThat(tip.steps.joinToString()).contains("Fixar app")
    }

    @Test
    fun `xiaomi redmi e poco usam a mesma dica`() {
        listOf("Xiaomi", "Redmi", "POCO").forEach {
            val tip = tipFor(it)!!
            assertThat(tip.brand).isEqualTo("Xiaomi")
            assertThat(tip.steps.joinToString()).contains("pop-up")
            assertThat(tip.steps.joinToString()).contains("Fixar apps")
        }
    }

    @Test
    fun `passo a passo de fixar o app manda procurar Fixar nas configuracoes`() {
        assertThat(DeviceTips.pinSteps).isNotEmpty()
        assertThat(DeviceTips.pinSteps.first()).contains("procure por \"Fixar\"")
        assertThat(DeviceTips.pinSteps.joinToString()).contains("recentes")
    }

    @Test
    fun `motorola e oppo tem dicas`() {
        assertThat(tipFor("motorola")?.brand).isEqualTo("Motorola")
        assertThat(tipFor("OPPO")).isNotNull()
        assertThat(tipFor("realme")).isNotNull()
        assertThat(tipFor("OnePlus")).isNotNull()
    }

    @Test
    fun `Pixel e marcas desconhecidas nao mostram dica`() {
        assertThat(tipFor("Google")).isNull()
        assertThat(tipFor("unknown")).isNull()
    }
}
