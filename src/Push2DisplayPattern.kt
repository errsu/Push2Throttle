import javafx.beans.property.SimpleStringProperty
import tornadofx.getValue
import tornadofx.setValue
import java.nio.ByteBuffer

@Suppress("UNUSED_PARAMETER")
class Push2DisplayPattern() {

    val patterns = listOf(
            "black", "white", "red", "green", "blue",
            "moving black line", "scales", "moving picture",
            "chessboard", "frame", "pin stripes", "color")

    val selectedPatternProperty = SimpleStringProperty(this, "pattern", "moving black line")
    private var selectedPattern by selectedPatternProperty
    private var patternNumber = patterns.indexOf(selectedPattern)

    init {
        selectedPatternProperty.addListener {
            _, _, new -> patternNumber = patterns.indexOf(new)
        }
    }

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

    private val scalesColor = arrayOf(
        red(31), red(30), red(29), red(28), red(27), red(26), red(25), red(24), red(23), red(22), red(21), red(20), red(19), red(18), red(17), red(16),
        red(15), red(14), red(13), red(12), red(11), red(10), red( 9), red( 8), red( 7), red( 6), red( 5), red( 4), red( 3), red( 2), red( 1), red( 0),

        grn(63), grn(62), grn(61), grn(60), grn(59), grn(58), grn(57), grn(56), grn(55), grn(54), grn(53), grn(52), grn(51), grn(50), grn(49), grn(48),
        grn(47), grn(46), grn(45), grn(44), grn(43), grn(42), grn(41), grn(40), grn(39), grn(38), grn(37), grn(36), grn(35), grn(34), grn(33), grn(32),
        grn(31), grn(30), grn(29), grn(28), grn(27), grn(26), grn(25), grn(24), grn(23), grn(22), grn(21), grn(20), grn(19), grn(18), grn(17), grn(16),
        grn(15), grn(14), grn(13), grn(12), grn(11), grn(10), grn( 9), grn( 8), grn( 7), grn( 6), grn( 5), grn( 4), grn( 3), grn( 2), grn( 1), grn( 0),

        blu(31), blu(30), blu(29), blu(28), blu(27), blu(26), blu(25), blu(24), blu(23), blu(22), blu(21), blu(20), blu(19), blu(18), blu(17), blu(16),
        blu(15), blu(14), blu(13), blu(12), blu(11), blu(10), blu( 9), blu( 8), blu( 7), blu( 6), blu( 5), blu( 4), blu( 3), blu( 2), blu( 1), blu( 0),

        gry(31), gry(30), gry(29), gry(28), gry(27), gry(26), gry(25), gry(24), gry(23), gry(22), gry(21), gry(20), gry(19), gry(18), gry(17), gry(16),
        gry(15), gry(14), gry(13), gry(12), gry(11), gry(10), gry( 9), gry( 8), gry( 7), gry( 6), gry( 5), gry( 4), gry( 3), gry( 2), gry( 1), gry( 0))

    private fun patternBlack(frame: Int, line: Int, pixel: Int) : Short {
        return black
    }

    private fun patternWhite(frame: Int, line: Int, pixel: Int) : Short {
        return white
    }

    private fun patternRed(frame: Int, line: Int, pixel: Int) : Short {
        return red(31)
    }

    private fun patternGreen(frame: Int, line: Int, pixel: Int) : Short {
        return grn(63)
    }

    private fun patternBlue(frame: Int, line: Int, pixel: Int) : Short {
        return blu(31)
    }

    private fun patternMovingBlackLine(frame: Int, line: Int, pixel: Int) : Short
    {
        return if (pixel == (frame % displayWidth)) black else white
    }

    private fun patternScales(frame: Int, line: Int, pixel: Int) : Short {
        return scalesColor[pixel/6]
    }

    private fun patternMovingPicture(frame: Int, line: Int, pixel: Int) : Short {
        val S = 240
        val pixelS = pixel % S
        val offset = ((frame % displayWidth) * 10) % S

        fun inRange(point: Int, start: Int, len:Int, interval:Int): Boolean {
            var r = (point - start) % interval
            if (r < 0) r += interval
            return r < len
        }

        if (10 < line && line < 30 && inRange(pixelS, offset +   0, 20, 240))
        {
            return blu(31)
        }
        else if (30 < line && line < 50 && inRange(pixelS, offset +  40, 20, 240))
        {
            return grn(63)
        }
        else if (50 < line && line < 70 && inRange(pixelS, offset +  80, 20, 240))
        {
            return red(31)
        }
        else if (70 < line && line < 90 && ( inRange(pixelS, offset +   0, 20, 240)
                        || inRange(pixelS, offset +  40, 20, 240)
                        || inRange(pixelS, offset +  80, 20, 240)
                        || inRange(pixelS, offset + 120, 20, 240)))
        {
            return white
        }
        else if (90 < line && line < 110 && inRange(pixelS, offset +  80, 20, 240))
        {
            return red(31)
        }
        else if (110 < line && line < 130 && inRange(pixelS, offset +  40, 20, 240))
        {
            return grn(63)
        }
        else if (130 < line && line < 150 && inRange(pixelS, offset +   0, 20, 240))
        {
            return blu(31)
        }
        else
        {
            return black
        }
    }

    private fun patternChessboard(frame: Int, line: Int, pixel: Int) : Short {
        val sizes = arrayOf(2, 4, 8, 16)
        val size = sizes[(pixel / 80) % 4]
        val hsize = size / 2;

        return if ((line % size < hsize) == (pixel % size < hsize)) black else white
    }

    private fun patternFrame(frame: Int, line: Int, pixel: Int) : Short {
        return if (pixel == 0 || pixel == displayWidth - 1 || line == 0 || line == displayHeight - 1) white else black
    }

    private fun patternPinstripes(frame: Int, line: Int, pixel: Int) : Short {
        return if (pixel % 8 == 4) white else black
    }

    private fun patternColor(frame: Int, line: Int, pixel: Int) : Short {
        val phase = (frame / 2) % 256
        return when {
            phase < 32  -> red(phase)
            phase < 64  -> red(63 - phase)
            phase < 96  -> grn(2 * (phase - 64))
            phase < 128 -> grn(2 * (127 - phase))
            phase < 160 -> blu(phase - 128)
            phase < 192 -> blu(191 - phase)
            phase < 224 -> gry(phase - 192)
            else        -> gry(255 - phase)
        }
    }

    private val patternGenerators : Array<(Int, Int, Int) -> Short> = arrayOf(
        ::patternBlack,
        ::patternWhite,
        ::patternRed,
        ::patternGreen,
        ::patternBlue,
        ::patternMovingBlackLine,
        ::patternScales,
        ::patternMovingPicture,
        ::patternChessboard,
        ::patternFrame,
        ::patternPinstripes,
        ::patternColor
    )

    fun apply(buffer: ByteBuffer, frame: Int, firstLine: Int, lineCount: Int)
    {
        val patternFun = patternGenerators[patternNumber]
        val shortBuffer = buffer.asShortBuffer()
        var bufferIndex = 0

        repeat(lineCount) {
            val line = firstLine + it
            repeat(displayWidth) {
                shortBuffer.put(bufferIndex++, patternFun(frame, line, it))
            }
            bufferIndex +=  lineFiller
        }
    }
}
