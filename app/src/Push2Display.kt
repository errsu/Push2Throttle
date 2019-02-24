package push2throttle

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
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

    private val viewList = mutableListOf<Push2View>()

    val driverStateMutex = Mutex()

    private fun drawFrame(frame: Int) {
        val display = this
        runBlocking {
            display.driverStateMutex.withLock {
                g.paint = Push2Colors.defaultDisplayBackgroundColor
                g.fill(Rectangle(0, 0, width, height))
                val iterator = viewList.iterator()
                for (view in iterator) {
                    val gView = g.create() as Graphics2D
                    gView.translate(view.rect.x, view.rect.y)
                    view.draw(gView, frame, display)
                }
            }
        }
    }

    fun addView(view: Push2View) {
        viewList.add(view)
    }

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
