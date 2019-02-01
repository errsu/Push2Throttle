package push2throttle

import java.awt.*
import java.io.File
import javax.imageio.ImageIO

// TODO: should not be a regular panel view
open class PanelView0(rect: Rectangle): PanelView(rect) {

    override val lines = arrayOf(arrayOf<Point>())
    final override val turnoutViews = listOf<TurnoutView>()
    final override val railViews = listOf<RailView>()
    final override val graphPoints = enumeratePoints(turnoutViews, railViews)
    override val graph = buildGraph(graphPoints)
    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Bf Obendruff"

    private val imagePath = "/images/PanelOverview.png"
    private val image = ImageIO.read(File(javaClass.getResource(imagePath).toURI()))

    init {
        update()
    }

    @Suppress("UNUSED_PARAMETER")
    override fun draw(g2: Graphics2D, frame: Int, display: Push2Display) {
        g2.drawImage(image, 0, 0, null)
    }
}
