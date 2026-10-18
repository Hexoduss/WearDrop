package com.weardrop.ui.outfits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weardrop.R
import com.weardrop.stylist.Occasion
import com.weardrop.stylist.OutfitTitle
import com.weardrop.stylist.Reason
import com.weardrop.stylist.Tip
import com.weardrop.ui.components.EmptyState
import com.weardrop.ui.components.ItemImage
import com.weardrop.ui.components.OutfitCard
import com.weardrop.ui.components.formatDay
import com.weardrop.ui.components.itemDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitsScreen(vm: OutfitsViewModel = viewModel()) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.outfits_title)) }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_favorites)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_history)) })
            }
            if (tab == 0) SavedList(saved, vm) else HistoryList(history, vm)
        }
    }
}

@Composable
private fun SavedList(entries: List<SavedEntry>?, vm: OutfitsViewModel) {
    when {
        entries == null -> Unit
        entries.isEmpty() -> EmptyState(
            icon = Icons.Outlined.FavoriteBorder,
            title = stringResource(R.string.favorites_empty_title),
            text = stringResource(R.string.favorites_empty_text),
        )

        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(entries, key = { it.outfit.id }) { entry ->
                val outfit = entry.outfit
                val occasion = Occasion.fromKey(outfit.occasion)?.let { stringResource(it.label) }
                val savedOn = stringResource(R.string.saved_on, formatDay(outfit.createdAt))
                OutfitCard(
                    title = stringResource(OutfitTitle.fromKey(outfit.title).label),
                    reasons = Reason.parseList(outfit.reasons).map { stringResource(it.label) },
                    tip = Tip.fromKey(outfit.tip)?.let { stringResource(it.label) },
                    pieces = entry.pieces,
                    subtitle = listOfNotNull(occasion, savedOn).joinToString(" · "),
                ) {
                    OutlinedButton(
                        onClick = { vm.wearToday(entry) },
                        enabled = entry.pieces.isNotEmpty(),
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.wear_today))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteSaved(outfit) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(entries: List<HistoryEntry>?, vm: OutfitsViewModel) {
    when {
        entries == null -> Unit
        entries.isEmpty() -> EmptyState(
            icon = Icons.Outlined.History,
            title = stringResource(R.string.history_empty_title),
            text = stringResource(R.string.history_empty_text),
        )

        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(entries, key = { it.log.id }) { entry ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            entry.pieces.take(3).forEach { piece ->
                                ItemImage(
                                    piece.imagePath,
                                    contentDescription = itemDisplayName(piece),
                                    modifier = Modifier
                                        .size(width = 38.dp, height = 50.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(OutfitTitle.fromKey(entry.log.title).label),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                formatDay(entry.log.wornAt).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { vm.deleteWear(entry.log) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}
