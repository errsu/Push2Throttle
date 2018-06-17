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
    private val throttleDisplay = ThrottleDisplay()
    private val panelDisplay = PanelDisplay()
    private var lastFrame = -1

    var showPanel = false

    val push2Colors = arrayListOf(
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

    private fun prepareFrame(frame: Int) {
        if (frame != lastFrame) {
            g.clearRect(0, 0, displayWidth, displayHeight)
            if (showPanel) {
                panelDisplay.drawFrame(g, frame, this)
            } else {
                throttleDisplay.drawFrame(g, frame, this)
            }
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
        throttleDisplay.updateState(throttleName, propertyName, newValue)
    }

    fun rosterChanged(roster: Roster) {
        throttleDisplay.rosterChanged(roster)
    }
}
