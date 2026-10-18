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
        setContentView(
            View(this).apply {
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
        val touches = AtomicInteger(0)

        @Volatile
        var focused = false

        @Volatile
        var current: TouchProbeActivity? = null
    }
}
