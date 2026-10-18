package app.babylock.lock

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Bloco nas Configurações rápidas: abre o vídeo, puxa a barra, toca no bloco, pronto. */
class LockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        update()
    }

    override fun onClick() {
        super.onClick()
        if (LockController.locked.value) return // destravar só pelo gesto + biometria
        // 2 s para a barra fechar e o vídeo voltar a aparecer.
        LockController.scheduleLock(applicationContext, 2)
        update()
    }

    private fun update() {
        val tile = qsTile ?: return
        tile.state = if (LockController.locked.value) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    companion object {
        fun refresh(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, LockTileService::class.java))
            }
        }
    }
}
