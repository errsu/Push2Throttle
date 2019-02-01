package push2throttle

import java.awt.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

abstract class Push2View(val rect: Rectangle) {
    abstract fun draw(g2: Graphics2D, frame: Int, display: Push2Display)
}

class Push2Display {

    val width       = 960
    val height      = 160

    private val lineFiller  = 64 // extra unused pixels per line
    private val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    private val g = image.createGraphics()

    private var lastFrame = -1

    // recommended sequence of colors for Throttles:
    // 1 7 14 18 23 26 2 8 15 21 24 3 9 16 21 4 10 17 25 5 11 19 6 12 20 13

    // TODO: Update for Live 10
    val push2Colors = mapOf(
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

    private val backgroundColor = Color(0x7070DF)

    fun brighterColor(color: Color) : Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return Color.getHSBColor(
                hsb[0],
                hsb[1],
                (2.0f + hsb[2]) / 3.0f)
    }

    fun darkerColor(color: Color) : Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return Color.getHSBColor(
                hsb[0],
                0.5f * (1.0f + hsb[1]),
                hsb[2] / 4.0f)
    }

    fun invertedColor(color: Color) : Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return Color.getHSBColor(
                if (hsb[0] > 0.5f) hsb[0] - 0.5f else hsb[0] + 0.5f,
                hsb[1] * 0.8f,
                hsb[2])
    }

    private val viewList = mutableListOf<Push2View>()

    @Synchronized
    private fun drawFrame(frame: Int) {
        g.paint = backgroundColor
        g.fill(Rectangle(0, 0, width, height))

        // TODO: make sure drawing is done in Main view
        val iterator = viewList.iterator()
        for (view in iterator) {
            val gView = g.create() as Graphics2D
            gView.translate(view.rect.x, view.rect.y)
            view.draw(gView, frame, this)
        }
    }

    @Synchronized
    fun addView(view: Push2View) {
        viewList.add(view)
    }

    @Synchronized
    fun removeView(view: Push2View) {
        viewList.remove(view)
    }

    private fun Int.aRgbTo565bgr() : Short {

        // a a a a a a a a r r r r r r r r g g g g g g g g b b b b b b b b
        //                 + + + + +       * * * * * *     # # # # #
        //        |       |       |       |       |       |       |
        //                                 # # # # # * * * * * * + + + + +
        // 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 b b b b b g g g g g g r r r r r

        val r = this.shr(19).and(0x0000001F)
        val g = this.shr(5).and(0x000007E0)
        val b = this.shl(8).and(0x0000F800)
        val color = r.or(g).or(b)
        return color.toShort()
    }

    fun fillBufferWithLines(buffer: ByteBuffer, frame: Int, firstLine: Int, lineCount: Int)
    {
        if (frame != lastFrame) {
            drawFrame(frame)
            lastFrame = frame
        }

        val shortBuffer = buffer.asShortBuffer()
        var bufferIndex = 0

        repeat(lineCount) {
            val line = firstLine + it
            repeat(width) {
                shortBuffer.put(bufferIndex++, image.getRGB(it, line).aRgbTo565bgr())
            }
            bufferIndex +=  lineFiller
        }
    }
}
