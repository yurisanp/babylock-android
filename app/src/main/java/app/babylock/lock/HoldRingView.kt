package app.babylock.lock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator

/**
 * Anel que enche sob o dedo enquanto o adulto segura o canto.
 * Só desenho: quem decide se destrava continua sendo o [LockOverlayView].
 * Fica invisível no resto do tempo, para a criança não ver nada para tocar.
 *
 * Anima com um relógio próprio (Handler + tempo decorrido) em vez de ValueAnimator,
 * para nunca interferir no tempo do gesto que destrava.
 */
@SuppressLint("ViewConstructor")
internal class HoldRingView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private val stroke = 4 * density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = Color.argb(90, 255, 255, 255)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(90, 18, 24, 51) // NightSky translúcido
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        color = Color.rgb(91, 200, 168) // Mint
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val oval = RectF()

    private val ticker = Handler(Looper.getMainLooper())
    private val pop = OvershootInterpolator(2f)
    private val ease = PathInterpolator(0.2f, 0f, 0f, 1f)

    private var progress = 0f
    private var mode = Mode.IDLE
    private var startedAt = 0L
    private var duration = 0L
    private var fromProgress = 0f
    private var fromAlpha = 0f

    private enum class Mode { IDLE, FILLING, RETURNING }

    private val tick = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.uptimeMillis() - startedAt
            when (mode) {
                Mode.FILLING -> {
                    progress = (fromProgress + elapsed.toFloat() / duration).coerceAtMost(1f)
                    val t = (elapsed / APPEAR_MS.toFloat()).coerceIn(0f, 1f)
                    alpha = t
                    val s = 0.8f + 0.2f * pop.getInterpolation(t)
                    scaleX = s
                    scaleY = s
                    if (progress >= 1f && t >= 1f) mode = Mode.IDLE
                }
                Mode.RETURNING -> {
                    val t = ease.getInterpolation((elapsed / RETURN_MS.toFloat()).coerceIn(0f, 1f))
                    progress = fromProgress * (1f - t)
                    alpha = fromAlpha * (1f - t)
                    if (t >= 1f) mode = Mode.IDLE
                }
                Mode.IDLE -> return
            }
            invalidate()
            if (mode != Mode.IDLE) ticker.postDelayed(this, FRAME_MS)
        }
    }

    init {
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f
    }

    fun start(durationMs: Long) {
        ticker.removeCallbacks(tick)
        fromProgress = progress
        duration = (durationMs * (1f - progress)).toLong().coerceAtLeast(1L)
        startedAt = SystemClock.uptimeMillis()
        mode = Mode.FILLING
        ticker.post(tick)
    }

    /** Soltou antes da hora: o anel volta e some. */
    fun cancel() {
        if (mode == Mode.IDLE && alpha == 0f) return
        ticker.removeCallbacks(tick)
        fromProgress = progress
        fromAlpha = alpha
        startedAt = SystemClock.uptimeMillis()
        mode = Mode.RETURNING
        ticker.post(tick)
    }

    /** Completou: some na hora, porque a tela de confirmação vai abrir. */
    fun reset() {
        ticker.removeCallbacks(tick)
        mode = Mode.IDLE
        progress = 0f
        alpha = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val r = (minOf(width, height) - stroke) / 2f
        val cx = width / 2f
        val cy = height / 2f
        oval.set(cx - r, cy - r, cx + r, cy + r)
        canvas.drawCircle(cx, cy, r, fill)
        canvas.drawCircle(cx, cy, r, track)
        if (progress > 0f) canvas.drawArc(oval, -90f, 360f * progress, false, arc)
        canvas.drawCircle(cx, cy, r * 0.3f, dot)
    }

    override fun onDetachedFromWindow() {
        ticker.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    private companion object {
        const val FRAME_MS = 16L
        const val APPEAR_MS = 260L
        const val RETURN_MS = 320L
    }
}
