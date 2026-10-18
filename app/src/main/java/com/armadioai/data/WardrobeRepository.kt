package com.armadioai.data

import android.content.Context
import android.net.Uri
import com.armadioai.ai.ClaudeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Aggiunta, analisi e rimozione dei capi.
 * Le analisi girano in uno scope dell'applicazione: continuano anche se cambi schermata.
 */
class WardrobeRepository(
    private val context: Context,
    private val dao: WardrobeDao,
    private val claude: ClaudeClient,
    private val scope: CoroutineScope,
    private val notify: (String) -> Unit,
) {
    val items: Flow<List<ClothingItem>> = dao.observeItems()

    fun observeItem(id: Long): Flow<ClothingItem?> = dao.observeItem(id)

    fun addPhotos(uris: List<Uri>) {
        for (uri in uris) scope.launch { addOne(uri) }
    }

    fun retryAnalysis(id: Long): Job = scope.launch {
        val item = dao.getItem(id) ?: return@launch
        dao.updateItem(item.copy(status = ItemStatus.ANALYZING))
        analyze(id)
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
            notify("Non riesco a leggere una delle foto")
            return
        } finally {
            if (ImageStore.isCameraUri(context, uri)) {
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
        val id = dao.insertItem(ClothingItem(imagePath = file.absolutePath, status = ItemStatus.ANALYZING))
        analyze(id)
    }

    private suspend fun analyze(id: Long) {
        val item = dao.getItem(id) ?: return
        try {
            val result = claude.analyzeItem(File(item.imagePath))
            val current = dao.getItem(id) ?: return // eliminato nel frattempo
            if (!result.isClothing) {
                dao.updateItem(current.copy(status = ItemStatus.ERROR, name = "Non riconosciuto"))
                notify("Una foto non sembra contenere un capo d'abbigliamento")
                return
            }
            dao.updateItem(
                current.copy(
                    name = result.name.ifBlank { "Capo" },
                    category = Category.fromKey(result.category).key,
                    colors = result.colors.joinToString(", "),
                    pattern = result.pattern,
                    style = result.style,
                    season = Season.fromKey(result.season).key,
                    formality = result.formality.coerceIn(1, 5),
                    description = result.description,
                    status = ItemStatus.READY,
                ),
            )
        } catch (e: Exception) {
            dao.getItem(id)?.let { dao.updateItem(it.copy(status = ItemStatus.ERROR)) }
            notify(e.message ?: "Analisi non riuscita")
        }
    }
}
