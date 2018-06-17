import java.awt.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class ThrottleDisplay {

    private val displayWidth       = 960
    private val displayHeight      = 160

    private val font18 = Font("SansSerif", Font.BOLD, 18)
    private val font24 = Font("SansSerif", Font.PLAIN, 24)
    private val font48 = Font("SansSerif", Font.PLAIN, 48)

    private val trackColor = arrayListOf(
            1, 13, 18, 8, 12, 22, 3, 25)

    private val trackName = arrayListOf("", "", "", "", "", "", "", "")
    private val maxTrackSpeed = arrayListOf(80.0f, 80.0f, 80.0f, 80.0f, 80.0f, 80.0f, 80.0f, 80.0f)
    private val trackSpeed = arrayListOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    private val forward = arrayListOf(true, true, true, true, true, true, true, true)

    fun drawFrame(g: Graphics2D, frame: Int, displayContent: Push2DisplayContent) {
        val th = displayHeight
        val tw = (displayWidth / 8) // track width
        for (n in 0..7) {
            val trackRect = Rectangle(n * tw, 0, tw, th)
            g.paint = displayContent.push2Colors[trackColor[n]]
            g.fill(trackRect)

            g.font = font18
            g.paint = Color.BLACK
            val wName = g.getFontMetrics(font18).stringWidth(trackName[n])
            g.drawString(trackName[n], n * tw + (tw - wName) / 2, 20)

            val sliderRect = Rectangle(n * tw + 6, 30, 10, th - 38)
            g.paint = displayContent.invertedColor(displayContent.push2Colors[trackColor[n]]) // trackColor[n].darker()
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
            val speed = (trackSpeed[n] * maxTrackSpeed[n]).roundToInt().toString()
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

    fun rosterChanged(roster: Roster) {
        repeat(8) {
            trackName[it] = roster.name[it] ?: ""
            maxTrackSpeed[it] = roster.maxSpeed[it]?.toFloat() ?: 80.0f
        }
    }
}
