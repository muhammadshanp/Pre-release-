package com.dudeapp.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BarHeight = 58.dp

/**
 * How much of the bottom of the screen the bar covers, system navigation included.
 * Lists add this to their bottom padding so the last item can scroll clear.
 */
@Composable
fun floatingBarSpace(): Dp =
    BarHeight + 1.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    REELS("Reels", Icons.Outlined.PlayCircle, Icons.Filled.PlayCircle),
    IMAGES("Images", Icons.Outlined.Image, Icons.Filled.Image),
    // "Files" rather than "PDFs": the page holds photos and PDFs, and at a narrow width
    // "PDFs" and "Notes" crowded the Home label.
    PDFS("Files", Icons.Outlined.PictureAsPdf, Icons.Filled.PictureAsPdf),
    NOTES("Notes", Icons.AutoMirrored.Outlined.StickyNote2, Icons.AutoMirrored.Filled.StickyNote2),
}

/**
 * The tab bar, in the design's language: it sits on the page rather than floating over it,
 * a single hairline separates it from the content, and the current tab is simply inked
 * while the rest stay #828282. No capsule, no shadow, no sliding pill.
 */
@Composable
fun PagerBottomBar(
    pagerState: PagerState,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected by remember(pagerState) {
        derivedStateOf { Tab.entries[pagerState.targetPage.coerceIn(0, Tab.entries.lastIndex)] }
    }
    BottomBar(selected = selected, onSelect = onSelect, modifier = modifier)
}

@Composable
fun BottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = preReColors()
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.page),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = inset)
                .height(BarHeight)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (tab in Tab.entries) {
                val active = tab == selected
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(
                            selected = active,
                            onClick = { onSelect(tab) },
                            role = Role.Tab,
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (active) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        tint = if (active) colors.ink else colors.muted,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = tab.label,
                        color = if (active) colors.ink else colors.muted,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
