package com.armadioai.ui.wardrobe

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armadioai.data.Category
import com.armadioai.data.ClothingItem
import com.armadioai.data.ImageStore
import com.armadioai.data.ItemStatus
import com.armadioai.ui.components.EmptyState
import com.armadioai.ui.components.ItemImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(
    onOpenItem: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    vm: WardrobeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val hasApiKey by vm.hasApiKey.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris -> if (uris.isNotEmpty()) vm.addPhotos(uris) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) vm.addPhotos(listOf(Uri.parse(uri)))
        pendingCameraUri = null
    }

    val openGallery: () -> Unit = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val openCamera: () -> Unit = {
        val uri = ImageStore.newCameraUri(context)
        pendingCameraUri = uri.toString()
        try {
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingCameraUri = null
            Toast.makeText(context, "Nessuna app fotocamera trovata", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Il mio armadio")
                        if (state.all.isNotEmpty()) {
                            Text(
                                if (state.all.size == 1) "1 capo" else "${state.all.size} capi",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.all.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Aggiungi") },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (!hasApiKey) ApiKeyBanner(onOpenSettings)

            when {
                state.loading -> Unit
                state.all.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Checkroom,
                    title = "Il tuo armadio è vuoto",
                    text = "Fotografa i tuoi capi uno alla volta, su uno sfondo semplice. " +
                        "L'AI li riconosce e li cataloga da sola.",
                ) {
                    Button(onClick = openCamera, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scatta una foto")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = openGallery, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scegli dalla galleria")
                    }
                }

                else -> {
                    CategoryFilters(state, onSelect = vm::setFilter)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.visible, key = { it.id }) { item ->
                            WardrobeTile(item, onClick = { onOpenItem(item.id) })
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Text(
                "Aggiungi capi",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text("Scatta una foto") },
                supportingContent = { Text("Un capo per foto, ben illuminato") },
                leadingContent = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    showAddSheet = false
                    openCamera()
                },
            )
            ListItem(
                headlineContent = { Text("Scegli dalla galleria") },
                supportingContent = { Text("Fino a 20 foto alla volta") },
                leadingContent = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    showAddSheet = false
                    openGallery()
                },
            )
            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(16.dp),
            )
        }
    }
}

@Composable
private fun ApiKeyBanner(onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Key, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                "Per far funzionare l'AI inserisci la tua chiave API.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenSettings) { Text("Imposta") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilters(state: WardrobeState, onSelect: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = state.filter == null,
                onClick = { onSelect(null) },
                label = { Text("Tutti") },
            )
        }
        items(state.categoryCounts) { (category, count) ->
            FilterChip(
                selected = state.filter == category.key,
                onClick = { onSelect(if (state.filter == category.key) null else category.key) },
                label = { Text("${category.label} · $count") },
            )
        }
    }
}

@Composable
private fun WardrobeTile(item: ClothingItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(shape),
        ) {
            ItemImage(item.imagePath, item.name, Modifier.fillMaxSize())
            when (item.status) {
                ItemStatus.ANALYZING -> TileOverlay {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Analisi in corso…", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }

                ItemStatus.ERROR -> TileOverlay {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Analisi non riuscita", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.name.ifBlank { "Nuovo capo" },
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (item.status == ItemStatus.READY) {
            Text(
                listOf(Category.fromKey(item.category).label, item.colors)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun TileOverlay(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}
