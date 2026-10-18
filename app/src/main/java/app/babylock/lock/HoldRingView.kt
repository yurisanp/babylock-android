package app.babylock.lock

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator

/**
 * Anel que enche sob o dedo enquanto o adulto segura o canto.
 * Só desenho: quem decide se destrava continua sendo o [LockOverlayView].
 * Fica invisível no resto do tempo, para a criança não ver nada para tocar.
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
    private var progress = 0f
    private var animator: ValueAnimator? = null

    init {
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f
    }

    fun start(durationMs: Long) {
        animator?.cancel()
        animate().cancel()
        animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(2f)).start()
        animator = ValueAnimator.ofFloat(progress, 1f).apply {
            duration = (durationMs * (1f - progress)).toLong()
            interpolator = LinearInterpolator()
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    /** Soltou antes da hora: o anel volta e some. */
    fun cancel() {
        if (alpha == 0f && progress == 0f) return
        animator?.cancel()
        animator = ValueAnimator.ofFloat(progress, 0f).apply {
            duration = 320
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
        animate().cancel()
        animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(320).start()
    }

    /** Completou: some na hora, porque a tela de confirmação vai abrir. */
    fun reset() {
        animator?.cancel()
        animate().cancel()
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
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
