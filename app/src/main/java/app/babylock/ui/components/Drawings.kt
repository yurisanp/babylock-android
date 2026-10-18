package app.babylock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cadeado desenhado à mão, para poder animar a alça.
 * [open] de 0 (fechado) a 1 (alça levantada e girada).
 * [moonHole] troca o furo da fechadura por uma lua (marca do app).
 */
@Composable
fun LockGlyph(
    modifier: Modifier = Modifier,
    body: Color,
    shackle: Color = body,
    hole: Color,
    open: Float = 0f,
    moonHole: Boolean = false,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.11f
        val bodyTop = h * 0.42f
        val bodyRect = Rect(0f, bodyTop, w, h)
        val arcW = w * 0.56f
        val arcLeft = (w - arcW) / 2f
        val arcTop = stroke / 2f
        val legBottom = bodyTop + stroke

        val lift = open * h * 0.14f
        val turn = open * -14f
        translate(top = -lift) {
            rotate(turn, pivot = Offset(arcLeft + arcW, legBottom)) {
                val p = Path().apply {
                    moveTo(arcLeft, legBottom)
                    lineTo(arcLeft, arcTop + arcW / 2f)
                    arcTo(Rect(arcLeft, arcTop, arcLeft + arcW, arcTop + arcW), 180f, 180f, false)
                    lineTo(arcLeft + arcW, legBottom)
                }
                drawPath(p, shackle, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
        drawRoundRect(body, bodyRect.topLeft, bodyRect.size, CornerRadius(w * 0.24f))

        val c = Offset(w / 2f, bodyTop + (h - bodyTop) * 0.52f)
        if (moonHole) {
            val r = w * 0.15f
            val moon = Path().apply {
                addOval(Rect(c, r))
            }
            val bite = Path().apply {
                addOval(Rect(Offset(c.x + r * 0.55f, c.y - r * 0.35f), r * 0.9f))
            }
            val crescent = Path().apply { op(moon, bite, androidx.compose.ui.graphics.PathOperation.Difference) }
            drawPath(crescent, hole)
        } else {
            drawCircle(hole, radius = w * 0.09f, center = c)
        }
    }
}

/** Anel de progresso que começa no topo e anda no sentido horário. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    track: Color,
    strokeWidth: Dp = 12.dp,
) {
    Canvas(modifier) {
        val s = strokeWidth.toPx()
        val d = size.minDimension - s
        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        drawArc(track, 0f, 360f, false, topLeft, Size(d, d), style = Stroke(s))
        if (progress > 0f) {
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft, Size(d, d), style = Stroke(s, cap = StrokeCap.Round))
        }
    }
}

/** Ícone de digital simplificado, em traço. */
@Composable
fun FingerprintGlyph(color: Color, modifier: Modifier = Modifier.size(52.dp)) {
    Canvas(modifier) {
        val u = size.width / 24f
        val st = Stroke(width = 1.7f * u, cap = StrokeCap.Round)
        fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float) =
            drawArc(color, start, sweep, false, Offset((cx - r) * u, (cy - r) * u), Size(2 * r * u, 2 * r * u), style = st)
        arc(12f, 12f, 9f, 200f, 140f)
        arc(12f, 13f, 6.5f, 180f, 200f)
        arc(12f, 13.5f, 4f, 170f, 230f)
        drawLine(color, Offset(12f * u, 12.5f * u), Offset(12f * u, 18f * u), st.width, StrokeCap.Round)
        arc(12f, 20f, 7.5f, 210f, 40f)
    }
}
