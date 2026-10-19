package app.babylock.lock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ImageView
import app.babylock.R
import kotlin.math.abs

/**
 * Botão flutuante de travar, desenhado pelo próprio BabyLock por cima dos outros apps.
 * Um toque trava. Arrastar muda o lugar dele. Não depende do atalho de acessibilidade do sistema
 * (que em alguns celulares, como os Xiaomi, desliga o serviço em vez de avisar o app).
 */
@SuppressLint("ViewConstructor")
class LockBubbleView(
    context: Context,
    private val onTap: () -> Unit,
    /** Quanto o dedo andou desde o último aviso, em pixels. */
    private val onDrag: (dx: Int, dy: Int) -> Unit = { _, _ -> },
    private val onDrop: () -> Unit = {},
) : ImageView(context) {

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    init {
        setImageResource(R.drawable.ic_lock)
        val pad = (14 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(220, 30, 33, 40))
            setStroke((2 * resources.displayMetrics.density).toInt(), Color.argb(200, 255, 255, 255))
        }
        alpha = 0.9f
        contentDescription = context.getString(R.string.lock_bubble_description)
        isClickable = true
    }

    @SuppressLint("ClickableViewAccessibility") // performClick é chamado no toque simples.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX; downY = event.rawY
                lastX = downX; lastY = downY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && (abs(event.rawX - downX) > slop || abs(event.rawY - downY) > slop)) dragging = true
                if (dragging) {
                    val dx = (event.rawX - lastX).toInt()
                    val dy = (event.rawY - lastY).toInt()
                    lastX += dx; lastY += dy
                    if (dx != 0 || dy != 0) onDrag(dx, dy)
                }
            }
            MotionEvent.ACTION_UP -> if (dragging) onDrop() else performClick()
            MotionEvent.ACTION_CANCEL -> if (dragging) onDrop()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        onTap()
        return true
    }
}
