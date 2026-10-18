package com.weardrop.stylist

import com.weardrop.data.Category
import com.weardrop.data.ClothingItem
import com.weardrop.data.ItemStatus
import com.weardrop.data.NamedColor
import com.weardrop.data.Pattern
import com.weardrop.data.Season
import com.weardrop.data.Style
import com.weardrop.data.categoryEnum
import com.weardrop.data.colorList
import com.weardrop.data.patternEnum
import com.weardrop.data.seasonEnum
import com.weardrop.data.styleEnum
import kotlin.math.abs
import kotlin.random.Random

/**
 * Rule-based stylist that runs entirely on the device.
 *
 * It builds every top + bottom (or dress) combination, completes each with the best
 * shoes, a layer when the weather calls for it and possibly an accessory, then scores
 * the outfit on color harmony, patterns, formality for the occasion, style, season
 * versus weather and recently worn pieces. A little randomness keeps suggestions varied.
 */
object OutfitEngine {

    private enum class Climate { HOT, WARM, MILD, COOL, COLD }

    private class Evaluation(
        val score: Double,
        val reasons: List<Reason>,
        val titles: List<OutfitTitle>,
        val accentCount: Int,
        val patterned: Int,
        val matchedStyle: Style?,
    )

    private class Candidate(val pieces: List<ClothingItem>, val evaluation: Evaluation, val total: Double)

    private const val MAX_BASES = 600

    fun hasEnoughItems(items: List<ClothingItem>): Boolean {
        val usable = usable(items)
        val hasTop = usable.any { it.categoryEnum() == Category.TOP }
        val hasBottom = usable.any { it.categoryEnum().isBottom }
        val hasDress = usable.any { it.categoryEnum() == Category.DRESS }
        return (hasTop && hasBottom) || hasDress
    }

    fun suggest(request: OutfitRequest, random: Random = Random.Default, count: Int = 3): List<OutfitSuggestion> {
        val usable = usable(request.items)
        val tops = usable.filter { it.categoryEnum() == Category.TOP }
        val bottoms = usable.filter { it.categoryEnum().isBottom }
        val dresses = usable.filter { it.categoryEnum() == Category.DRESS }
        val layers = usable.filter { it.categoryEnum() == Category.OUTERWEAR }
        val shoes = usable.filter { it.categoryEnum() == Category.SHOES }
        val accessories = usable.filter { it.categoryEnum() == Category.ACCESSORY }

        val climate = climateOf(request)
        val anchor = request.anchor?.let { a -> usable.firstOrNull { it.id == a.id } }
        val anchorCategory = anchor?.categoryEnum()

        var bases: List<List<ClothingItem>> =
            tops.flatMap { top -> bottoms.map { bottom -> listOf(top, bottom) } } + dresses.map { listOf(it) }
        if (anchor != null && (anchorCategory == Category.TOP || anchorCategory == Category.DRESS || anchorCategory?.isBottom == true)) {
            bases = bases.filter { base -> base.any { it.id == anchor.id } }
        }
        if (bases.isEmpty()) return emptyList()
        if (bases.size > MAX_BASES) bases = bases.shuffled(random).take(MAX_BASES)

        val wantsLayer = climate == Climate.COOL || climate == Climate.COLD ||
            (request.weather?.rainy == true && climate != Climate.HOT) ||
            (request.occasion.formality >= 4.5 && climate != Climate.HOT)

        val anchorShoes = anchor?.takeIf { anchorCategory == Category.SHOES }
        val anchorLayer = anchor?.takeIf { anchorCategory == Category.OUTERWEAR }
        val anchorAccessory = anchor?.takeIf { anchorCategory == Category.ACCESSORY }

        val candidates = bases.map { base ->
            var pieces = base

            val shoe = anchorShoes ?: shoes.maxByOrNull { evaluate(pieces + it, request, climate).score }
            if (shoe != null) pieces = pieces + shoe

            val layer = anchorLayer
                ?: if (wantsLayer) layers.maxByOrNull { evaluate(pieces + it, request, climate).score } else null
            if (layer != null) pieces = pieces + layer

            if (anchorAccessory != null) {
                pieces = pieces + anchorAccessory
            } else {
                val withoutAccessory = evaluate(pieces, request, climate).score
                val accessory = accessories.maxByOrNull { evaluate(pieces + it, request, climate).score }
                if (accessory != null && evaluate(pieces + accessory, request, climate).score > withoutAccessory + 1.2) {
                    pieces = pieces + accessory
                }
            }

            val evaluation = evaluate(pieces, request, climate)
            Candidate(pieces, evaluation, evaluation.score + random.nextDouble() * 1.5)
        }

        // Pick the best outfits that are clearly different from each other.
        val sorted = candidates.sortedByDescending { it.total }
        val chosen = mutableListOf<Candidate>()
        for (candidate in sorted) {
            if (chosen.size == count) break
            if (chosen.none { tooSimilar(it.pieces, candidate.pieces) }) chosen += candidate
        }
        for (candidate in sorted) {
            if (chosen.size == count) break
            if (chosen.none { samePieces(it.pieces, candidate.pieces) }) chosen += candidate
        }

        val usedTitles = mutableSetOf<OutfitTitle>()
        val usedTips = mutableSetOf<Tip>()
        return chosen.map { candidate ->
            val evaluation = candidate.evaluation
            val title = evaluation.titles.firstOrNull { it !in usedTitles }
                ?: OutfitTitle.READY.takeIf { it !in usedTitles }
                ?: evaluation.titles.firstOrNull()
                ?: OutfitTitle.READY
            usedTitles += title
            val tips = tipsFor(request, climate, evaluation, random)
            val tip = tips.firstOrNull { it !in usedTips } ?: tips.first()
            usedTips += tip
            OutfitSuggestion(
                pieces = candidate.pieces,
                title = title,
                reasons = evaluation.reasons.take(3),
                tip = tip,
                matchedStyle = evaluation.matchedStyle,
            )
        }
    }

