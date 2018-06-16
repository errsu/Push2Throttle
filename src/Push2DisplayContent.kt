import java.awt.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class Push2DisplayContent {

    private val displayWidth       = 960
    private val displayHeight      = 160
    private val lineFiller         = 64 // extra unused pixels per line

    private val image = BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_ARGB)
    private val g = image.createGraphics()
    private val font18 = Font("SansSerif", Font.BOLD, 18)
    private val font24 = Font("SansSerif", Font.PLAIN, 24)
    private val font48 = Font("SansSerif", Font.PLAIN, 48)

    private var lastFrame = -1

    private val push2Colors = arrayListOf(
        Color(0x000000),  // 0
        Color(0xed5938),  // 1
        Color(0xd1170a),  // 2
        Color(0xff6400),  // 3
        Color(0xff3200),  // 4
        Color(0x804713),  // 5
        Color(0x582307),  // 6
        Color(0xedda3c),  // 7
        Color(0xe4c200),  // 8
        Color(0x94ff18),  // 9
        Color(0x00e631),  // 10
        Color(0x009d32),  // 11
        Color(0x339e13),  // 12
        Color(0x00b955),  // 13
        Color(0x00714e),  // 14
        Color(0x00CC89),  // 15
        Color(0x00bbad),  // 16
        Color(0x0071a4),  // 17
        Color(0x006aca),  // 18
        Color(0x4932b3),  // 19
        Color(0x005a62),  // 20
        Color(0x5260dd),  // 21
        Color(0xab50ff),  // 22
        Color(0xe157e3),  // 23
        Color(0x88425b),  // 24
        Color(0xff1e32),  // 25
        Color(0xff4a96)   // 26
    )

    private val trackColor = arrayListOf(
            1, 13, 18, 8, 12, 22, 3, 25)

    private val trackName = arrayListOf(
            "TestLok33",
            "Dampf22",
            "ELok88",
            "",
            "",
            "",
            "",
            "")

    private val trackSpeed = arrayListOf(
            0.2f,
            0.4f,
            1.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f)

    private val forward = arrayListOf(
            true,
            false,
            true,
            true,
            true,
            false,
            true,
            true)

    var first = true

    private fun darkerColor(color: Color) : Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return Color.getHSBColor(
                hsb[0],
                0.5f * (1.0f + hsb[1]),
                hsb[2] / 4.0f)
    }

    private fun invertedColor(color: Color) : Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return Color.getHSBColor(
                if (hsb[0] > 0.5f) hsb[0] - 0.5f else hsb[0] + 0.5f,
                hsb[1] * 0.8f,
                hsb[2])
    }

    private fun prepareFrame(frame: Int) {
        if (frame != lastFrame) {
            val th = displayHeight
            val tw = (displayWidth / 8) // track width
            g.clearRect(0, 0, displayWidth, displayHeight)
            for (n in 0..7) {
                val trackRect = Rectangle(n * tw, 0, tw, th)
                g.paint = push2Colors[trackColor[n]]
                g.fill(trackRect)

                g.font = font18
                g.paint = Color.BLACK
                val wName = g.getFontMetrics(font18).stringWidth(trackName[n])
                g.drawString(trackName[n], n * tw + (tw - wName) / 2, 20)

                val sliderRect = Rectangle(n * tw + 6, 30, 10, th - 38)
                g.paint = invertedColor(push2Colors[trackColor[n]]) // trackColor[n].darker()
                g.fill(sliderRect)
                g.paint = Color.BLACK
                g.draw(sliderRect)

                repeat(10) {
                    val y = sliderRect.y + sliderRect.height * it / 10
                    g.drawLine(sliderRect.x, y, sliderRect.x + sliderRect.width / 2, y)
                }

                val triangle = Polygon()
                triangle.addPoint(0, 0)
                triangle.addPoint(10, -5)
                triangle.addPoint(10, 5)
                val speedTriangleY = sliderRect.y + 1 +
                        ((sliderRect.height - 2) * (1.0f - trackSpeed[n])).roundToInt()
                val speedTriangleX = sliderRect.x + sliderRect.width + 2
                triangle.translate(speedTriangleX, speedTriangleY)
                g.fill(triangle)

                g.font = font48
                g.paint = Color.BLACK
                val speed = (trackSpeed[n] * 120.0).roundToInt().toString()
                val wSpeed = g.getFontMetrics(font48).stringWidth(speed)
                g.drawString(speed, (n + 1) * tw - wSpeed - 10, 30 + (th - 30 - 10)/2)

                if (!forward[n]) {
                    val reverseRect = Rectangle(n * tw + 36, th - 40, tw - 42, 18)
                    g.paint = Color.WHITE
                    g.fill(reverseRect)
                    g.paint = Color.BLACK
                    g.draw(reverseRect)
                    g.font = font18
                    g.paint = Color.RED
                    g.drawString("reverse", n * tw + 42, th - 25)
                }
            }
            g.font = font24
            g.paint = Color(64, 64, 64)
            g.drawString("Hello!", (frame % (displayWidth + 100)) - 50, 92)
            lastFrame = frame

        }
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

    fun apply(buffer: ByteBuffer, frame: Int, firstLine: Int, lineCount: Int)
    {
        prepareFrame(frame)

        val shortBuffer = buffer.asShortBuffer()
        var bufferIndex = 0

        repeat(lineCount) {
            val line = firstLine + it
            repeat(displayWidth) {
                shortBuffer.put(bufferIndex++, image.getRGB(it, line).aRgbTo565bgr())
            }
            bufferIndex +=  lineFiller
        }
    }

    fun updateState(throttleName: String, propertyName: String, newValue: Any) {
        if (throttleName.startsWith("T")) {
            val index = throttleName.substring(1).toInt() - 1
            if (index !in 0..7) {
                println("bad throttle index: $throttleName")
                return
            }
            when (propertyName) {
                "speed" -> if (newValue is Float) trackSpeed[index] = newValue.coerceIn(0.0f, 1.0f)
                "forward" -> if (newValue is Boolean) forward[index] = newValue
            }
        } else {
            println("bad throttle name: $throttleName")
        }
    }
}
