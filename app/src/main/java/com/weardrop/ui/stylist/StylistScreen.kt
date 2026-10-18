package com.weardrop.ui.stylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weardrop.R
import com.weardrop.stylist.Occasion
import com.weardrop.ui.components.ItemImage
import com.weardrop.ui.components.OutfitCard
import com.weardrop.ui.components.SectionLabel
import com.weardrop.ui.components.itemDisplayName
import com.weardrop.ui.components.reasonTexts

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StylistScreen(
    onOpenSettings: () -> Unit,
    vm: StylistViewModel = viewModel(),
) {
    val anchor by vm.anchor.collectAsStateWithLifecycle()
    val preferredStyles by vm.preferredStyles.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_stylist)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(stringResource(R.string.stylist_headline), style = MaterialTheme.typography.headlineMedium)
            }

            item {
                WeatherCard(
                    state = vm.weather,
                    onRefresh = { vm.refreshWeather(force = true) },
                    onOpenSettings = onOpenSettings,
                )
            }

            item {
                Column {
                    SectionLabel(stringResource(R.string.occasion_label))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Occasion.entries.forEach { option ->
                            FilterChip(
                                selected = vm.occasion == option,
                                onClick = { vm.occasion = option },
                                label = { Text(stringResource(option.label)) },
                            )
                        }
                    }
                }
            }

            item {
                StyleRow(preferredStyles.map { stringResource(it.label) }, onOpenSettings)
            }

            anchor?.let { piece ->
                item {
                    Column {
                        SectionLabel(stringResource(R.string.anchor_label))
                        InputChip(
                            selected = true,
                            onClick = vm::clearAnchor,
                            label = { Text(itemDisplayName(piece)) },
                            avatar = {
                                ItemImage(
                                    piece.imagePath,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.anchor_remove),
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = vm::generate,
                    enabled = !vm.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (vm.loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(if (vm.results.isEmpty()) R.string.stylist_generate else R.string.stylist_more),
                        )
                    }
                }
            }

            vm.error?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(
                            stringResource(message),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            itemsIndexed(vm.results) { index, result ->
                val suggestion = result.suggestion
                OutfitCard(
                    title = stringResource(suggestion.title.label),
                    reasons = reasonTexts(suggestion.reasons, suggestion.matchedStyle),
                    tip = stringResource(suggestion.tip.label),
                    pieces = suggestion.pieces,
                    subtitle = suggestion.matchedStyle?.let { stringResource(it.label) },
                ) {
                    FilledTonalButton(onClick = { vm.save(index) }, enabled = !result.saved) {
                        Icon(
                            if (result.saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (result.saved) R.string.saved else R.string.save))
                    }
                    OutlinedButton(onClick = { vm.wearToday(index) }, enabled = !result.worn) {
                        Icon(
                            if (result.worn) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (result.worn) R.string.worn else R.string.wear_today))
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleRow(styles: List<String>, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SectionLabel(stringResource(R.string.stylist_style_label))
            Text(
                if (styles.isEmpty()) stringResource(R.string.stylist_style_none) else styles.joinToString(" · "),
                style = if (styles.isEmpty()) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = if (styles.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
        TextButton(onClick = onOpenSettings) {
            Text(stringResource(if (styles.isEmpty()) R.string.stylist_style_choose else R.string.stylist_style_change))
        }
    }
}

@Composable
private fun WeatherCard(state: WeatherState, onRefresh: () -> Unit, onOpenSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (state) {
                WeatherState.Loading -> {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.weather_loading), style = MaterialTheme.typography.bodyMedium)
                }

                WeatherState.NoCity -> {
                    Text(
                        stringResource(R.string.weather_no_city),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.weather_set)) }
                }

                is WeatherState.Failed -> {
                    Text(
                        stringResource(state.message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRefresh) { Text(stringResource(R.string.weather_retry)) }
                }

                is WeatherState.Ready -> {
                    val w = state.weather
                    Text(w.emoji, fontSize = 34.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.weather_now, w.temperature, stringResource(w.description)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val details = stringResource(R.string.weather_details, w.place, w.min, w.max)
                        Text(
                            if (w.rainChance != null) stringResource(R.string.weather_rain, details, w.rainChance) else details,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.weather_refresh))
                    }
                }
            }
        }
    }
}
