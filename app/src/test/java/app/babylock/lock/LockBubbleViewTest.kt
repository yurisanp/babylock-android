package app.babylock.lock

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockBubbleViewTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var taps = 0
    private var moved = 0 to 0
    private var drops = 0
    private val view = LockBubbleView(
        context,
        onTap = { taps++ },
        onDrag = { dx, dy -> moved = moved.first + dx to moved.second + dy },
        onDrop = { drops++ },
    )

    private fun touch(action: Int, x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        val e = MotionEvent.obtain(t, t, action, x, y, 0)
        view.onTouchEvent(e)
        e.recycle()
    }

    @Test
    fun `toque simples trava`() {
        touch(MotionEvent.ACTION_DOWN, 100f, 100f)
        touch(MotionEvent.ACTION_UP, 101f, 100f)
        assertThat(taps).isEqualTo(1)
        assertThat(drops).isEqualTo(0)
    }

    @Test
    fun `arrastar move o botao e nao trava`() {
        touch(MotionEvent.ACTION_DOWN, 100f, 100f)
        touch(MotionEvent.ACTION_MOVE, 160f, 130f)
        touch(MotionEvent.ACTION_MOVE, 200f, 300f)
        touch(MotionEvent.ACTION_UP, 200f, 300f)
        assertThat(taps).isEqualTo(0)
        assertThat(drops).isEqualTo(1)
        assertThat(moved).isEqualTo(100 to 200)
    }

    @Test
    fun `tem descricao para o leitor de tela`() {
        assertThat(view.contentDescription.toString()).isNotEmpty()
        assertThat(view.isClickable).isTrue()
    }
}
