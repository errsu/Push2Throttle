import java.awt.*
import kotlin.math.roundToInt

class ThrottleView(rect: Rectangle, val selectionManager: SelectionManager): Push2View(rect) {

    var throttle: JmriThrottle? = null

    // TODO: centralize fonts and color names
    private val font18  = Font("SansSerif", Font.PLAIN, 18)
    private val font18b = Font("SansSerif", Font.BOLD,  18)
    private val font24  = Font("SansSerif", Font.PLAIN, 24)
    private val font48  = Font("SansSerif", Font.PLAIN, 48)

    override fun draw(g2: Graphics2D, frame: Int, display: Push2Display) {
        val loco = throttle?.loco ?: return
        val trackColorIndex = loco.color.value
        val trackColor = display.push2Colors.getOrDefault(trackColorIndex, Color.GRAY)
        g2.paint = trackColor
        g2.fill(Rectangle(0, 0, rect.width, rect.height))
        if (selectionManager.getSelectedColumn() == selectionManager.getThrottleColumn(throttle!!)) {
            g2.paint = display.invertedColor(trackColor)
            g2.fill(Rectangle(0, 0, rect.width, 26))
        }

        g2.font = font18b
        g2.paint = Color.BLACK
        val locoName = loco.name
        val wName = g2.getFontMetrics(font18b).stringWidth(locoName)
        g2.drawString(locoName, (rect.width - wName) / 2, 20)

        val sliderRect = Rectangle(6, 30, 10, rect.height - 38)
        g2.paint = display.invertedColor(trackColor)
        g2.fill(sliderRect)
        g2.paint = Color.BLACK
        g2.draw(sliderRect)

        repeat(10) {
            val y = sliderRect.y + sliderRect.height * it / 10
            g2.drawLine(sliderRect.x, y, sliderRect.x + sliderRect.width / 2, y)
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
        g2.fill(triangle)

        if (fullStop) {
            g2.font = font24
            g2.paint = Color.BLACK
            val speed = "STOP!"
            val wSpeed = g2.getFontMetrics(g2.font).stringWidth(speed)
            g2.drawString(speed, rect.width - wSpeed - 10, 30 + (rect.height - 30 - 10) / 2)
        } else {
            g2.font = font48
            g2.paint = Color.BLACK
            val maxSpeed = loco.maxSpeed.value
            val speed = (locoSpeed * maxSpeed.toFloat()).roundToInt().toString()
            val wSpeed = g2.getFontMetrics(font48).stringWidth(speed)
            val speedY = 30 + (rect.height - 30 - 10) / 2
            g2.drawString(speed, rect.width - wSpeed - 10, speedY)

            g2.font = font18
            g2.paint = Color.BLACK
            val kmh = "km/h"
            val wKmh = g2.getFontMetrics(font18).stringWidth(kmh)
            g2.drawString(kmh, rect.width - wKmh - 12, speedY + 23)
        }

        val forward = loco.forward.value
        if (!forward) {
            val reverseRect = Rectangle(36, rect.height - 34, rect.width - 42, 18)
            g2.paint = Color.WHITE
            g2.fill(reverseRect)
            g2.paint = Color.BLACK
            g2.draw(reverseRect)
            g2.font = font18b
            g2.paint = Color.RED
            g2.drawString("reverse", 42, rect.height - 19)
        }
        g2.font = font24
        g2.paint = Color(64, 64, 64)
    }
}
