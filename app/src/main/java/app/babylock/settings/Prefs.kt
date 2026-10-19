package app.babylock.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/** Configurações locais do app. Nada sai do aparelho. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("babylock", Context.MODE_PRIVATE)
    }

    /** Bloquear os botões de volume durante o bloqueio (só no modo acessibilidade). */
    var lockVolume: Boolean
        get() = sp.getBoolean("lock_volume", true)
        set(v) = sp.edit().putBoolean("lock_volume", v).apply()

    /** Fechar a barra de notificações assim que ela abrir (só no modo acessibilidade). */
    var closeShade: Boolean
        get() = sp.getBoolean("close_shade", true)
        set(v) = sp.edit().putBoolean("close_shade", v).apply()

    /** Quantos segundos segurar o canto para chamar o desbloqueio. */
    var holdSeconds: Int
        get() = sp.getInt("hold_seconds", 3)
        set(v) = sp.edit().putInt("hold_seconds", v.coerceIn(2, 6)).apply()

    /**
     * Mostrar o botão flutuante do BabyLock para travar com um toque, já dentro do vídeo.
     * Desligado por padrão, para não aparecer no uso normal do celular.
     */
    var accessibilityButton: Boolean
        get() = sp.getBoolean("accessibility_button", false)
        set(v) = sp.edit().putBoolean("accessibility_button", v).apply()

    /** Onde o adulto deixou o botão flutuante (em pixels; -1 = lugar padrão, na borda direita). */
    var bubbleX: Int
        get() = sp.getInt("bubble_x", -1)
        set(v) = sp.edit().putInt("bubble_x", v).apply()

    var bubbleY: Int
        get() = sp.getInt("bubble_y", -1)
        set(v) = sp.edit().putInt("bubble_y", v).apply()

    /** Mostrar um cadeado discreto no canto de desbloqueio enquanto travado. */
    var showCornerHint: Boolean
        get() = sp.getBoolean("show_corner_hint", true)
        set(v) = sp.edit().putBoolean("show_corner_hint", v).apply()

    /**
     * A barra de notificações abriu durante um bloqueio e precisou ser fechada à força.
     * A tela inicial usa isso para ensinar a fixar o app (que barra a barra de vez).
     */
    var shadeEscaped: Boolean
        get() = sp.getBoolean("shade_escaped", false)
        set(v) = sp.edit().putBoolean("shade_escaped", v).apply()

    /** O adulto aceitou a divulgação do uso de acessibilidade (exigência do Google Play). */
    var disclosureAccepted: Boolean
        get() = sp.getBoolean("disclosure_accepted", false)
        set(v) = sp.edit().putBoolean("disclosure_accepted", v).apply()

    /** Última resposta da Play sobre a compra do BabyLock Pro (vale até a Play responder de novo). */
    var pro: Boolean
        get() = sp.getBoolean("pro", false)
        set(v) = sp.edit().putBoolean("pro", v).apply()

    /** As boas-vindas já foram vistas (ou puladas). */
    var onboardingDone: Boolean
        get() = sp.getBoolean("onboarding_done", false)
        set(v) = sp.edit().putBoolean("onboarding_done", v).apply()

    val hasPin: Boolean get() = sp.contains("pin_hash")

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp.edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", hash(salt, pin))
            .apply()
    }

    fun clearPin() {
        sp.edit().remove("pin_salt").remove("pin_hash").apply()
    }

    fun checkPin(pin: String): Boolean {
        val salt = sp.getString("pin_salt", null) ?: return false
        val stored = sp.getString("pin_hash", null) ?: return false
        val candidate = hash(Base64.decode(salt, Base64.NO_WRAP), pin)
        return MessageDigest.isEqual(stored.toByteArray(), candidate.toByteArray())
    }

    private fun hash(salt: ByteArray, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val bytes = md.digest(pin.toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
