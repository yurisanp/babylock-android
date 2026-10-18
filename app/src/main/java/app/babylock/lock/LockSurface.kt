package app.babylock.lock

import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager

/** Algo que sabe mostrar e esconder a camada de bloqueio (serviço de acessibilidade ou overlay comum). */
interface LockSurface {
    fun showOverlay()
    fun hideOverlay()

    /** Encerra o bloqueio de vez (remove a camada e libera recursos). */
    fun release()
}

/** Parâmetros da janela de bloqueio: tela inteira, sem tirar o foco do app de vídeo, tela sempre acesa. */
@Suppress("DEPRECATION")
internal fun lockWindowParams(type: Int): WindowManager.LayoutParams =
    WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        type,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        PixelFormat.TRANSLUCENT,
    ).apply {
        title = "BabyLock"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            fitInsetsTypes = 0
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
