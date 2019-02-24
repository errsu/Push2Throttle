package push2throttle

import java.awt.*

object Push2Colors {

    // recommended sequence of colors for Throttles:
    // 1 7 14 18 23 26 2 8 15 21 24 3 9 16 21 4 10 17 25 5 11 19 6 12 20 13

    // TODO: Update for Live 10
    val num2Display = mapOf(
         0 to Color(0x000000),
         1 to Color(0xed5938),
         2 to Color(0xd1170a),
         3 to Color(0xff6400),
         4 to Color(0xff3200),
         5 to Color(0x804713),
         6 to Color(0x582307),
         7 to Color(0xedda3c),
         8 to Color(0xe4c200),
         9 to Color(0x94ff18),
        10 to Color(0x00e631),
        11 to Color(0x009d32),
        12 to Color(0x339e13),
        13 to Color(0x00b955),
        14 to Color(0x00714e),
        15 to Color(0x00CC89),
        16 to Color(0x00bbad),
        17 to Color(0x0071a4),
        18 to Color(0x006aca),
        19 to Color(0x4932b3),
        20 to Color(0x005a62),
        21 to Color(0x5260dd),
        22 to Color(0xab50ff),
        23 to Color(0xe157e3),
        24 to Color(0x88425b),
        25 to Color(0xff1e32),
        26 to Color(0xff4a96),
       122 to Color(0xFFFFFF),
       123 to Color(0x404040),
       124 to Color(0x141414),
       125 to Color(0x0000FF),
       126 to Color(0x00FF00),
       127 to Color(0xFF0000)
    )

    val defaultDisplayBackgroundColor = Color(0x7070DF)

    fun brighterDisplayColor(color: Color) : Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return Color.getHSBColor(
                hsb[0],
                hsb[1],
                (2.0f + hsb[2]) / 3.0f)
    }

    fun darkerDisplayColor(color: Color) : Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return Color.getHSBColor(
                hsb[0],
                0.5f * (1.0f + hsb[1]),
                hsb[2] / 4.0f)
    }

    fun invertedDisplayColor(color: Color) : Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return Color.getHSBColor(
                if (hsb[0] > 0.5f) hsb[0] - 0.5f else hsb[0] + 0.5f,
                hsb[1] * 0.8f,
                hsb[2])
    }

    val nameToNumber = mapOf(
            "lt-rose"  to   1, "red"       to   2, "orange"    to   3, "dk-orange" to   4,
            "lt-brown" to   5, "dk-brown"  to   6, "lt-yellow" to   7, "yellow"    to   8,
            "lime"     to   9, "lt-green"  to  10, "green"     to  11, "dk-lime"   to  12,
            "mint"     to  13, "pale-cyan" to  14, "cyan"      to  15, "lt-blue"   to  16,
            "blue"     to  17, "dk-blue"   to  18, "navy"      to  19, "pale-blue" to  20,
            "lt-navy"  to  21, "indigo"    to  22, "lt-indigo" to  23, "pale-pink" to  24,
            "rose"     to  25, "pink"      to  26,
            "white"    to 120, "dk-white"  to 122, "lt-gray"   to 121, "gray"      to 123,
            "dk-gray"  to 124, "lt-black"  to 119, "black"     to   0,
            "rgb-blue" to 125, "rgb-green" to 126, "rgb-red"   to 127,
            "hi-blue"  to 48)

    val numberToName = Array(128) {"unknown"}.apply {
        nameToNumber.forEach { name, number ->
            set(number, name)
        }
    }

    fun darkerLEDColor(number: Int): Int {
        return if (number in 1..26) 63 + number * 2
        else throw Exception("not a standard color: $number")
    }

    fun darkestLEDColor(number: Int): Int {
        return if (number in 1..26) 64 + number * 2
        else throw Exception("not a standard color: $number")
    }

    fun initRgbPalette(midi: Push2MidiDriver) {
        midi.setColorPaletteEntry(119, 8, 8, 8, 123) // make light black really dark (was 0x1A, 0x1A, 0x1A, 123)
    }
}
