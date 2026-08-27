package tg.goddivor.jobcalender.ui.component

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.EventMode
import tg.goddivor.jobcalender.domain.usecase.MeetingBrand
import tg.goddivor.jobcalender.domain.usecase.meetingBrand

/**
 * Product marks, in their own colours.
 *
 * They are the one exception to the palette: an affordance that names a product is recognised by
 * its mark long before its label is read, and a WhatsApp button drawn in the accent colour says
 * nothing. Everywhere else the icons stay Material's, tinted by the theme.
 *
 * Bitmaps rather than vectors: two of these marks carry a gradient and a dozen overlapping shapes,
 * and hand-translating them to `VectorDrawable` would be a chance to draw them wrong.
 */
@Composable
fun BrandIcon(brand: MeetingBrand, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val drawable = when (brand) {
        MeetingBrand.TEAMS -> R.drawable.ic_brand_teams
        MeetingBrand.GOOGLE_MEET -> R.drawable.ic_brand_meet
        MeetingBrand.ZOOM -> R.drawable.ic_brand_zoom
        MeetingBrand.NONE -> null
    }
    if (drawable == null) {
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = null,
            modifier = modifier.size(size),
        )
    } else {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            modifier = modifier.size(size),
        )
    }
}

/** The mark of whatever this appointment will open, decided by its link first, then by its mode. */
@Composable
fun MeetingIcon(link: String?, mode: EventMode?, size: Dp = 18.dp, modifier: Modifier = Modifier) =
    BrandIcon(meetingBrand(link, mode), size, modifier)

/** The mark of the product an application left through, when that product has one. */
@Composable
fun ChannelIcon(channel: Channel, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val drawable = when (channel) {
        Channel.EMAIL -> R.drawable.ic_brand_gmail
        Channel.LINKEDIN -> R.drawable.ic_brand_linkedin
        Channel.WHATSAPP -> R.drawable.ic_brand_whatsapp
        // Emploi.tg has no mark of its own, and a web form or a hand delivery names no product.
        Channel.WEB_FORM, Channel.EMPLOI_TG, Channel.DIRECT -> null
    } ?: return
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

/**
 * GitHub's mark is black on light and white on dark, so it ships as two files rather than one tint.
 * Read from the surface actually being painted, not from the system setting: the theme can be forced
 * to light on a phone in dark mode, and the wallpaper's palette can decide it too.
 */
@Composable
fun GithubIcon(size: Dp = 24.dp, modifier: Modifier = Modifier) = Image(
    painter = painterResource(
        if (MaterialTheme.colorScheme.background.luminance() < DARK_ENOUGH) {
            R.drawable.ic_brand_github_light
        } else {
            R.drawable.ic_brand_github
        },
    ),
    contentDescription = stringResource(R.string.about_github),
    modifier = modifier.size(size),
)

private const val DARK_ENOUGH = 0.5f
