package com.weardrop.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.weardrop.R

object ItemStatus {
    const val ANALYZING = "analyzing"
    const val READY = "ready"
}

@Entity(tableName = "items")
data class ClothingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String,
    /** Custom name; when empty the UI builds one from color and category. */
    val name: String = "",
    val category: String = Category.OTHER.key,
    /** Color keys (see [NamedColor]) separated by commas, main color first. */
    val colors: String = "",
    val pattern: String = Pattern.SOLID.key,
    /** Style key (see [Style]) or empty. */
    val style: String = "",
    val season: String = Season.ALL.key,
    /** 1 = very casual … 5 = formal */
    val formality: Int = 3,
    val notes: String = "",
    val status: String = ItemStatus.ANALYZING,
    /** False until the user has checked the automatic guesses. */
    val reviewed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

fun ClothingItem.colorList(): List<NamedColor> =
    colors.split(',').mapNotNull { NamedColor.fromKey(it) }

fun ClothingItem.categoryEnum(): Category = Category.fromKey(category)
fun ClothingItem.patternEnum(): Pattern = Pattern.fromKey(pattern)
fun ClothingItem.styleEnum(): Style? = Style.fromKey(style)
fun ClothingItem.seasonEnum(): Season = Season.fromKey(season)

@Entity(tableName = "saved_outfits")
data class SavedOutfit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Item ids separated by commas. */
    val itemIds: String,
    /** Keys of [com.weardrop.stylist.OutfitTitle], [com.weardrop.stylist.Reason] and [com.weardrop.stylist.Tip]. */
    val title: String,
    val reasons: String = "",
    val tip: String = "",
    val occasion: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "wear_log")
data class WearLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemIds: String,
    val title: String,
    val wornAt: Long = System.currentTimeMillis(),
)

fun String.toIdList(): List<Long> = split(',').mapNotNull { it.trim().toLongOrNull() }

fun List<Long>.toIdString(): String = joinToString(",")

enum class Category(val key: String, @StringRes val label: Int, @StringRes val singular: Int) {
    TOP("top", R.string.cat_top, R.string.cat_top_one),
    PANTS("pants", R.string.cat_pants, R.string.cat_pants_one),
    SKIRT("skirt", R.string.cat_skirt, R.string.cat_skirt_one),
    DRESS("dress", R.string.cat_dress, R.string.cat_dress_one),
    OUTERWEAR("outerwear", R.string.cat_outerwear, R.string.cat_outerwear_one),
    SHOES("shoes", R.string.cat_shoes, R.string.cat_shoes_one),
    ACCESSORY("accessory", R.string.cat_accessory, R.string.cat_accessory_one),
    OTHER("other", R.string.cat_other, R.string.cat_other_one);

    val isBottom: Boolean get() = this == PANTS || this == SKIRT

    companion object {
        fun fromKey(key: String): Category = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

enum class Pattern(val key: String, @StringRes val label: Int) {
    SOLID("solid", R.string.pattern_solid),
    STRIPED("striped", R.string.pattern_striped),
    CHECKED("checked", R.string.pattern_checked),
    FLORAL("floral", R.string.pattern_floral),
    PRINT("print", R.string.pattern_print);

    companion object {
        fun fromKey(key: String): Pattern = entries.firstOrNull { it.key == key } ?: SOLID
    }
}

/** The 25 styles users can pick as favorites and assign to items. */
enum class Style(val key: String, @StringRes val label: Int) {
    CASUAL("casual", R.string.style_casual),
    SPORTY("sporty", R.string.style_sporty),
    ELEGANT("elegant", R.string.style_elegant),
    CLASSIC("classic", R.string.style_classic),
    STREETWEAR("streetwear", R.string.style_streetwear),
    MINIMAL("minimal", R.string.style_minimal),
    GLAM("glam", R.string.style_glam),
    PUNK("punk", R.string.style_punk),
    GRUNGE("grunge", R.string.style_grunge),
    ROCK("rock", R.string.style_rock),
    GOTHIC("gothic", R.string.style_gothic),
    BOHO("boho", R.string.style_boho),
    PREPPY("preppy", R.string.style_preppy),
    OLD_MONEY("old_money", R.string.style_old_money),
    VINTAGE("vintage", R.string.style_vintage),
    Y2K("y2k", R.string.style_y2k),
    ROMANTIC("romantic", R.string.style_romantic),
    BUSINESS("business", R.string.style_business),
    SMART_CASUAL("smart_casual", R.string.style_smart_casual),
    WESTERN("western", R.string.style_western),
    UTILITY("utility", R.string.style_utility),
    WORKWEAR("workwear", R.string.style_workwear),
    SKATER("skater", R.string.style_skater),
    INDIE("indie", R.string.style_indie),
    TECHWEAR("techwear", R.string.style_techwear);

    companion object {
        fun fromKey(key: String): Style? = entries.firstOrNull { it.key == key }

        fun parseList(keys: String): List<Style> = keys.split(',').mapNotNull { fromKey(it.trim()) }
    }
}

enum class Season(val key: String, @StringRes val label: Int) {
    WARM("warm", R.string.season_warm),
    COLD("cold", R.string.season_cold),
    ALL("all", R.string.season_all);

    companion object {
        fun fromKey(key: String): Season = entries.firstOrNull { it.key == key } ?: ALL
    }
}

@StringRes
fun formalityLabel(level: Int): Int = when (level) {
    1 -> R.string.formality_1
    2 -> R.string.formality_2
    3 -> R.string.formality_3
    4 -> R.string.formality_4
    else -> R.string.formality_5
}
