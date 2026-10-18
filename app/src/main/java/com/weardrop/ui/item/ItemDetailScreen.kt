package com.weardrop.ui.item

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weardrop.R
import com.weardrop.data.Category
import com.weardrop.data.ItemStatus
import com.weardrop.data.NamedColor
import com.weardrop.data.Pattern
import com.weardrop.data.Season
import com.weardrop.data.Style
import com.weardrop.data.colorList
import com.weardrop.data.formalityLabel
import com.weardrop.ui.components.ColorDot
import com.weardrop.ui.components.ItemImage
import com.weardrop.ui.components.SectionLabel
import com.weardrop.ui.components.colorName
import com.weardrop.ui.components.formatDay
import com.weardrop.ui.components.itemDisplayName
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
    val showSaveBar = isDirty || (draft != null && !analyzing && !draft.reviewed)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = {
                    Text(
                        if (draft != null) itemDisplayName(draft) else "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.item_remove))
                    }
                },
            )
        },
        bottomBar = {
            if (showSaveBar) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = vm::save,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .fillMaxWidth()
                            .height(52.dp),
                    ) { Text(stringResource(R.string.item_save)) }
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
                contentDescription = itemDisplayName(draft),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(24.dp)),
            )

            if (analyzing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.item_analyzing), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                if (!draft.reviewed) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text(
                            stringResource(R.string.item_review_banner),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (stats.count == 0) {
                            stringResource(R.string.worn_never)
                        } else {
                            pluralStringResource(R.plurals.worn_count, stats.count, stats.count, formatDay(stats.lastWorn ?: 0))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = {
                        vm.useAsStylistAnchor()
                        onCreateOutfit()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.item_create_outfit))
                }
            }

            val editable = !analyzing
            val automaticName = itemDisplayName(draft.copy(name = ""))

            OutlinedTextField(
                value = draft.name,
                onValueChange = { value -> vm.edit { it.copy(name = value) } },
                label = { Text(stringResource(R.string.item_name)) },
                placeholder = { Text(automaticName) },
                supportingText = { Text(stringResource(R.string.item_name_hint, automaticName)) },
                singleLine = true,
                enabled = editable,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                SectionLabel(stringResource(R.string.item_category))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { category ->
                        FilterChip(
                            selected = draft.category == category.key,
                            onClick = { vm.edit { it.copy(category = category.key) } },
                            label = { Text(stringResource(category.label)) },
                            enabled = editable,
                        )
                    }
                }
            }

            Column {
                SectionLabel(stringResource(R.string.item_colors))
                val selected = draft.colorList()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NamedColor.entries.forEach { color ->
                        val isSelected = color in selected
                        FilterChip(
                            selected = isSelected,
                            onClick = { vm.toggleColor(color) },
                            label = { Text(colorName(color)) },
                            leadingIcon = { ColorDot(color) },
                            enabled = editable && (isSelected || selected.size < 3),
                        )
                    }
                }
            }

            Column {
                SectionLabel(stringResource(R.string.item_pattern))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pattern.entries.forEach { pattern ->
                        FilterChip(
                            selected = draft.pattern == pattern.key,
                            onClick = { vm.edit { it.copy(pattern = pattern.key) } },
                            label = { Text(stringResource(pattern.label)) },
                            enabled = editable,
                        )
                    }
                }
            }

            Column {
                SectionLabel(stringResource(R.string.item_style))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Style.entries.forEach { style ->
                        FilterChip(
                            selected = draft.style == style.key,
                            onClick = {
                                vm.edit { it.copy(style = if (it.style == style.key) "" else style.key) }
                            },
                            label = { Text(stringResource(style.label)) },
                            enabled = editable,
                        )
                    }
                }
            }

            Column {
                SectionLabel(stringResource(R.string.item_season))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Season.entries.forEach { season ->
                        FilterChip(
                            selected = draft.season == season.key,
                            onClick = { vm.edit { it.copy(season = season.key) } },
                            label = { Text(stringResource(season.label)) },
                            enabled = editable,
                        )
                    }
                }
            }

            Column {
                SectionLabel(stringResource(R.string.item_formality, stringResource(formalityLabel(draft.formality))))
                Slider(
                    value = draft.formality.toFloat(),
                    onValueChange = { value -> vm.edit { it.copy(formality = value.roundToInt().coerceIn(1, 5)) } },
                    valueRange = 1f..5f,
                    steps = 3,
                    enabled = editable,
                )
            }

            OutlinedTextField(
                value = draft.notes,
                onValueChange = { value -> vm.edit { it.copy(notes = value) } },
                label = { Text(stringResource(R.string.item_notes)) },
                minLines = 2,
                enabled = editable,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_item_title)) },
            text = { Text(stringResource(R.string.delete_item_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onDone = onBack)
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
