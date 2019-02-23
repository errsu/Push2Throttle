package push2throttle

import java.awt.*

open class PanelView11(rect: Rectangle): PanelView(rect) {

    private val a = Point("a", x[0], y[0])
    private val n = Point("n", x[0], y[3])
    private val z = Point("z", x[5], y[3])

    override val lines = arrayOf(arrayOf(a, n, z))

    final override val switchViews = listOf<SwitchViewInterface>()
    final override val railViews = listOf(RailView(arrayOf(a, n, z)))
    final override val graphPoints = enumeratePoints(switchViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Sbf Süd"

    init {
        update()
    }
}
