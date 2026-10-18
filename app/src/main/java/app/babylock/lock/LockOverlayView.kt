package app.babylock.lock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import app.babylock.R

/**
 * Camada transparente que engole todos os toques.
 * Só um gesto escapa: segurar o canto superior direito com UM dedo por [holdMillis].
 * Um segundo dedo ou arrastar para fora do canto cancela (criança batendo na tela não desbloqueia).
 */
@SuppressLint("ViewConstructor")
class LockOverlayView(
    context: Context,
    private val holdMillis: Long,
    private val onUnlockRequested: () -> Unit,
) : FrameLayout(context) {

    private val cornerPx = (96 * resources.displayMetrics.density)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val trigger = Runnable {
        holding = false
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onUnlockRequested()
    }
    private var holding = false

    private val hint = ImageView(context).apply {
        setImageResource(R.drawable.ic_lock)
        val pad = (20 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(140, 30, 33, 40))
        }
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        val size = (72 * resources.displayMetrics.density).toInt()
        addView(hint, LayoutParams(size, size, Gravity.CENTER))
    }

    /** Mostra o cadeado por um instante e some, para o adulto saber que travou. */
    fun flashHint() {
        hint.animate().cancel()
        hint.alpha = 1f
        hint.animate().alpha(0f).setStartDelay(1200).setDuration(600).start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                if (inCorner(event.x, event.y)) {
                    holding = true
                    mainHandler.postDelayed(trigger, holdMillis)
                }
            MotionEvent.ACTION_POINTER_DOWN -> cancelHold()
            MotionEvent.ACTION_MOVE -> if (holding && !inCorner(event.x, event.y)) cancelHold()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelHold()
        }
        return true // consome tudo: nada chega ao vídeo
    }

    private fun inCorner(x: Float, y: Float) = x >= width - cornerPx && y <= cornerPx

    private fun cancelHold() {
        holding = false
        mainHandler.removeCallbacks(trigger)
    }

    override fun onDetachedFromWindow() {
        cancelHold()
        super.onDetachedFromWindow()
    }
}
