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

    fun apply(buffer: ByteBuffer, frame: Int, firstLine: Int, lineCount: Int)
    {
        val shortBuffer = buffer.asShortBuffer()
        var bufferIndex = 0

        repeat(lineCount) {
            val line = firstLine + it
            repeat(displayWidth) {
                shortBuffer.put(bufferIndex++, rgb(31, 63, 0))
            }
            bufferIndex +=  lineFiller
        }
    }
}
