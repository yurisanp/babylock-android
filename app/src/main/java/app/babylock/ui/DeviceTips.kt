package app.babylock.ui

import android.os.Build

/** Dicas por fabricante (ver seção "Diferenças entre fabricantes" do plano). */
object DeviceTips {
    data class Tip(val brand: String, val steps: List<String>)

    /**
     * Fixar o app do vídeo é o jeito do próprio Android de barrar a barra de notificações de vez.
     * Serve para todas as marcas; o nome da opção muda, por isso o passo manda procurar "Fixar".
     */
    val pinSteps: List<String> = listOf(
        "Abra as Configurações e procure por \"Fixar\" (o nome muda: \"Fixar apps\", \"Fixação de tela\" ou \"Fixar app\").",
        "Ative a opção e também \"Pedir senha para desafixar\", se aparecer.",
        "Abra o vídeo, toque no botão de apps recentes, toque no ícone do app e escolha \"Fixar\".",
        "Depois trave o BabyLock como sempre. Com o app fixado, a barra de notificações não desce.",
    )

    fun current(): Tip? {
        val m = Build.MANUFACTURER.lowercase()
        return when {
            "samsung" in m -> Tip(
                "Samsung",
                listOf(
                    "Configurações > Apps > BabyLock > Bateria: escolha \"Sem restrição\".",
                    "Cuidados com o dispositivo > Bateria > Limites de uso em segundo plano: coloque o BabyLock em \"Apps que nunca suspendem\".",
                    "Para o Modo Fortaleza: Segurança e privacidade > Mais configurações de segurança > Fixar app, com \"Pedir PIN antes de desafixar\".",
                ),
            )
            "xiaomi" in m || "redmi" in m || "poco" in m -> Tip(
                "Xiaomi",
                listOf(
                    "Configurações > Apps > BabyLock > Economia de bateria: \"Sem restrições\".",
                    "Na mesma tela: ative \"Inicialização automática\".",
                    "Outras permissões: permita \"Exibir janelas pop-up em segundo plano\".",
                    "Em Recentes, segure o BabyLock e toque no cadeado para ele não ser fechado.",
                    "Se a barra de notificações ainda descer: em Configurações, procure \"Fixar apps\" e ative. " +
                        "Veja o passo a passo em \"A barra de notificações ainda abre?\".",
                ),
            )
            "motorola" in m -> Tip(
                "Motorola",
                listOf("Configurações > Apps > BabyLock > Bateria: \"Sem restrição\"."),
            )
            "oppo" in m || "realme" in m || "oneplus" in m -> Tip(
                Build.MANUFACTURER,
                listOf(
                    "Configurações > Bateria: permita atividade em segundo plano para o BabyLock.",
                    "Ative a inicialização automática e trave o app em Recentes.",
                ),
            )
            else -> null
        }
    }
}
