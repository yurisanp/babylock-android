package app.babylock

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import java.util.concurrent.atomic.AtomicInteger

/** Tela cheia que só conta quantos toques recebeu e se está com o foco. */
class TouchProbeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val content = if (intent.getBooleanExtra(EXTRA_CARTOON, false)) CartoonView(this) else View(this)
        setContentView(
            content.apply {
                setBackgroundColor(Color.rgb(46, 58, 110))
                setOnTouchListener { _, e ->
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) touches.incrementAndGet()
                    true
                }
            },
        )
        current = this
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        focused = hasFocus
    }

    override fun onDestroy() {
        if (current === this) current = null
        super.onDestroy()
    }

    companion object {
        /** Mostra um "desenho" animado no lugar da tela lisa (só para o vídeo de demonstração). */
        const val EXTRA_CARTOON = "cartoon"

        val touches = AtomicInteger(0)

        @Volatile
        var focused = false

        @Volatile
        var current: TouchProbeActivity? = null
    }
}

/** "Desenho animado" simples para o vídeo da Play: céu, sol pulando, nuvens e a barra do player andando. */
private class CartoonView(context: android.content.Context) : View(context) {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val start = android.os.SystemClock.uptimeMillis()

    override fun onDraw(canvas: android.graphics.Canvas) {
        val t = (android.os.SystemClock.uptimeMillis() - start) / 1000f
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(Color.rgb(135, 206, 235))
        paint.color = Color.rgb(91, 200, 168)
        canvas.drawRect(0f, h * 0.72f, w, h, paint)
        // Sol pulando.
        val bounce = kotlin.math.abs(kotlin.math.sin(t * 2.2f))
        paint.color = Color.rgb(255, 200, 60)
        canvas.drawCircle(w / 2, h * 0.62f - bounce * h * 0.3f, w * 0.12f, paint)
        // Nuvens andando.
        paint.color = Color.WHITE
        for (i in 0..2) {
            val x = ((t * 60f + i * w / 2.5f) % (w + 300f)) - 150f
            val y = h * (0.12f + i * 0.08f)
            canvas.drawCircle(x, y, 60f, paint)
            canvas.drawCircle(x + 70f, y + 10f, 50f, paint)
            canvas.drawCircle(x - 70f, y + 10f, 50f, paint)
        }
        // Barra do player.
        paint.color = Color.argb(140, 0, 0, 0)
        canvas.drawRect(0f, h - 120f, w, h - 108f, paint)
        paint.color = Color.rgb(230, 50, 50)
        canvas.drawRect(0f, h - 120f, w * ((t / 120f) % 1f), h - 108f, paint)
        postInvalidateOnAnimation()
    }
}
