import java.awt.Graphics2D
import java.io.File
import javax.imageio.ImageIO

class PanelDisplay {

    private val displayWidth  = 960
    private val displayHeight = 160

    private val img = ImageIO.read(File("Panel.png"))

    @Suppress("UNUSED_PARAMETER")
    fun drawFrame(g: Graphics2D, frame: Int, displayContent: Push2DisplayContent) {
//        g.paint = displayContent.push2Colors[(frame / 60) % 27]
//        g.fillRect(0, 0, displayWidth, displayHeight)
        g.drawImage(img, 0, 0, null)
    }
}
