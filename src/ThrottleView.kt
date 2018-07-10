import java.awt.*
import kotlin.math.roundToInt

class ThrottleView(rect: Rectangle): Push2View(rect) {

    var throttle: JmriThrottle? = null

    private val font18 = Font("SansSerif", Font.BOLD, 18)
    private val font24 = Font("SansSerif", Font.PLAIN, 24)
    private val font48 = Font("SansSerif", Font.PLAIN, 48)

    override fun draw(g: Graphics2D, frame: Int, display: Push2Display) {
        val loco = throttle?.loco ?: return
        val trackColorIndex = loco.color.value
        val trackColor = display.push2Colors.getOrDefault(trackColorIndex, Color.GRAY)
        g.paint = trackColor
        g.fill(Rectangle(0, 0, rect.width, rect.height))

        g.font = font18
        g.paint = Color.BLACK
        val locoName = loco.name
        val wName = g.getFontMetrics(font18).stringWidth(locoName)
        g.drawString(locoName, (rect.width - wName) / 2, 20)

        val sliderRect = Rectangle(6, 30, 10, rect.height - 38)
        g.paint = display.invertedColor(trackColor)
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
        var locoSpeed = loco.speed.value
        val fullStop = locoSpeed < 0.0f
        if (fullStop) locoSpeed = 0.0f
        val speedTriangleY = sliderRect.y + 1 +
                ((sliderRect.height - 2) * (1.0f - locoSpeed)).roundToInt()
        val speedTriangleX = sliderRect.x + sliderRect.width + 2
        triangle.translate(speedTriangleX, speedTriangleY)
        g.fill(triangle)

        // TODO: add "km/h"

        if (fullStop) {
            g.font = font24
            g.paint = Color.BLACK
            val speed = "STOP!"
            val wSpeed = g.getFontMetrics(g.font).stringWidth(speed)
            g.drawString(speed, rect.width - wSpeed - 10, 30 + (rect.height - 30 - 10) / 2)
        } else {
            g.font = font48
            g.paint = Color.BLACK
            val maxSpeed = loco.maxSpeed.value
            val speed = if (fullStop) "STOP!" else (locoSpeed * maxSpeed.toFloat()).roundToInt().toString()
            val wSpeed = g.getFontMetrics(font48).stringWidth(speed)
            g.drawString(speed, rect.width - wSpeed - 10, 30 + (rect.height - 30 - 10) / 2)
        }

        val forward = loco.forward.value
        if (!forward) {
            val reverseRect = Rectangle(36, rect.height - 40, rect.width - 42, 18)
            g.paint = Color.WHITE
            g.fill(reverseRect)
            g.paint = Color.BLACK
            g.draw(reverseRect)
            g.font = font18
            g.paint = Color.RED
            g.drawString("reverse", 42, rect.height - 25)
        }
        g.font = font24
        g.paint = Color(64, 64, 64)
    }
}
