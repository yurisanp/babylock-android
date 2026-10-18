package app.babylock.lock

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import app.babylock.unlock.UnlockActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ponto único para travar e destravar.
 * Escolhe o motor: serviço de acessibilidade (completo) se estiver ligado; senão overlay comum (básico).
 */
object LockController {

    enum class Engine { ACCESSIBILITY, OVERLAY, NONE }

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** Segundos que faltam para um bloqueio agendado, ou null. */
    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var tick: Runnable? = null

    @Volatile
    internal var activeSurface: LockSurface? = null

    fun availableEngine(context: Context): Engine = when {
        LockAccessibilityService.instance != null -> Engine.ACCESSIBILITY
        Settings.canDrawOverlays(context) -> Engine.OVERLAY
        else -> Engine.NONE
    }

    /** Trava agora. Retorna false se nenhuma permissão foi dada ainda. */
    fun lockNow(context: Context): Boolean {
        cancelScheduled()
        val app = context.applicationContext
        when (availableEngine(app)) {
            Engine.ACCESSIBILITY -> {
                val svc = LockAccessibilityService.instance ?: return false
                activeSurface = svc
                svc.showOverlay()
            }
            Engine.OVERLAY -> ContextCompat.startForegroundService(app, OverlayLockService.lockIntent(app))
            Engine.NONE -> return false
        }
        _locked.value = true
        LockTileService.refresh(app)
        return true
    }

    /** Trava depois de alguns segundos, para dar tempo de abrir o vídeo. */
    fun scheduleLock(context: Context, seconds: Int) {
        cancelScheduled()
        val app = context.applicationContext
        var left = seconds
        _countdown.value = left
        val r = object : Runnable {
            override fun run() {
                left -= 1
                if (left <= 0) {
                    _countdown.value = null
                    tick = null
                    lockNow(app)
                } else {
                    _countdown.value = left
                    main.postDelayed(this, 1000)
                }
            }
        }
        tick = r
        main.postDelayed(r, 1000)
    }

    fun cancelScheduled() {
        tick?.let { main.removeCallbacks(it) }
        tick = null
        _countdown.value = null
    }

    /** O gesto do canto foi feito: esconde a camada e pede a confirmação do adulto. */
    fun requestUnlock(context: Context) {
        activeSurface?.hideOverlay()
        val intent = Intent(context, UnlockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        context.startActivity(intent)
    }

    /** O adulto não confirmou (cancelou, saiu ou o tempo acabou): volta a travar. */
    fun relock() {
        if (_locked.value) activeSurface?.showOverlay()
    }

    /** Desbloqueio confirmado. */
    fun unlock(context: Context) {
        val app = context.applicationContext
        activeSurface?.release()
        activeSurface = null
        app.stopService(Intent(app, OverlayLockService::class.java))
        _locked.value = false
        LockTileService.refresh(app)
    }

    /** Chamado quando o motor some sozinho (ex.: o sistema desligou o serviço). */
    internal fun onSurfaceLost(surface: LockSurface, context: Context) {
        if (activeSurface === surface) {
            activeSurface = null
            _locked.value = false
            LockTileService.refresh(context.applicationContext)
        }
    }
}
