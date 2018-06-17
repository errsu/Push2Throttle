import java.awt.Graphics2D

class PanelDisplay {

    private val displayWidth  = 960
    private val displayHeight = 160

    fun drawFrame(g: Graphics2D, frame: Int, displayContent: Push2DisplayContent) {
        g.paint = displayContent.push2Colors[(frame / 60) % 27]
        g.fillRect(0, 0, displayWidth, displayHeight)
    }
}
