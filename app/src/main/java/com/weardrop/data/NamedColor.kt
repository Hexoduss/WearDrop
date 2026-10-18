package com.weardrop.data

import androidx.annotation.StringRes
import com.weardrop.R
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Fixed color palette. Photos are mapped to these names, and the outfit rules
 * reason about them (neutral or not, hue, color family).
 */
enum class NamedColor(
    val key: String,
    val rgb: Int,
    val neutral: Boolean,
    val family: String,
    @StringRes val label: Int,
) {
    BLACK("black", 0x1C1C1C, true, "mono", R.string.color_black),
    WHITE("white", 0xF4F4F2, true, "mono", R.string.color_white),
    GREY("grey", 0x9A9A9A, true, "mono", R.string.color_grey),
    DARK_GREY("dark_grey", 0x4A4A4A, true, "mono", R.string.color_dark_grey),
    BEIGE("beige", 0xD9C7A7, true, "earth", R.string.color_beige),
    CAMEL("camel", 0xB88A57, true, "earth", R.string.color_camel),
    BROWN("brown", 0x5E3B22, true, "earth", R.string.color_brown),
    NAVY("navy", 0x1F2A44, true, "blue", R.string.color_navy),
    DENIM("denim", 0x4C6A8C, true, "blue", R.string.color_denim),
    BLUE("blue", 0x2F62B5, false, "blue", R.string.color_blue),
    LIGHT_BLUE("light_blue", 0x9CC3E6, false, "blue", R.string.color_light_blue),
    TEAL("teal", 0x1D7373, false, "green", R.string.color_teal),
    GREEN("green", 0x3C8A45, false, "green", R.string.color_green),
    OLIVE("olive", 0x6B6A3A, true, "green", R.string.color_olive),
    YELLOW("yellow", 0xE6C443, false, "warm", R.string.color_yellow),
    ORANGE("orange", 0xE07B2A, false, "warm", R.string.color_orange),
    RED("red", 0xC0392B, false, "red", R.string.color_red),
    BURGUNDY("burgundy", 0x6A1B2B, false, "red", R.string.color_burgundy),
    PINK("pink", 0xE9A6BA, false, "red", R.string.color_pink),
    PURPLE("purple", 0x6A3A8A, false, "purple", R.string.color_purple),
    LILAC("lilac", 0xBBA6D8, false, "purple", R.string.color_lilac);

    /** ARGB, ready for Compose `Color(...)`. */
    val argb: Int get() = rgb or (0xFF shl 24)

    /** Hue in degrees (0–360). */
    val hue: Float by lazy { hueOf(rgb) }

    internal val lab: DoubleArray by lazy { rgbToLab(rgb) }

    companion object {
        fun fromKey(key: String): NamedColor? = entries.firstOrNull { it.key == key.trim() }

        /** Closest palette color to an (A)RGB pixel, by perceptual (Lab) distance. */
        fun nearest(pixel: Int): NamedColor {
            val lab = rgbToLab(pixel and 0xFFFFFF)
            var best = BLACK
            var bestDistance = Double.MAX_VALUE
            for (color in entries) {
                val l = color.lab
                val d = (lab[0] - l[0]).pow(2) + (lab[1] - l[1]).pow(2) + (lab[2] - l[2]).pow(2)
                if (d < bestDistance) {
                    bestDistance = d
                    best = color
                }
            }
            return best
        }

        /** Smallest angle between two hues (0–180). */
        fun hueDistance(a: NamedColor, b: NamedColor): Float {
            val d = abs(a.hue - b.hue) % 360f
            return if (d > 180f) 360f - d else d
        }
    }
}

private fun hueOf(rgb: Int): Float {
    val r = ((rgb shr 16) and 0xFF) / 255f
    val g = ((rgb shr 8) and 0xFF) / 255f
    val b = (rgb and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    if (delta == 0f) return 0f
    val h = when (maxC) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return if (h < 0f) h + 360f else h
}

private fun rgbToLab(rgb: Int): DoubleArray {
    fun linear(channel: Int): Double {
        val v = channel / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    val r = linear((rgb shr 16) and 0xFF)
    val g = linear((rgb shr 8) and 0xFF)
    val b = linear(rgb and 0xFF)
    val x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
    val y = (r * 0.2126 + g * 0.7152 + b * 0.0722)
    val z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883
    fun f(t: Double) = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116.0
    val fx = f(x)
    val fy = f(y)
    val fz = f(z)
    return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
}
