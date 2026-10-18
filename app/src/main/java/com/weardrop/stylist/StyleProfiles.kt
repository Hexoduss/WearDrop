package com.weardrop.stylist

import com.weardrop.data.NamedColor
import com.weardrop.data.NamedColor.BEIGE
import com.weardrop.data.NamedColor.BLACK
import com.weardrop.data.NamedColor.BLUE
import com.weardrop.data.NamedColor.BROWN
import com.weardrop.data.NamedColor.BURGUNDY
import com.weardrop.data.NamedColor.CAMEL
import com.weardrop.data.NamedColor.DARK_GREY
import com.weardrop.data.NamedColor.DENIM
import com.weardrop.data.NamedColor.GREEN
import com.weardrop.data.NamedColor.GREY
import com.weardrop.data.NamedColor.LIGHT_BLUE
import com.weardrop.data.NamedColor.LILAC
import com.weardrop.data.NamedColor.NAVY
import com.weardrop.data.NamedColor.OLIVE
import com.weardrop.data.NamedColor.ORANGE
import com.weardrop.data.NamedColor.PINK
import com.weardrop.data.NamedColor.PURPLE
import com.weardrop.data.NamedColor.RED
import com.weardrop.data.NamedColor.TEAL
import com.weardrop.data.NamedColor.WHITE
import com.weardrop.data.NamedColor.YELLOW
import com.weardrop.data.Pattern
import com.weardrop.data.Pattern.CHECKED
import com.weardrop.data.Pattern.FLORAL
import com.weardrop.data.Pattern.PRINT
import com.weardrop.data.Pattern.SOLID
import com.weardrop.data.Pattern.STRIPED
import com.weardrop.data.Style

/**
 * What a style looks like, in terms the outfit rules understand:
 * typical colors, patterns, how dressy it is, which item styles belong to it
 * and which clash with it. [boldColors] styles tolerate several strong colors together.
 */
data class StyleProfile(
    val formality: Double,
    val colors: Set<NamedColor>,
    val patterns: Set<Pattern> = emptySet(),
    val related: Set<Style> = emptySet(),
    val opposed: Set<Style> = emptySet(),
    val boldColors: Boolean = false,
)

object StyleProfiles {

