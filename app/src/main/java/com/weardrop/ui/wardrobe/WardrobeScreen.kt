package com.weardrop.ui.wardrobe

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
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weardrop.R
import com.weardrop.data.ClothingItem
import com.weardrop.data.ImageStore
import com.weardrop.data.ItemStatus
import com.weardrop.data.categoryEnum
import com.weardrop.data.colorList
import com.weardrop.ui.components.ColorDot
import com.weardrop.ui.components.EmptyState
import com.weardrop.ui.components.ItemImage
import com.weardrop.ui.components.itemDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(
    onOpenItem: (Long) -> Unit,
    vm: WardrobeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noCameraMessage = stringResource(R.string.no_camera_app)

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
            Toast.makeText(context, noCameraMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.wardrobe_title))
                        if (state.all.isNotEmpty()) {
                            Text(
                                pluralStringResource(R.plurals.item_count, state.all.size, state.all.size),
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
                    text = { Text(stringResource(R.string.wardrobe_add)) },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> Unit
                state.all.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Checkroom,
                    title = stringResource(R.string.wardrobe_empty_title),
                    text = stringResource(R.string.wardrobe_empty_text),
                ) {
                    Button(onClick = openCamera, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_take_photo))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = openGallery, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_from_gallery))
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
                stringResource(R.string.add_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.add_take_photo)) },
                supportingContent = { Text(stringResource(R.string.add_take_photo_hint)) },
                leadingContent = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    showAddSheet = false
                    openCamera()
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.add_from_gallery)) },
                supportingContent = { Text(stringResource(R.string.add_from_gallery_hint)) },
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
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        items(state.categoryCounts) { (category, count) ->
            FilterChip(
                selected = state.filter == category.key,
                onClick = { onSelect(if (state.filter == category.key) null else category.key) },
                label = { Text("${stringResource(category.label)} · $count") },
            )
        }
    }
}

@Composable
private fun WardrobeTile(item: ClothingItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val name = itemDisplayName(item)
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
            ItemImage(item.imagePath, name, Modifier.fillMaxSize())
            if (item.status == ItemStatus.ANALYZING) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.tile_analyzing),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            } else if (!item.reviewed) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    Text(
                        stringResource(R.string.tile_review),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (item.status == ItemStatus.READY) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                item.colorList().forEach { ColorDot(it, size = 12.dp) }
                if (item.colorList().isNotEmpty()) Spacer(Modifier.width(2.dp))
                Text(
                    stringResource(item.categoryEnum().label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