    private fun usable(items: List<ClothingItem>): List<ClothingItem> =
        items.filter { it.status == ItemStatus.READY && it.categoryEnum() != Category.OTHER }

    private fun climateOf(request: OutfitRequest): Climate {
        val t = request.weather?.temperature
        if (t != null) {
            return when {
                t >= 26 -> Climate.HOT
                t >= 20 -> Climate.WARM
                t >= 14 -> Climate.MILD
                t >= 8 -> Climate.COOL
                else -> Climate.COLD
            }
        }
        return when (request.month) {
            6, 7, 8 -> Climate.WARM
            4, 5, 9, 10 -> Climate.MILD
            3, 11 -> Climate.COOL
            else -> Climate.COLD
        }
    }

    private val reasonPriority = listOf(
        Reason.ANCHOR,
        Reason.STYLE,
        Reason.TONAL, Reason.CONTRAST, Reason.POP, Reason.ANALOGOUS, Reason.COMPLEMENTARY, Reason.NEUTRALS,
        Reason.WARM_LAYER, Reason.RAIN, Reason.LIGHT,
        Reason.FORMALITY,
        Reason.ONE_PATTERN,
        Reason.NO_SHOES,
        Reason.FRESH,
    )

    private fun evaluate(pieces: List<ClothingItem>, request: OutfitRequest, climate: Climate): Evaluation {
        var score = 0.0
        val reasons = mutableListOf<Reason>()

        val anchor = request.anchor
        if (anchor != null && pieces.any { it.id == anchor.id }) reasons += Reason.ANCHOR

        // --- Colors ---
        val boldColors = request.preferredStyles.any { StyleProfiles.of(it).boldColors }
        val primaries = pieces.mapNotNull { it.colorList().firstOrNull() }
        val accents = primaries.filter { !it.neutral }.distinct()
        val families = primaries.map { it.family }.distinct()
        val tonal = primaries.size >= 2 && families.size == 1 && primaries.distinct().size >= 2
        val contrast = NamedColor.WHITE in primaries &&
            primaries.any { it == NamedColor.BLACK || it == NamedColor.NAVY || it == NamedColor.DARK_GREY }
        var harmony = false
        when {
            tonal -> {
                score += 2.0
                reasons += Reason.TONAL
            }
            accents.isEmpty() -> {
                score += 2.0
                if (contrast) {
                    score += 1.0
                    reasons += Reason.CONTRAST
                } else {
                    reasons += Reason.NEUTRALS
                }
            }
            accents.size == 1 -> {
                score += 3.0
                reasons += Reason.POP
            }
            accents.size == 2 -> {
                val distance = NamedColor.hueDistance(accents[0], accents[1])
                when {
                    distance <= 45f -> {
                        score += 2.0
                        reasons += Reason.ANALOGOUS
                        harmony = true
                    }
                    distance >= 150f -> {
                        score += 1.5
                        reasons += Reason.COMPLEMENTARY
                        harmony = true
                    }
                    else -> score -= if (boldColors) 0.5 else 2.5
                }
            }
            else -> score -= if (boldColors) 1.5 else 4.0
        }

        // --- Patterns: one is great, more is busy ---
        val patterned = pieces.count { it.patternEnum() != Pattern.SOLID }
        if (patterned == 1) {
            score += 0.5
            reasons += Reason.ONE_PATTERN
        } else if (patterned > 1) {
            score -= 3.0 * (patterned - 1)
        }

        // --- Formality and style (clothes only) ---
        val clothes = pieces.filter { it.categoryEnum() != Category.ACCESSORY }
        val target = request.occasion.formality
        val deviations = clothes.map { abs(it.formality - target) }
        val deviation = deviations.average()
        score -= 2.0 * deviation
        // A single piece far off the occasion (sneakers at a wedding) is a real problem.
        score -= deviations.sumOf { if (it > 1.5) 2.0 * (it - 1.5) else 0.0 }
        val spread = clothes.maxOf { it.formality } - clothes.minOf { it.formality }
        if (spread > 2) score -= 1.5
        if (deviation <= 0.8) reasons += Reason.FORMALITY
        score += clothes.sumOf { request.occasion.styleAffinity(it.styleEnum()) }

        // --- Season versus weather ---
        val warmWeather = climate == Climate.HOT || climate == Climate.WARM
        val coldWeather = climate == Climate.COOL || climate == Climate.COLD
        for (piece in clothes) {
            val season = piece.seasonEnum()
            if (warmWeather && season == Season.COLD) score -= 2.0
            if (coldWeather && season == Season.WARM) score -= 2.0
        }

        // --- Layers ---
        val hasLayer = pieces.any { it.categoryEnum() == Category.OUTERWEAR }
        if (hasLayer) {
            when (climate) {
                Climate.COOL, Climate.COLD -> {
                    score += 2.0
                    reasons += Reason.WARM_LAYER
                }
                Climate.HOT -> score -= 4.0
                Climate.WARM -> score -= 1.0
                Climate.MILD -> Unit
            }
            if (request.weather?.rainy == true) reasons += Reason.RAIN
        } else if (climate == Climate.HOT && clothes.none { it.seasonEnum() == Season.COLD }) {
            reasons += Reason.LIGHT
        }

        if (pieces.none { it.categoryEnum() == Category.SHOES }) reasons += Reason.NO_SHOES

        // --- Recently worn ---
        val worn = pieces.count { it.id in request.recentlyWorn }
        score -= 2.0 * worn
        if (request.recentlyWorn.isNotEmpty() && worn == 0) reasons += Reason.FRESH

        // --- Favorite styles: reward the outfit for the style it fits best ---
        var matchedStyle: Style? = null
        if (request.preferredStyles.isNotEmpty()) {
            val best = request.preferredStyles
                .map { style -> style to styleFit(StyleProfiles.of(style), style, pieces, clothes) }
                .maxByOrNull { it.second }
            if (best != null) {
                score += best.second
                if (best.second >= 1.5) {
                    matchedStyle = best.first
                    reasons += Reason.STYLE
                }
            }
        }

        // --- Possible titles, best first ---
        val averageFormality = clothes.map { it.formality.toDouble() }.average()
        val titles = buildList {
            if (averageFormality >= 4.0) add(OutfitTitle.SHARP)
            if (patterned == 1 && accents.size <= 1) add(OutfitTitle.PATTERN)
            if (tonal) add(OutfitTitle.TONAL)
            if (contrast && accents.isEmpty()) add(OutfitTitle.CONTRAST)
            if (accents.size == 1) add(OutfitTitle.POP)
            if (harmony) add(OutfitTitle.HARMONY)
            if (hasLayer && coldWeather) add(OutfitTitle.LAYERED)
            if (averageFormality <= 2.2) add(OutfitTitle.EASY)
            if (accents.isEmpty()) add(OutfitTitle.NEUTRAL)
        }

        return Evaluation(
            score = score,
            reasons = reasons.distinct().sortedBy { reasonPriority.indexOf(it) },
            titles = titles,
            accentCount = accents.size,
            patterned = patterned,
            matchedStyle = matchedStyle,
        )
    }

