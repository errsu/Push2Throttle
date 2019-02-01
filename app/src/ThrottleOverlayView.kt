package push2throttle

import java.awt.*

class ThrottleOverlayView(rect: Rectangle): Push2View(rect) {

    private val font18  = Font("SansSerif", Font.PLAIN, 18)

    private var overlayCount = 0
    private var overlayText : String? = null
    private var overlayX: Int = 0

    fun showText(column: Int, name: String, action: Boolean?) {
        val state = when(action) {
            true -> "an"
            false -> "aus"
            null -> ""
        }
        overlayCount = 120
        overlayText = "$name $state"
        overlayX = rect.width * (2 * column + 1) / 16
    }

    fun alphaForOverlayCount(count: Int) : Int {
        return when {
            count > 30 -> 224
            else -> 224 * count / 30
        }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun draw(g2: Graphics2D, frame: Int, display: Push2Display) {
        if (overlayCount > 0) {
            overlayCount--
            val wText = g2.getFontMetrics(font18).stringWidth(overlayText)
            g2.paint = Color(0, 0, 0, alphaForOverlayCount(overlayCount))
            val yBaseline = rect.height - 19
            val insetx = 10
            val x = (overlayX + 8 - wText / 2 - insetx).coerceIn(0, rect.width - wText - 2 * insetx)
            g2.fill(Rectangle(x, yBaseline - 18, wText + 2 * insetx, 24))
            g2.font = font18
            g2.paint = Color(255, 255, 255, alphaForOverlayCount(overlayCount))
            g2.drawString(overlayText, x + insetx, yBaseline)
        }
    }
}
