package app.babylock.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.babylock.BuildConfig
import app.babylock.billing.Pro
import app.babylock.lock.LockController
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Faixa de anúncio no rodapé da tela inicial. Some com o Pro, enquanto a tela está travada ou na contagem,
 * e não aparece até o SDK estar pronto (nem nos testes).
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val ready by Ads.ready.collectAsStateWithLifecycle()
    val locked by LockController.locked.collectAsStateWithLifecycle()
    val countdown by LockController.countdown.collectAsStateWithLifecycle()
    val pro by Pro.owned.collectAsStateWithLifecycle()
    if (!ready || pro || locked || countdown != null || Ads.suppressed) return

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth.value.toInt()
        val context = LocalContext.current
        val adView = remember(width) {
            AdView(context).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
                loadAd(Ads.request())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().testTag("ad_banner"))
    }
}
