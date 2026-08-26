package tg.goddivor.jobcalender.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.ui.theme.AppPalette
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme
import tg.goddivor.jobcalender.ui.theme.ThemeMode
import tg.goddivor.jobcalender.ui.theme.dynamicColorAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SettingsTopBar(stringResource(R.string.settings_section_appearance), onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel(stringResource(R.string.appearance_section_theme))
            ThemeModeRow(state.themeMode, viewModel::setThemeMode)
            PaletteRow(state.palette, state.themeMode, state.amoled, viewModel::setPalette)
            Hint(stringResource(R.string.palette_dynamic_note))
            SwitchRow(
                title = stringResource(R.string.theme_amoled),
                subtitle = stringResource(R.string.theme_amoled_sub),
                checked = state.amoled,
                onCheckedChange = viewModel::setAmoled,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeRow(current: ThemeMode, onPick: (ThemeMode) -> Unit) {
    val labels = listOf(
        ThemeMode.SYSTEM to R.string.theme_mode_system,
        ThemeMode.LIGHT to R.string.theme_mode_light,
        ThemeMode.DARK to R.string.theme_mode_dark,
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_PADDING, vertical = 4.dp),
    ) {
        labels.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = mode == current,
                onClick = { onPick(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                icon = {
                    if (mode == current) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            ) {
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun PaletteRow(
    current: AppPalette,
    themeMode: ThemeMode,
    amoled: Boolean,
    onPick: (AppPalette) -> Unit,
) {
    // Dynamic colour has nothing to show before Android 12, so the card is not offered there.
    val palettes = remember {
        AppPalette.entries.filter { it != AppPalette.DYNAMIC || dynamicColorAvailable }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = ROW_PADDING, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = palettes, key = { it.name }) { palette ->
            Column(modifier = Modifier.width(114.dp)) {
                // Each card is wrapped in the theme it stands for, so it always shows true colours.
                JobCalenderTheme(themeMode = themeMode, palette = palette, amoled = amoled) {
                    PalettePreview(
                        selected = palette == current,
                        onClick = { onPick(palette) },
                    )
                }
                Text(
                    text = stringResource(
                        when (palette) {
                            AppPalette.DEFAULT -> R.string.palette_default
                            AppPalette.DYNAMIC -> R.string.palette_dynamic
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

/** A miniature of this very app: an app bar, the next appointment, a month grid, a bottom bar. */
@Composable
private fun PalettePreview(selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .border(
                width = 4.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(17.dp),
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(9.dp)
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(3.dp)),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.theme_selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp).size(14.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .alpha(0.55f)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer, RoundedCornerShape(3.dp)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(6.dp)
                    .alpha(0.55f)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer, RoundedCornerShape(3.dp)),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(DAYS_IN_PREVIEW_WEEK) { index ->
                val accented = index == 2
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .alpha(if (accented) 1f else 0.22f)
                        .background(
                            if (accented) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.35f)
                            .weight(1f)
                            .alpha(0.55f)
                            .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

private const val DAYS_IN_PREVIEW_WEEK = 7
