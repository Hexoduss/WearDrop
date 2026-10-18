package com.weardrop.stylist

import androidx.annotation.StringRes
import com.weardrop.R
import com.weardrop.data.ClothingItem
import com.weardrop.data.Style

enum class Occasion(val key: String, @StringRes val label: Int, val formality: Double) {
    CASUAL("casual", R.string.occ_casual, 2.0),
    WORK("work", R.string.occ_work, 3.5),
    SCHOOL("school", R.string.occ_school, 2.2),
    DRINKS("drinks", R.string.occ_drinks, 3.0),
    DINNER("dinner", R.string.occ_dinner, 3.5),
    NIGHT("night", R.string.occ_night, 3.0),
    DATE("date", R.string.occ_date, 3.4),
    SPORT("sport", R.string.occ_sport, 1.0),
    CEREMONY("ceremony", R.string.occ_ceremony, 4.8),
    TRAVEL("travel", R.string.occ_travel, 2.0);

    /** How well a style suits this occasion (positive = good fit). */
    fun styleAffinity(style: Style?): Double = when (this) {
        SPORT -> when (style) {
            Style.SPORTY -> 1.0
            Style.ELEGANT -> -2.0
            Style.CLASSIC -> -1.0
            else -> 0.0
        }
        WORK -> when (style) {
            Style.CLASSIC -> 0.7
            Style.ELEGANT, Style.MINIMAL -> 0.5
            Style.SPORTY -> -1.5
            Style.STREETWEAR -> -0.7
            else -> 0.0
        }
        CEREMONY -> when (style) {
            Style.ELEGANT -> 1.0
            Style.CLASSIC -> 0.7
            Style.SPORTY -> -3.0
            Style.STREETWEAR -> -2.0
            Style.CASUAL -> -1.0
            else -> 0.0
        }
        DINNER, DATE -> when (style) {
            Style.ELEGANT -> 0.6
            Style.CLASSIC, Style.MINIMAL -> 0.4
            Style.SPORTY -> -1.5
            else -> 0.0
        }
        NIGHT, DRINKS -> when (style) {
            Style.ELEGANT, Style.STREETWEAR, Style.MINIMAL -> 0.3
            Style.SPORTY -> -1.0
            else -> 0.0
        }
        CASUAL, SCHOOL, TRAVEL -> when (style) {
            Style.CASUAL, Style.STREETWEAR -> 0.4
            Style.ELEGANT -> -0.4
            else -> 0.0
        }
    }

    companion object {
        fun fromKey(key: String): Occasion? = entries.firstOrNull { it.key == key }
    }
}

enum class OutfitTitle(@StringRes val label: Int) {
    SHARP(R.string.title_sharp),
    PATTERN(R.string.title_pattern),
    TONAL(R.string.title_tonal),
    CONTRAST(R.string.title_contrast),
    POP(R.string.title_pop),
    HARMONY(R.string.title_harmony),
    LAYERED(R.string.title_layered),
    EASY(R.string.title_easy),
    NEUTRAL(R.string.title_neutral),
    READY(R.string.title_ready);

    companion object {
        fun fromKey(key: String): OutfitTitle = entries.firstOrNull { it.name == key } ?: READY
    }
}

enum class Reason(@StringRes val label: Int) {
    ANCHOR(R.string.r_anchor),
    NEUTRALS(R.string.r_neutrals),
    POP(R.string.r_pop),
    ANALOGOUS(R.string.r_analogous),
    COMPLEMENTARY(R.string.r_complementary),
    CONTRAST(R.string.r_contrast),
    TONAL(R.string.r_tonal),
    ONE_PATTERN(R.string.r_one_pattern),
    FORMALITY(R.string.r_formality),
    WARM_LAYER(R.string.r_warm_layer),
    LIGHT(R.string.r_light),
    RAIN(R.string.r_rain),
    FRESH(R.string.r_fresh),
    NO_SHOES(R.string.r_no_shoes);

    companion object {
        fun parseList(keys: String): List<Reason> =
            keys.split(',').mapNotNull { key -> entries.firstOrNull { it.name == key.trim() } }
    }
}

enum class Tip(@StringRes val label: Int) {
    RAIN(R.string.tip_rain),
    COLD(R.string.tip_cold),
    HOT(R.string.tip_hot),
    CEREMONY(R.string.tip_ceremony),
    WORK(R.string.tip_work),
    SPORT(R.string.tip_sport),
    PATTERN(R.string.tip_pattern),
    NEUTRAL(R.string.tip_neutral),
    ROLL(R.string.tip_roll),
    FIT(R.string.tip_fit);

    companion object {
        fun fromKey(key: String): Tip? = entries.firstOrNull { it.name == key }
    }
}

/** Weather as seen by the outfit rules. */
data class WeatherContext(val temperature: Int, val rainy: Boolean)

data class OutfitRequest(
    val items: List<ClothingItem>,
    val occasion: Occasion,
    val weather: WeatherContext?,
    val anchor: ClothingItem?,
    val recentlyWorn: Set<Long>,
    /** 1–12, used to guess the season when there is no weather. */
    val month: Int,
)

data class OutfitSuggestion(
    val pieces: List<ClothingItem>,
    val title: OutfitTitle,
    val reasons: List<Reason>,
    val tip: Tip,
)