    private val profiles: Map<Style, StyleProfile> = mapOf(
        Style.CASUAL to StyleProfile(
            formality = 2.0,
            colors = setOf(DENIM, WHITE, GREY, NAVY, BEIGE),
            patterns = setOf(STRIPED),
            related = setOf(Style.STREETWEAR, Style.SMART_CASUAL, Style.MINIMAL, Style.SPORTY),
            opposed = setOf(Style.BUSINESS, Style.GLAM),
        ),
        Style.SPORTY to StyleProfile(
            formality = 1.5,
            colors = setOf(BLACK, WHITE, GREY, NAVY, BLUE, RED),
            patterns = setOf(SOLID),
            related = setOf(Style.STREETWEAR, Style.TECHWEAR, Style.CASUAL, Style.SKATER),
            opposed = setOf(Style.ELEGANT, Style.BUSINESS, Style.ROMANTIC, Style.GLAM),
        ),
        Style.ELEGANT to StyleProfile(
            formality = 4.2,
            colors = setOf(BLACK, WHITE, NAVY, BURGUNDY, CAMEL, BEIGE, DARK_GREY),
            patterns = setOf(SOLID),
            related = setOf(Style.CLASSIC, Style.GLAM, Style.BUSINESS, Style.OLD_MONEY, Style.ROMANTIC),
            opposed = setOf(Style.SPORTY, Style.GRUNGE, Style.SKATER, Style.PUNK),
        ),
        Style.CLASSIC to StyleProfile(
            formality = 3.5,
            colors = setOf(NAVY, WHITE, BEIGE, CAMEL, GREY, LIGHT_BLUE, BROWN),
            patterns = setOf(STRIPED, CHECKED, SOLID),
            related = setOf(Style.PREPPY, Style.OLD_MONEY, Style.BUSINESS, Style.ELEGANT, Style.SMART_CASUAL),
            opposed = setOf(Style.Y2K, Style.PUNK, Style.GRUNGE),
        ),
        Style.STREETWEAR to StyleProfile(
            formality = 1.8,
            colors = setOf(BLACK, WHITE, GREY, OLIVE, ORANGE, RED),
            patterns = setOf(PRINT),
            related = setOf(Style.SKATER, Style.SPORTY, Style.TECHWEAR, Style.Y2K, Style.CASUAL),
            opposed = setOf(Style.BUSINESS, Style.OLD_MONEY, Style.ROMANTIC),
            boldColors = true,
        ),
        Style.MINIMAL to StyleProfile(
            formality = 3.0,
            colors = setOf(BLACK, WHITE, GREY, BEIGE, NAVY, DARK_GREY),
            patterns = setOf(SOLID),
            related = setOf(Style.CLASSIC, Style.SMART_CASUAL, Style.TECHWEAR, Style.ELEGANT),
            opposed = setOf(Style.BOHO, Style.Y2K, Style.GLAM),
        ),
        Style.GLAM to StyleProfile(
            formality = 4.3,
            colors = setOf(BLACK, BURGUNDY, RED, PINK, WHITE, PURPLE),
            patterns = setOf(SOLID, PRINT),
            related = setOf(Style.ELEGANT, Style.ROMANTIC, Style.Y2K, Style.ROCK),
            opposed = setOf(Style.WORKWEAR, Style.UTILITY, Style.SPORTY, Style.GRUNGE),
            boldColors = true,
        ),
        Style.PUNK to StyleProfile(
            formality = 1.8,
            colors = setOf(BLACK, RED, DARK_GREY, WHITE),
            patterns = setOf(CHECKED, PRINT),
            related = setOf(Style.ROCK, Style.GRUNGE, Style.GOTHIC, Style.SKATER),
            opposed = setOf(Style.PREPPY, Style.OLD_MONEY, Style.ROMANTIC, Style.BUSINESS, Style.ELEGANT),
        ),
        Style.GRUNGE to StyleProfile(
            formality = 1.7,
            colors = setOf(BLACK, DARK_GREY, OLIVE, BURGUNDY, BROWN, DENIM),
            patterns = setOf(CHECKED, SOLID),
            related = setOf(Style.PUNK, Style.ROCK, Style.INDIE, Style.SKATER),
            opposed = setOf(Style.PREPPY, Style.GLAM, Style.BUSINESS, Style.OLD_MONEY),
        ),
        Style.ROCK to StyleProfile(
            formality = 2.2,
            colors = setOf(BLACK, DARK_GREY, WHITE, DENIM, BURGUNDY),
            patterns = setOf(PRINT, SOLID),
            related = setOf(Style.PUNK, Style.GRUNGE, Style.GOTHIC, Style.GLAM),
            opposed = setOf(Style.PREPPY, Style.ROMANTIC, Style.BUSINESS),
        ),
        Style.GOTHIC to StyleProfile(
            formality = 3.0,
            colors = setOf(BLACK, BURGUNDY, DARK_GREY, PURPLE),
            patterns = setOf(SOLID, PRINT),
            related = setOf(Style.PUNK, Style.ROCK, Style.GRUNGE),
            opposed = setOf(Style.PREPPY, Style.BOHO, Style.Y2K, Style.WESTERN),
        ),
        Style.BOHO to StyleProfile(
            formality = 2.3,
            colors = setOf(BEIGE, CAMEL, BROWN, OLIVE, ORANGE, WHITE, BURGUNDY),
            patterns = setOf(FLORAL, PRINT),
            related = setOf(Style.ROMANTIC, Style.VINTAGE, Style.WESTERN, Style.INDIE),
            opposed = setOf(Style.BUSINESS, Style.TECHWEAR, Style.MINIMAL),
            boldColors = true,
        ),
        Style.PREPPY to StyleProfile(
            formality = 3.2,
            colors = setOf(NAVY, WHITE, LIGHT_BLUE, BEIGE, RED, GREEN, BURGUNDY),
            patterns = setOf(STRIPED, CHECKED),
            related = setOf(Style.CLASSIC, Style.OLD_MONEY, Style.SMART_CASUAL),
            opposed = setOf(Style.PUNK, Style.GRUNGE, Style.GOTHIC, Style.STREETWEAR),
        ),
        Style.OLD_MONEY to StyleProfile(
            formality = 3.8,
            colors = setOf(NAVY, BEIGE, CAMEL, WHITE, BROWN, GREEN, LIGHT_BLUE, BURGUNDY),
            patterns = setOf(SOLID, CHECKED, STRIPED),
            related = setOf(Style.CLASSIC, Style.PREPPY, Style.ELEGANT),
            opposed = setOf(Style.STREETWEAR, Style.Y2K, Style.PUNK, Style.SPORTY),
        ),
        Style.VINTAGE to StyleProfile(
            formality = 2.8,
            colors = setOf(BROWN, CAMEL, BURGUNDY, OLIVE, BEIGE, DENIM, YELLOW),
            patterns = setOf(FLORAL, CHECKED, STRIPED),
            related = setOf(Style.INDIE, Style.BOHO, Style.ROMANTIC, Style.WESTERN),
            opposed = setOf(Style.TECHWEAR, Style.SPORTY),
        ),
        Style.Y2K to StyleProfile(
            formality = 2.0,
            colors = setOf(PINK, LILAC, LIGHT_BLUE, WHITE, BLACK, DENIM, PURPLE),
            patterns = setOf(PRINT),
            related = setOf(Style.STREETWEAR, Style.GLAM, Style.SPORTY),
            opposed = setOf(Style.OLD_MONEY, Style.BUSINESS, Style.CLASSIC),
            boldColors = true,
        ),
        Style.ROMANTIC to StyleProfile(
            formality = 3.0,
            colors = setOf(PINK, WHITE, LILAC, BEIGE, LIGHT_BLUE, BURGUNDY),
            patterns = setOf(FLORAL),
            related = setOf(Style.BOHO, Style.VINTAGE, Style.ELEGANT, Style.GLAM),
            opposed = setOf(Style.PUNK, Style.UTILITY, Style.TECHWEAR, Style.WORKWEAR),
        ),
        Style.BUSINESS to StyleProfile(
            formality = 4.0,
            colors = setOf(NAVY, DARK_GREY, BLACK, WHITE, LIGHT_BLUE, GREY),
            patterns = setOf(SOLID, STRIPED),
            related = setOf(Style.CLASSIC, Style.ELEGANT, Style.SMART_CASUAL, Style.MINIMAL),
            opposed = setOf(Style.SPORTY, Style.STREETWEAR, Style.PUNK, Style.GRUNGE, Style.SKATER, Style.Y2K),
        ),
        Style.SMART_CASUAL to StyleProfile(
            formality = 3.0,
            colors = setOf(NAVY, WHITE, BEIGE, GREY, LIGHT_BLUE, DENIM, CAMEL),
            patterns = setOf(SOLID, CHECKED),
            related = setOf(Style.CLASSIC, Style.CASUAL, Style.MINIMAL, Style.PREPPY),
            opposed = setOf(Style.PUNK, Style.SPORTY),
        ),
        Style.WESTERN to StyleProfile(
            formality = 2.5,
            colors = setOf(BROWN, CAMEL, DENIM, BEIGE, WHITE, BURGUNDY),
            patterns = setOf(CHECKED, PRINT),
            related = setOf(Style.VINTAGE, Style.BOHO, Style.WORKWEAR),
            opposed = setOf(Style.TECHWEAR, Style.BUSINESS, Style.Y2K),
        ),
        Style.UTILITY to StyleProfile(
            formality = 2.3,
            colors = setOf(OLIVE, BEIGE, CAMEL, BLACK, BROWN),
            patterns = setOf(SOLID),
            related = setOf(Style.WORKWEAR, Style.TECHWEAR, Style.STREETWEAR),
            opposed = setOf(Style.GLAM, Style.ROMANTIC),
        ),
        Style.WORKWEAR to StyleProfile(
            formality = 2.2,
            colors = setOf(BROWN, CAMEL, NAVY, OLIVE, DENIM, ORANGE),
            patterns = setOf(CHECKED, SOLID),
            related = setOf(Style.UTILITY, Style.WESTERN, Style.VINTAGE, Style.CASUAL),
            opposed = setOf(Style.GLAM, Style.ROMANTIC),
        ),
        Style.SKATER to StyleProfile(
            formality = 1.5,
            colors = setOf(BLACK, WHITE, RED, NAVY, DENIM, GREY),
            patterns = setOf(CHECKED, PRINT, STRIPED),
            related = setOf(Style.STREETWEAR, Style.PUNK, Style.GRUNGE, Style.CASUAL),
            opposed = setOf(Style.BUSINESS, Style.ELEGANT, Style.OLD_MONEY),
        ),
        Style.INDIE to StyleProfile(
            formality = 2.3,
            colors = setOf(BURGUNDY, OLIVE, BROWN, YELLOW, DENIM, BEIGE, TEAL),
            patterns = setOf(STRIPED, CHECKED, FLORAL),
            related = setOf(Style.VINTAGE, Style.GRUNGE, Style.BOHO),
            opposed = setOf(Style.BUSINESS, Style.GLAM),
            boldColors = true,
        ),
        Style.TECHWEAR to StyleProfile(
            formality = 2.0,
            colors = setOf(BLACK, DARK_GREY, GREY, OLIVE, NAVY),
            patterns = setOf(SOLID),
            related = setOf(Style.UTILITY, Style.STREETWEAR, Style.SPORTY, Style.MINIMAL),
            opposed = setOf(Style.BOHO, Style.ROMANTIC, Style.VINTAGE, Style.WESTERN),
        ),
    )

    fun of(style: Style): StyleProfile = profiles.getValue(style)
}
