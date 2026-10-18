package com.armadioai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object ItemStatus {
    const val ANALYZING = "analyzing"
    const val READY = "ready"
    const val ERROR = "error"
}

@Entity(tableName = "items")
data class ClothingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String,
    val name: String = "",
    val category: String = Category.ALTRO.key,
    /** Colori separati da virgola, dal più presente. */
    val colors: String = "",
    val pattern: String = "",
    val style: String = "",
    val season: String = Season.TUTTE.key,
    /** 1 = molto informale … 5 = formale */
    val formality: Int = 3,
    val description: String = "",
    val status: String = ItemStatus.ANALYZING,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "saved_outfits")
data class SavedOutfit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Id dei capi separati da virgola. */
    val itemIds: String,
    val why: String = "",
    val tip: String = "",
    val occasion: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "wear_log")
data class WearLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val itemIds: String,
    val wornAt: Long = System.currentTimeMillis(),
)

fun String.toIdList(): List<Long> = split(',').mapNotNull { it.trim().toLongOrNull() }

fun List<Long>.toIdString(): String = joinToString(",")

enum class Category(val key: String, val label: String) {
    TOP("top", "Top"),
    PANTALONI("pantaloni", "Pantaloni"),
    GONNA("gonna", "Gonne"),
    ABITO("abito", "Abiti"),
    CAPOSPALLA("capospalla", "Capispalla"),
    SCARPE("scarpe", "Scarpe"),
    ACCESSORI("accessori", "Accessori"),
    ALTRO("altro", "Altro");

    companion object {
        fun fromKey(key: String): Category =
            entries.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) } ?: ALTRO
    }
}

enum class Season(val key: String, val label: String) {
    PRIMAVERA_ESTATE("primavera-estate", "Primavera/Estate"),
    AUTUNNO_INVERNO("autunno-inverno", "Autunno/Inverno"),
    MEZZA("mezza stagione", "Mezza stagione"),
    TUTTE("tutte", "Tutto l'anno");

    companion object {
        fun fromKey(key: String): Season =
            entries.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) } ?: TUTTE
    }
}

fun formalityLabel(level: Int): String = when (level) {
    1 -> "Molto informale"
    2 -> "Casual"
    3 -> "Smart casual"
    4 -> "Elegante"
    else -> "Formale"
}
