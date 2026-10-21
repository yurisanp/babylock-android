package app.babylock.lock

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.babylock.R
import app.babylock.ads.Interstitial
import app.babylock.settings.Prefs
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowToast
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class LockControllerTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @After
    fun limpar() {
        LockController.cancelScheduled()
        LockController.unlock(app)
        ShadowSettings.setCanDrawOverlays(false)
    }

    @Test
    fun `sem permissao nenhuma nao trava`() {
        ShadowSettings.setCanDrawOverlays(false)
        assertThat(LockController.availableEngine(app)).isEqualTo(LockController.Engine.NONE)
        assertThat(LockController.lockNow(app)).isFalse()
        assertThat(LockController.locked.value).isFalse()
    }

    @Test
    fun `com sobreposicao usa o motor basico`() {
        ShadowSettings.setCanDrawOverlays(true)
        assertThat(LockController.availableEngine(app)).isEqualTo(LockController.Engine.OVERLAY)
        assertThat(LockController.lockNow(app)).isTrue()
        assertThat(LockController.locked.value).isTrue()
        val started = shadowOf(app).nextStartedService
        assertThat(started.component?.className).isEqualTo(OverlayLockService::class.java.name)
    }

    @Test
    fun `desbloquear libera e para o servico`() {
        ShadowSettings.setCanDrawOverlays(true)
        LockController.lockNow(app)
        LockController.unlock(app)
        assertThat(LockController.locked.value).isFalse()
        val stopped = shadowOf(app).nextStoppedService
        assertThat(stopped.component?.className).isEqualTo(OverlayLockService::class.java.name)
    }

    @Test
    fun `fim de um bloqueio abre a pausa para o anuncio de tela cheia`() {
        app.getSharedPreferences("babylock", Context.MODE_PRIVATE).edit().clear().commit()
        Prefs.init(app)
        Interstitial.resetForTest()
        ShadowSettings.setCanDrawOverlays(true)
        LockController.unlock(app)
        // Desbloquear sem estar travado não conta.
        assertThat(Interstitial.pendingForTest).isFalse()
        LockController.lockNow(app)
        LockController.unlock(app)
        assertThat(Prefs.sessionsDone).isEqualTo(1)
        assertThat(Interstitial.pendingForTest).isTrue()
        Interstitial.resetForTest()
    }

    @Test
    fun `bloqueio agendado faz contagem regressiva e trava no fim`() {
        ShadowSettings.setCanDrawOverlays(true)
        LockController.scheduleLock(app, 3)
        assertThat(LockController.countdown.value).isEqualTo(3)
        advance(1_000)
        assertThat(LockController.countdown.value).isEqualTo(2)
        advance(1_000)
        assertThat(LockController.countdown.value).isEqualTo(1)
        assertThat(LockController.locked.value).isFalse()
        advance(1_000)
        assertThat(LockController.countdown.value).isNull()
        assertThat(LockController.locked.value).isTrue()
    }

    @Test
    fun `cancelar o agendamento nao trava`() {
        ShadowSettings.setCanDrawOverlays(true)
        LockController.scheduleLock(app, 3)
        advance(1_000)
        LockController.cancelScheduled()
        assertThat(LockController.countdown.value).isNull()
        advance(5_000)
        assertThat(LockController.locked.value).isFalse()
    }

    @Test
    fun `reagendar substitui o agendamento anterior`() {
        ShadowSettings.setCanDrawOverlays(true)
        LockController.scheduleLock(app, 2)
        LockController.scheduleLock(app, 5)
        advance(2_000)
        assertThat(LockController.locked.value).isFalse()
        assertThat(LockController.countdown.value).isEqualTo(3)
    }

    @Test
    fun `pedir desbloqueio esconde a camada e abre a tela do adulto`() {
        val surface = FakeSurface()
        LockController.activeSurface = surface
        LockController.requestUnlock(app)
        assertThat(surface.hidden).isEqualTo(1)
        val next = shadowOf(app).nextStartedActivity
        assertThat(next.component?.className).isEqualTo("app.babylock.unlock.UnlockActivity")
    }

    @Test
    fun `cancelar a tela do adulto volta a travar`() {
        ShadowSettings.setCanDrawOverlays(true)
        LockController.lockNow(app)
        val surface = FakeSurface()
        LockController.activeSurface = surface
        LockController.relock(app)
        assertThat(surface.shown).isEqualTo(1)
    }

    @Test
    fun `motor que some sozinho marca como destravado`() {
        val surface = FakeSurface()
        ShadowSettings.setCanDrawOverlays(true)
        LockController.lockNow(app)
        LockController.activeSurface = surface
        LockController.onSurfaceLost(surface, app)
        assertThat(LockController.locked.value).isFalse()
        assertThat(LockController.activeSurface).isNull()
    }

    @Test
    fun `travar e destravar mostram um aviso rapido`() {
        ShadowSettings.setCanDrawOverlays(true)
        LockController.lockNow(app)
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(app.getString(R.string.notice_locked))
        LockController.unlock(app)
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(app.getString(R.string.notice_unlocked))
    }

    @Test
    fun `destravar sem estar travado nao mostra aviso`() {
        ShadowToast.reset()
        LockController.unlock(app)
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
    }

    @Test
    fun `motor que some sozinho avisa que o bloqueio desligou`() {
        val surface = FakeSurface()
        ShadowSettings.setCanDrawOverlays(true)
        LockController.lockNow(app)
        LockController.activeSurface = surface
        LockController.onSurfaceLost(surface, app)
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(app.getString(R.string.notice_stopped))
    }

    private class FakeSurface : LockSurface {
        var shown = 0
        var hidden = 0
        var released = 0
        override fun showOverlay() { shown++ }
        override fun hideOverlay() { hidden++ }
        override fun release() { released++ }
    }
}
