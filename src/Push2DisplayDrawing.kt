import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class Push2DisplayDrawing() {

    private val displayWidth       = 960
    private val displayHeight      = 160
    private val lineFiller         = 64 // extra unused pixels per line

    private fun rgb(r: Int, g: Int, b: Int) : Short {
        return (r).or(g.shl(5)).or(b.shl(11)).toShort()
    }

    private val white        = rgb(31, 63, 31)
    private val black        = rgb(0, 0, 0)
    private fun red(r: Int)  = rgb(r, 0, 0)
    private fun grn(g: Int)  = rgb(0, g, 0)
    private fun blu(b: Int)  = rgb(0, 0, b)
    private fun gry(gr: Int) = rgb(gr, 2*gr, gr)

    private val image = BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_USHORT_565_RGB)
    private val g = image.createGraphics()
    private val font24 = Font("SansSerif", Font.PLAIN, 24)

    private var lastFrame = -1

    fun prepareFrame(frame: Int) {
        if (frame != lastFrame) {
            g.clearRect(0, 0, displayWidth, displayHeight)
            g.font = font24
            g.drawString("Hello!", (frame % (displayWidth + 100)) - 50, 92)
            lastFrame = frame
        }
    }

    fun apply(buffer: ByteBuffer, frame: Int, firstLine: Int, lineCount: Int)
    {
        prepareFrame(frame)

        val shortBuffer = buffer.asShortBuffer()
        var bufferIndex = 0

        repeat(lineCount) {
            val line = firstLine + it
            repeat(displayWidth) {
                shortBuffer.put(bufferIndex++, image.getRGB(it, line).toShort())
            }
            bufferIndex +=  lineFiller
        }
    }
}
