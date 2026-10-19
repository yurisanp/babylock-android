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
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
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
    showCornerHint: Boolean = true,
    private val onUnlockRequested: () -> Unit,
) : FrameLayout(context) {

    private val cornerPx = (96 * resources.displayMetrics.density)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val trigger = Runnable {
        holding = false
        ring.reset()
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

    /**
     * Cadeado pequeno e meio transparente no canto de desbloqueio, sempre visível enquanto travado,
     * para o adulto lembrar onde segurar. Pequeno o bastante para não atrapalhar o vídeo.
     */
    internal val cornerHint = ImageView(context).apply {
        setImageResource(R.drawable.ic_lock)
        val pad = (7 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(110, 30, 33, 40))
        }
        alpha = CORNER_ALPHA
        contentDescription = "Segure aqui para destravar"
        visibility = if (showCornerHint) VISIBLE else GONE
    }

    /** Anel de progresso no canto, só enquanto o canto está sendo segurado. */
    private val ring = HoldRingView(context)

    init {
        setBackgroundColor(Color.TRANSPARENT)
        val size = (72 * resources.displayMetrics.density).toInt()
        addView(hint, LayoutParams(size, size, Gravity.CENTER))
        val ringSize = (64 * resources.displayMetrics.density).toInt()
        val inset = ((cornerPx - ringSize) / 2f).toInt()
        val markSize = (32 * resources.displayMetrics.density).toInt()
        val markInset = ((cornerPx - markSize) / 2f).toInt()
        addView(
            cornerHint,
            LayoutParams(markSize, markSize, Gravity.TOP or Gravity.END).apply { setMargins(0, markInset, markInset, 0) },
        )
        addView(ring, LayoutParams(ringSize, ringSize, Gravity.TOP or Gravity.END).apply { setMargins(0, inset, inset, 0) })
    }

    /** Mostra o cadeado por um instante (com um pequeno salto) e some, para o adulto saber que travou. */
    fun flashHint() {
        // O cadeado do canto pulsa junto, para mostrar onde fica o desbloqueio.
        if (cornerHint.visibility == VISIBLE) {
            cornerHint.animate().cancel()
            cornerHint.alpha = 1f
            cornerHint.scaleX = 1.35f
            cornerHint.scaleY = 1.35f
            cornerHint.animate().alpha(CORNER_ALPHA).scaleX(1f).scaleY(1f)
                .setStartDelay(1200).setDuration(700)
                .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f))
                .start()
        }
        hint.animate().cancel()
        hint.alpha = 0f
        hint.scaleX = 0.8f
        hint.scaleY = 0.8f
        hint.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setStartDelay(0).setDuration(320)
            .setInterpolator(OvershootInterpolator(2.2f))
            .withEndAction {
                hint.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f)
                    .setStartDelay(900).setDuration(600)
                    .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f))
                    .start()
            }
            .start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                if (inCorner(event.x, event.y)) {
                    holding = true
                    mainHandler.postDelayed(trigger, holdMillis)
                    ring.start(holdMillis)
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
        ring.cancel()
    }

    override fun onDetachedFromWindow() {
        cancelHold()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val CORNER_ALPHA = 0.55f
    }
}
