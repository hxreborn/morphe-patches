/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.theme

internal enum class ColorRole { BACKGROUND, FOREGROUND, LINE }

private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL_MASK = 0xFF
private const val OPAQUE_ALPHA = 0xFF
private const val MIN_VISIBLE_ALPHA = 0x40
private const val RED_WEIGHT = 299
private const val GREEN_WEIGHT = 587
private const val BLUE_WEIGHT = 114
private const val WEIGHT_TOTAL = 1000
private const val NEUTRAL_SPREAD = 0x28

private const val CARD_MIN_LUMA = 0xFA
private const val PAGE_MIN_LUMA = 0xE8
private const val FILL_MIN_LUMA = 0xC8
private const val CARD_NIGHT = 0x1E1E1E
private const val PAGE_NIGHT = 0x121212
private const val FILL_NIGHT = 0x2B2B2B

private const val PRIMARY_TEXT_MAX_LUMA = 0x48
private const val SECONDARY_TEXT_MAX_LUMA = 0x80
private const val TERTIARY_TEXT_MAX_LUMA = 0xB8
private const val PRIMARY_TEXT_NIGHT = 0xE8E8E8
private const val SECONDARY_TEXT_NIGHT = 0xB8B8B8
private const val TERTIARY_TEXT_NIGHT = 0x8E8E8E
private const val LINE_NIGHT = 0x3A3A3A
private const val LIGHT_LINE_MIN_LUMA = 0xC8

internal object DarkPalette {

    fun night(role: ColorRole, argb: Int): Int? {
        val alpha = (argb ushr ALPHA_SHIFT) and CHANNEL_MASK
        val red = (argb shr RED_SHIFT) and CHANNEL_MASK
        val green = (argb shr GREEN_SHIFT) and CHANNEL_MASK
        val blue = argb and CHANNEL_MASK
        if (alpha < MIN_VISIBLE_ALPHA || maxOf(red, green, blue) - minOf(red, green, blue) > NEUTRAL_SPREAD) return null
        val luma = (red * RED_WEIGHT + green * GREEN_WEIGHT + blue * BLUE_WEIGHT) / WEIGHT_TOTAL
        val rgb = when (role) {
            ColorRole.BACKGROUND -> when {
                luma >= CARD_MIN_LUMA -> CARD_NIGHT
                luma >= PAGE_MIN_LUMA -> PAGE_NIGHT
                luma >= FILL_MIN_LUMA -> FILL_NIGHT
                else -> return null
            }
            ColorRole.FOREGROUND -> foreground(luma) ?: return null
            ColorRole.LINE -> when {
                luma >= LIGHT_LINE_MIN_LUMA -> LINE_NIGHT
                else -> foreground(luma) ?: return null
            }
        }
        return (alpha shl ALPHA_SHIFT) or rgb
    }

    private fun foreground(luma: Int): Int? = when {
        luma <= PRIMARY_TEXT_MAX_LUMA -> PRIMARY_TEXT_NIGHT
        luma <= SECONDARY_TEXT_MAX_LUMA -> SECONDARY_TEXT_NIGHT
        luma <= TERTIARY_TEXT_MAX_LUMA -> TERTIARY_TEXT_NIGHT
        else -> null
    }

    fun parse(value: String): Int? {
        val digits = value.removePrefix("#")
        val expanded = when (digits.length) {
            3 -> digits.map { "$it$it" }.joinToString("").let { "FF$it" }
            4 -> digits.map { "$it$it" }.joinToString("")
            6 -> "FF$digits"
            8 -> digits
            else -> return null
        }
        return expanded.toLongOrNull(16)?.toInt()
    }

    fun format(argb: Int) = "#%08x".format(argb)

}
