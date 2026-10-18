package app.babylock.lock

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class LockOverlayViewTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var unlockRequests = 0
    private lateinit var view: LockOverlayView

    private val w = 1080
    private val h = 2400
    private val corner get() = w - 20f to 20f

    @Before
    fun montar() {
        view = LockOverlayView(context, holdMillis = 3_000) { unlockRequests++ }
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, w, h)
    }

    private fun event(action: Int, x: Float, y: Float, pointers: Int = 1): MotionEvent {
        val t = SystemClock.uptimeMillis()
        if (pointers == 1) return MotionEvent.obtain(t, t, action, x, y, 0)
        val props = Array(pointers) { i -> MotionEvent.PointerProperties().apply { id = i } }
        val coords = Array(pointers) { i -> MotionEvent.PointerCoords().apply { this.x = x - i * 10; this.y = y + i * 10 } }
        return MotionEvent.obtain(t, t, action, pointers, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
    }

    private fun wait(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun `todo toque e consumido`() {
        assertThat(view.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, 500f, 1200f))).isTrue()
        assertThat(view.dispatchTouchEvent(event(MotionEvent.ACTION_UP, 500f, 1200f))).isTrue()
        assertThat(unlockRequests).isEqualTo(0)
    }

    @Test
    fun `segurar o canto pelo tempo certo pede o desbloqueio`() {
        val (x, y) = corner
        view.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, x, y))
        wait(2_900)
        assertThat(unlockRequests).isEqualTo(0)
        wait(200)
        assertThat(unlockRequests).isEqualTo(1)
    }

    @Test
    fun `soltar antes do tempo nao desbloqueia`() {
        val (x, y) = corner
        view.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, x, y))
        wait(2_000)
        view.dispatchTouchEvent(event(MotionEvent.ACTION_UP, x, y))
        wait(5_000)
        assertThat(unlockRequests).isEqualTo(0)
    }

    @Test
    fun `segurar fora do canto nao desbloqueia`() {
        listOf(20f to 20f, w - 20f to h - 20f, w / 2f to h / 2f).forEach { (x, y) ->
            view.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, x, y))
            wait(5_000)
            view.dispatchTouchEvent(event(MotionEvent.ACTION_UP, x, y))
        }
        assertThat(unlockRequests).isEqualTo(0)
    }

    @Test
    fun `arrastar para fora do canto cancela`() {
        val (x, y) = corner
        view.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, x, y))
        wait(1_000)
        view.dispatchTouchEvent(event(MotionEvent.ACTION_MOVE, x - 500, y + 500))
        wait(5_000)
        assertThat(unlockRequests).isEqualTo(0)
    }

    @Test
    fun `segundo dedo cancela (crianca batendo na tela)`() {
        val (x, y) = corner
        view.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, x, y))
        wait(1_000)
        val second = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        view.dispatchTouchEvent(event(second, x, y, pointers = 2))
        wait(5_000)
        assertThat(unlockRequests).isEqualTo(0)
    }

    @Test
    fun `sair da tela cancela o gesto em andamento`() {
        val (x, y) = corner
        view.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, x, y))
        wait(1_000)
        view.dispatchTouchEvent(event(MotionEvent.ACTION_CANCEL, x, y))
        wait(5_000)
        assertThat(unlockRequests).isEqualTo(0)
    }
}
