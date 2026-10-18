package com.armadioai.ui.stylist

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armadioai.ui.components.ItemImage
import com.armadioai.ui.components.OutfitCard
import com.armadioai.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StylistScreen(
    onOpenSettings: () -> Unit,
    vm: StylistViewModel = viewModel(),
) {
    val anchor by vm.anchor.collectAsStateWithLifecycle()
    val readyCount by vm.readyCount.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Stylist") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Cosa indosso oggi?", style = MaterialTheme.typography.headlineMedium)
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
                    SectionLabel("Occasione")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OCCASIONS.forEach { option ->
                            FilterChip(
                                selected = vm.occasion == option,
                                onClick = { vm.occasion = option },
                                label = { Text(option) },
                            )
                        }
                    }
                }
            }

            anchor?.let { piece ->
                item {
                    Column {
                        SectionLabel("Parti da questo capo")
                        InputChip(
                            selected = true,
                            onClick = vm::clearAnchor,
                            label = { Text(piece.name) },
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
                                Icon(Icons.Outlined.Close, contentDescription = "Togli", modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = vm.notes,
                    onValueChange = { vm.notes = it },
                    label = { Text("Altro da sapere? (facoltativo)") },
                    placeholder = { Text("Es. cammino molto, voglio un tocco di colore") },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                        Spacer(Modifier.width(12.dp))
                        Text("Lo stylist sta pensando…")
                    } else {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (vm.results.isEmpty()) "Crea 3 outfit" else "Proponimene altri 3")
                    }
                }
                if (readyCount in 1..2) {
                    Text(
                        "Hai $readyCount capi analizzati: aggiungine altri per proposte migliori.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            vm.error?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            itemsIndexed(vm.results) { index, result ->
                OutfitCard(
                    title = result.idea.title,
                    why = result.idea.why,
                    tip = result.idea.tip,
                    pieces = result.pieces,
                ) {
                    FilledTonalButton(onClick = { vm.save(index) }, enabled = !result.saved) {
                        Icon(
                            if (result.saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (result.saved) "Salvato" else "Salva")
                    }
                    OutlinedButton(onClick = { vm.wearToday(index) }, enabled = !result.worn) {
                        Icon(
                            if (result.worn) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (result.worn) "Indossato" else "Lo metto oggi")
                    }
                }
            }
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
                    Text("Controllo il meteo…", style = MaterialTheme.typography.bodyMedium)
                }

                WeatherState.NoCity -> {
                    Text(
                        "Imposta la tua città per avere outfit adatti al meteo.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onOpenSettings) { Text("Imposta") }
                }

                is WeatherState.Failed -> {
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRefresh) { Text("Riprova") }
                }

                is WeatherState.Ready -> {
                    val w = state.weather
                    Text(w.emoji, fontSize = 34.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${w.temperature}° · ${w.description}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            buildString {
                                append("${w.place} · min ${w.min}° max ${w.max}°")
                                w.rainChance?.let { append(" · pioggia $it%") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Aggiorna meteo")
                    }
                }
            }
        }
    }
}
