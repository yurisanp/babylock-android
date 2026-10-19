package app.babylock.lock

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Avisos rápidos de funcionamento ("Tela travada", "Tela destravada").
 * Usa um aviso curto do sistema (Toast): some sozinho em ~2 s e não deixa nada na barra de notificações.
 * Um aviso novo substitui o anterior, para não empilhar.
 */
internal object QuickNotice {

    private val main = Handler(Looper.getMainLooper())
    private var current: Toast? = null

    fun show(context: Context, @StringRes text: Int) {
        val app = context.applicationContext
        val run = Runnable {
            current?.cancel()
            current = Toast.makeText(app, text, Toast.LENGTH_SHORT).also { it.show() }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run() else main.post(run)
    }
}
