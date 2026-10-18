package com.armadioai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object ImageStore {

    /**
     * Copia la foto nella memoria privata dell'app: la ridimensiona (lato lungo max [maxSide] px),
     * corregge la rotazione e la salva in JPEG. Foto più leggere = analisi più rapide ed economiche.
     */
    fun importPhoto(context: Context, uri: Uri, maxSide: Int = 1280): File {
        val resolver = context.contentResolver

        // 1) Legge solo le dimensioni, per non caricare in memoria foto da 50 megapixel.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: throw IOException("Foto non leggibile")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Formato immagine non supportato")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

        // 2) Decodifica ridotta.
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decodeStream = resolver.openInputStream(uri) ?: throw IOException("Foto non leggibile")
        val decoded = decodeStream.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Formato immagine non supportato")

        // 3) Scala e ruota secondo i dati EXIF.
        val rotation = readRotation(context, uri)
        val scale = min(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
        val result = if (scale < 1f || rotation != 0) {
            val matrix = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                if (rotation != 0) postRotate(rotation.toFloat())
            }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else {
            decoded
        }

        val dir = File(context.filesDir, "wardrobe").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { result.compress(Bitmap.CompressFormat.JPEG, 85, it) }

        if (result !== decoded) decoded.recycle()
        result.recycle()
        return file
    }

    /** File temporaneo dove la fotocamera salverà lo scatto. */
    fun newCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "scatto_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun isCameraUri(context: Context, uri: Uri): Boolean =
        uri.authority == "${context.packageName}.fileprovider"

    private fun readRotation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
    } catch (e: Exception) {
        0
    }
}