    /** How close an outfit is to a style profile (roughly -3 … +6). */
    private fun styleFit(
        profile: StyleProfile,
        style: Style,
        pieces: List<ClothingItem>,
        clothes: List<ClothingItem>,
    ): Double {
        var fit = 0.0
        for (piece in pieces) {
            val itemStyle = piece.styleEnum()
            fit += when {
                itemStyle == null -> 0.0
                itemStyle == style -> 1.2
                itemStyle in profile.related -> 0.6
                itemStyle in profile.opposed -> -0.8
                else -> 0.0
            }
            val primary = piece.colorList().firstOrNull()
            if (primary != null) fit += if (primary in profile.colors) 0.4 else -0.15
            if (piece.patternEnum() != Pattern.SOLID && piece.patternEnum() in profile.patterns) fit += 0.5
        }
        val averageFormality = clothes.map { it.formality.toDouble() }.average()
        fit -= 0.5 * abs(averageFormality - profile.formality)
        return fit
    }

    private fun tipsFor(request: OutfitRequest, climate: Climate, evaluation: Evaluation, random: Random): List<Tip> =
        buildList {
            if (request.weather?.rainy == true) add(Tip.RAIN)
            when (climate) {
                Climate.COOL, Climate.COLD -> add(Tip.COLD)
                Climate.HOT -> add(Tip.HOT)
                else -> Unit
            }
            when (request.occasion) {
                Occasion.CEREMONY -> add(Tip.CEREMONY)
                Occasion.WORK -> add(Tip.WORK)
                Occasion.SPORT -> add(Tip.SPORT)
                else -> Unit
            }
            if (evaluation.patterned == 1) add(Tip.PATTERN)
            if (evaluation.accentCount == 0) add(Tip.NEUTRAL)
            addAll(listOf(Tip.ROLL, Tip.FIT).shuffled(random))
        }

    private fun tooSimilar(a: List<ClothingItem>, b: List<ClothingItem>): Boolean {
        val overlap = a.map { it.id }.intersect(b.map { it.id }.toSet()).size
        return overlap * 2 > minOf(a.size, b.size)
    }

    private fun samePieces(a: List<ClothingItem>, b: List<ClothingItem>): Boolean =
        a.map { it.id }.toSet() == b.map { it.id }.toSet()
}
