package push2throttle

import java.awt.*

open class PanelView10(rect: Rectangle): PanelView(rect) {

    private val a = Point("a", x[0], y[2])
    private val z = Point("z", x[4], y[2])

    override val lines = arrayOf(arrayOf(a, z))

    final override val turnoutViews = listOf<TurnoutViewInterface>()
    final override val railViews = listOf(RailView(arrayOf(a, z)))
    final override val graphPoints = enumeratePoints(turnoutViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Pbf Süd"

    init {
        update()
    }
}
