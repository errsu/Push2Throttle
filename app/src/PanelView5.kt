package push2throttle

import java.awt.*

open class PanelView5(rect: Rectangle): PanelView(rect) {

    private val a = Point("a", x[2], y[0])
    private val m = Point("m", x[1], y[0])
    private val n = Point("n", x[1], y[3])
    private val z = Point("z", x[3], y[3])

    override val lines = arrayOf(arrayOf(a, m, n, z))

    final override val turnoutViews = listOf<TurnoutView>()
    final override val railViews = listOf(RailView(arrayOf(a, m, n, z)))
    final override val graphPoints = enumeratePoints(turnoutViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Mittelpunkt NO"

    init {
        update()
    }
}
