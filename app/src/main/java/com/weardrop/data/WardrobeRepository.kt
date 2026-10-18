package com.weardrop.data

import android.content.Context
import android.net.Uri
import com.weardrop.R
import com.weardrop.analysis.ItemAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Adds, analyzes and removes items. Analyses run in an app-wide scope,
 * so they keep going if the user switches screen.
 */
class WardrobeRepository(
    private val context: Context,
    private val dao: WardrobeDao,
    private val analyzer: ItemAnalyzer,
    private val scope: CoroutineScope,
    private val notify: (Int) -> Unit,
) {
    val items: Flow<List<ClothingItem>> = dao.observeItems()

    fun observeItem(id: Long): Flow<ClothingItem?> = dao.observeItem(id)

    fun addPhotos(uris: List<Uri>) {
        for (uri in uris) scope.launch { addOne(uri) }
    }

    suspend fun delete(id: Long) {
        val item = dao.getItem(id) ?: return
        dao.deleteItem(id)
        withContext(Dispatchers.IO) { File(item.imagePath).delete() }
    }

    private suspend fun addOne(uri: Uri) {
        val file = try {
            withContext(Dispatchers.IO) { ImageStore.importPhoto(context, uri) }
        } catch (e: Exception) {
            notify(R.string.photo_read_error)
            return
        } finally {
            if (ImageStore.isCameraUri(context, uri)) {
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }

        val id = dao.insertItem(ClothingItem(imagePath = file.absolutePath, status = ItemStatus.ANALYZING))
        val guess = analyzer.analyze(file)
        val current = dao.getItem(id) ?: return // removed in the meantime
        dao.updateItem(
            current.copy(
                category = guess.category.key,
                colors = guess.colors.joinToString(",") { it.key },
                style = guess.style?.key.orEmpty(),
                season = guess.season.key,
                formality = guess.formality,
                status = ItemStatus.READY,
            ),
        )
    }
}
