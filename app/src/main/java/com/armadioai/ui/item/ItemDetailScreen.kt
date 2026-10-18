package com.armadioai.ui.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armadioai.data.Category
import com.armadioai.data.ItemStatus
import com.armadioai.data.Season
import com.armadioai.data.formalityLabel
import com.armadioai.ui.components.ItemImage
import com.armadioai.ui.components.SectionLabel
import com.armadioai.ui.components.formatDay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    onBack: () -> Unit,
    onCreateOutfit: () -> Unit,
    vm: ItemDetailViewModel = viewModel(),
) {
    val item by vm.item.collectAsStateWithLifecycle()
    val stats by vm.wearStats.collectAsStateWithLifecycle()
    val draft = vm.draft
    var confirmDelete by remember { mutableStateOf(false) }

    val isDirty = draft != null && item != null && draft != item
    val analyzing = draft?.status == ItemStatus.ANALYZING
    val ready = draft?.status == ItemStatus.READY

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                title = {
                    Text(
                        draft?.name?.ifBlank { "Capo" } ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Rimuovi")
                    }
                },
            )
        },
        bottomBar = {
            if (isDirty) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = vm::save,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .fillMaxWidth()
                            .height(52.dp),
                    ) { Text("Salva modifiche") }
                }
            }
        },
    ) { padding ->
        if (draft == null) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ItemImage(
                path = draft.imagePath,
                contentDescription = draft.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(24.dp)),
            )

            when (draft.status) {
                ItemStatus.ANALYZING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Sto analizzando il capo…", style = MaterialTheme.typography.bodyMedium)
                }

                ItemStatus.ERROR -> Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "L'analisi non è riuscita. Puoi riprovare o compilare i campi a mano.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = vm::retryAnalysis) { Text("Riprova") }
                    }
                }

                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            stats.count == 0 -> "Non ancora indossato"
                            stats.count == 1 -> "Indossato 1 volta · ${formatDay(stats.lastWorn ?: 0)}"
                            else -> "Indossato ${stats.count} volte · l'ultima ${formatDay(stats.lastWorn ?: 0)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (ready) {
                FilledTonalButton(
                    onClick = {
                        vm.useAsStylistAnchor()
                        onCreateOutfit()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crea un outfit con questo capo")
                }
            }

            val editable = !analyzing

            OutlinedTextField(
                value = draft.name,
                onValueChange = { value -> vm.edit { it.copy(name = value) } },
                label = { Text("Nome") },
                singleLine = true,
                enabled = editable,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                SectionLabel("Categoria")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { category ->
                        FilterChip(
                            selected = draft.category == category.key,
                            onClick = { vm.edit { it.copy(category = category.key) } },
                            label = { Text(category.label) },
                            enabled = editable,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = draft.colors,
                onValueChange = { value -> vm.edit { it.copy(colors = value) } },
                label = { Text("Colori") },
                supportingText = { Text("Separati da virgola, dal più presente") },
                singleLine = true,
                enabled = editable,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.pattern,
                    onValueChange = { value -> vm.edit { it.copy(pattern = value) } },
                    label = { Text("Fantasia") },
                    singleLine = true,
                    enabled = editable,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.style,
                    onValueChange = { value -> vm.edit { it.copy(style = value) } },
                    label = { Text("Stile") },
                    singleLine = true,
                    enabled = editable,
                    modifier = Modifier.weight(1f),
                )
            }

            Column {
                SectionLabel("Stagione")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Season.entries.forEach { season ->
                        FilterChip(
                            selected = draft.season == season.key,
                            onClick = { vm.edit { it.copy(season = season.key) } },
                            label = { Text(season.label) },
                            enabled = editable,
                        )
                    }
                }
            }

            Column {
                SectionLabel("Formalità: ${formalityLabel(draft.formality)}")
                Slider(
                    value = draft.formality.toFloat(),
                    onValueChange = { value -> vm.edit { it.copy(formality = value.roundToInt().coerceIn(1, 5)) } },
                    valueRange = 1f..5f,
                    steps = 3,
                    enabled = editable,
                )
            }

            OutlinedTextField(
                value = draft.description,
                onValueChange = { value -> vm.edit { it.copy(description = value) } },
                label = { Text("Descrizione") },
                minLines = 3,
                enabled = editable,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Rimuovere questo capo?") },
            text = { Text("Verrà tolto dall'armadio insieme alla sua foto.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onDone = onBack)
                }) { Text("Rimuovi") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annulla") }
            },
        )
    }
}
