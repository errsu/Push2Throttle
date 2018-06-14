import java.awt.Color
import java.awt.Font
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class Push2DisplayDrawing() {

    private val displayWidth       = 960
    private val displayHeight      = 160
    private val lineFiller         = 64 // extra unused pixels per line

    private val image = BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_ARGB)
    private val g = image.createGraphics()
    private val font24 = Font("SansSerif", Font.PLAIN, 24)

    private var lastFrame = -1

    private fun prepareFrame(frame: Int) {
        if (frame != lastFrame) {
            val x0 = 0.0
            val x1 = (displayWidth - 1).toDouble()
            val y0 = 0.0
            val y1 = (displayHeight - 1).toDouble()
            val tw = (displayWidth / 8).toDouble() // track width
            g.clearRect(0, 0, displayWidth, displayHeight)
            g.paint = Color(32, 64, 64)
            g.fill(Rectangle2D.Double(x0, y0, x1, y1))
            g.paint = Color(128, 255, 255)
            var t0 = 0.0
            for (n in 0..7) {
                g.draw(Line2D.Double(t0, y0, t0, y1))
                g.draw(Line2D.Double(t0 + tw - 1.0, y0, t0 + tw - 1.0, y1))
                t0 += tw
            }
            g.draw(Line2D.Double(x0, y0, x1, y0))
            g.draw(Line2D.Double(x0, y1, x1, y1))
            g.font = font24
            g.paint = Color(255, 0, 0)
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
        // println("this: ${java.lang.Integer.toHexString(this)} color: ${java.lang.Integer.toHexString(color)}")
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
}
