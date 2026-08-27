package tg.goddivor.jobcalender.ui.component

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tg.goddivor.jobcalender.R

/**
 * Three ways to reach a recruiter. A button whose channel is missing stays visible and disabled:
 * an absent button says nothing, a greyed one says "no number was ever given".
 */
@Composable
fun ContactActions(
    email: String?,
    phone: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ContactButton(
            brand = R.drawable.ic_brand_gmail,
            label = stringResource(R.string.action_email),
            enabled = email != null,
            modifier = Modifier.weight(1f),
        ) { context.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) }

        ContactButton(
            icon = Icons.Filled.Call,
            label = stringResource(R.string.action_call),
            enabled = phone != null,
            modifier = Modifier.weight(1f),
        ) { context.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }

        ContactButton(
            brand = R.drawable.ic_brand_whatsapp,
            label = stringResource(R.string.action_whatsapp),
            enabled = phone != null,
            modifier = Modifier.weight(1f),
        ) {
            val digits = phone.orEmpty().filter { it.isDigit() }
            context.launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
        }
    }
}

/**
 * One of the three ways out. A button that opens a named product carries its mark; calling opens no
 * product in particular, so it keeps Material's handset. A disabled button fades whichever it is,
 * mark included, or a full-colour logo would read as available.
 */
@Composable
private fun ContactButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    @DrawableRes brand: Int? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 11.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            val tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
            when {
                brand != null -> Image(
                    painter = painterResource(brand),
                    contentDescription = null,
                    alpha = if (enabled) 1f else DISABLED_ALPHA,
                    modifier = Modifier.size(24.dp),
                )
                icon != null -> Icon(imageVector = icon, contentDescription = null, tint = tint)
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

private const val DISABLED_ALPHA = 0.4f

/** A missing handler app must not take the screen down with it. */
internal fun Context.launch(intent: Intent) {
    runCatching { startActivity(intent) }.recoverCatching { error ->
        if (error !is ActivityNotFoundException) throw error
    }
}
