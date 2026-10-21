package app.babylock.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.babylock.BuildConfig
import app.babylock.R
import app.babylock.billing.Pro
import app.babylock.lock.LockController
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Faixa de anúncio no rodapé da tela inicial. Some com o Pro, enquanto a tela está travada ou na contagem,
 * e não aparece até o SDK estar pronto (nem nos testes). Com [onRemoveAds], mostra acima um convite curto ao Pro.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier, onRemoveAds: (() -> Unit)? = null) {
    val ready by Ads.ready.collectAsStateWithLifecycle()
    val locked by LockController.locked.collectAsStateWithLifecycle()
    val countdown by LockController.countdown.collectAsStateWithLifecycle()
    val pro by Pro.owned.collectAsStateWithLifecycle()
    if (!ready || pro || locked || countdown != null || Ads.suppressed) return

    // Lembrete discreto, colado no anúncio: só quando a Play já mostrou o preço do Pro.
    if (onRemoveAds != null) RemoveAdsHint(onRemoveAds)
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

/** Uma linha curta acima do anúncio: por que ele existe e como tirar. */
@Composable
internal fun RemoveAdsHint(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp).testTag("ads_hint"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.ads_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClick, modifier = Modifier.testTag("ads_hint_remove")) {
            Text(stringResource(R.string.ads_hint_action), style = MaterialTheme.typography.labelLarge)
        }
    }
}
