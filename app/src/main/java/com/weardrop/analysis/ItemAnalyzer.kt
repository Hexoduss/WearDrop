package com.weardrop.analysis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.weardrop.data.Category
import com.weardrop.data.NamedColor
import com.weardrop.data.Season
import com.weardrop.data.Style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** What the app could work out from a photo. Every field can be corrected by the user. */
data class ItemGuess(
    val category: Category = Category.OTHER,
    val colors: List<NamedColor> = emptyList(),
    val style: Style? = null,
    val season: Season = Season.ALL,
    val formality: Int = 3,
)

/**
 * Analyzes a clothing photo entirely on the device:
 * - main colors, by mapping the pixels of the central area to the named palette;
 * - item type, with ML Kit's bundled image labeling model (free, offline).
 */
class ItemAnalyzer {

    private val labeler by lazy { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }

    suspend fun analyze(file: File): ItemGuess = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext ItemGuess()
        try {
            val colors = runCatching { dominantColors(bitmap) }.getOrDefault(emptyList())
            val labels = runCatching { label(bitmap) }.getOrDefault(emptyList())
            CategoryGuesser.guess(labels).copy(colors = colors)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun label(bitmap: Bitmap): List<Pair<String, Float>> =
        suspendCancellableCoroutine { continuation ->
            labeler.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { labels ->
                    if (continuation.isActive) continuation.resume(labels.map { it.text to it.confidence })
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }

    /**
     * Counts palette colors in the center of the photo. The background color (guessed
     * from the borders) is ignored when there is enough of something else.
     */
    private fun dominantColors(bitmap: Bitmap): List<NamedColor> {
        val size = SAMPLE_SIZE
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== bitmap) scaled.recycle()

        val palette = NamedColor.entries
        val border = IntArray(palette.size)
        val center = IntArray(palette.size)
        val margin = size / 10
        val centerStart = size / 5
        val centerEnd = size - centerStart
        var borderTotal = 0
        var centerTotal = 0

        for (y in 0 until size) {
            for (x in 0 until size) {
                val isBorder = x < margin || y < margin || x >= size - margin || y >= size - margin
                val isCenter = x in centerStart until centerEnd && y in centerStart until centerEnd
                if (!isBorder && !isCenter) continue
                val index = NamedColor.nearest(pixels[y * size + x]).ordinal
                if (isBorder) {
                    border[index]++
                    borderTotal++
                }
                if (isCenter) {
                    center[index]++
                    centerTotal++
                }
            }
        }

        val background = border.indices.maxByOrNull { border[it] } ?: 0
        val backgroundIsUniform = borderTotal > 0 && border[background] > borderTotal * 0.45
        val withoutBackground = centerTotal - center[background]
        if (backgroundIsUniform && withoutBackground >= centerTotal * 0.25) center[background] = 0

        val total = center.sum().coerceAtLeast(1)
        val ranked = center.indices
            .filter { center[it] >= total * 0.15 }
            .sortedByDescending { center[it] }
            .take(3)
            .map { palette[it] }
        return ranked.ifEmpty { listOf(palette[center.indices.maxByOrNull { center[it] } ?: 0]) }
    }

    private companion object {
        const val SAMPLE_SIZE = 80
    }
}

/** Turns ML Kit's generic labels into a clothing category plus a few reasonable defaults. */
internal object CategoryGuesser {

    private data class Rule(
        val pattern: Regex,
        val category: Category,
        val formality: Int = 3,
        val style: Style? = null,
        val season: Season = Season.ALL,
    )

    private fun words(vararg w: String) = Regex("\\b(" + w.joinToString("|") + ")\\b")

    // Order matters: more specific words first.
    private val rules = listOf(
        Rule(words("high heels?", "heels?", "pumps?"), Category.SHOES, 4, Style.ELEGANT),
        Rule(words("sneakers?", "trainers?", "running shoes?"), Category.SHOES, 2, Style.CASUAL),
        Rule(words("boots?"), Category.SHOES, 3, season = Season.COLD),
        Rule(words("sandals?", "flip-flops?"), Category.SHOES, 2, Style.CASUAL, Season.WARM),
        Rule(words("shoes?", "footwear", "loafers?"), Category.SHOES, 3),
        Rule(words("blazer", "suit"), Category.OUTERWEAR, 4, Style.CLASSIC),
        Rule(words("coat", "overcoat", "parka"), Category.OUTERWEAR, 3, season = Season.COLD),
        Rule(words("jacket", "outerwear", "raincoat"), Category.OUTERWEAR, 3),
        Rule(words("dress", "gown"), Category.DRESS, 3),
        Rule(words("skirt", "miniskirt"), Category.SKIRT, 3),
        Rule(words("shorts"), Category.PANTS, 1, Style.CASUAL, Season.WARM),
        Rule(words("jeans"), Category.PANTS, 2, Style.CASUAL),
        Rule(words("trousers", "pants", "leggings"), Category.PANTS, 3),
        Rule(words("hoodie", "sweatshirt"), Category.TOP, 2, Style.SPORTY, Season.COLD),
        Rule(words("sweater", "jumper", "cardigan", "knitwear"), Category.TOP, 3, season = Season.COLD),
        Rule(words("t-shirt", "tshirt", "tee"), Category.TOP, 2, Style.CASUAL),
        Rule(words("shirt", "blouse", "polo", "jersey", "top", "tank top", "vest"), Category.TOP, 3),
        Rule(
            words(
                "bag", "handbag", "backpack", "hat", "cap", "scarf", "sunglasses", "tie",
                "belt", "watch", "jewelry", "jewellery", "necklace", "bracelet", "gloves?",
            ),
            Category.ACCESSORY,
            3,
        ),
    )

    fun guess(labels: List<Pair<String, Float>>): ItemGuess {
        for ((text, _) in labels.sortedByDescending { it.second }) {
            val lower = text.lowercase()
            val rule = rules.firstOrNull { it.pattern.containsMatchIn(lower) } ?: continue
            return ItemGuess(
                category = rule.category,
                style = rule.style,
                season = rule.season,
                formality = rule.formality,
            )
        }
        return ItemGuess()
    }
}
