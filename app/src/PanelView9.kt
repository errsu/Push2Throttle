package push2throttle

import java.awt.*

open class PanelView9(rect: Rectangle): PanelView(rect) {

    private val a = Point("a", x[0], y[0])
    private val z = Point("z", x[6], y[3])

    override val lines = arrayOf(arrayOf(a, z))

    final override val switchViews = listOf<SwitchViewInterface>()
    final override val railViews = listOf(RailView(arrayOf(a, z)))
    final override val graphPoints = enumeratePoints(switchViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Gbf Süd"

    init {
        update()
    }
}
