package app.babylock.ui

import android.content.Context
import android.os.Build
import androidx.annotation.ArrayRes
import app.babylock.R

/** Dicas por fabricante (ver seção "Diferenças entre fabricantes" do plano). Os textos ficam em strings.xml, um por idioma. */
object DeviceTips {
    data class Tip(val brand: String, val steps: List<String>)

    /**
     * Fixar o app do vídeo é o jeito do próprio Android de barrar a barra de notificações de vez.
     * Serve para todas as marcas; o nome da opção muda, por isso o passo manda procurar "Fixar".
     */
    fun pinSteps(context: Context): List<String> = context.resources.getStringArray(R.array.shade_pin_steps).toList()

    fun current(context: Context): Tip? {
        val m = Build.MANUFACTURER.lowercase()
        fun tip(brand: String, @ArrayRes steps: Int) = Tip(brand, context.resources.getStringArray(steps).toList())
        return when {
            "samsung" in m -> tip("Samsung", R.array.tips_samsung)
            "xiaomi" in m || "redmi" in m || "poco" in m -> tip("Xiaomi", R.array.tips_xiaomi)
            "motorola" in m -> tip("Motorola", R.array.tips_motorola)
            "oppo" in m || "realme" in m || "oneplus" in m -> tip(Build.MANUFACTURER, R.array.tips_oppo)
            else -> null
        }
    }
}
