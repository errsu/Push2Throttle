package push2throttle

import java.awt.*

open class PanelView12(rect: Rectangle): PanelView(rect) {

    private val a = Point("a", x[0], y[0])
    private val b = Point("b", x[4], y[0])
    private val p = Point("p", x[1], y[2])
    private val z = Point("z", x[2], y[2])

    override val lines = arrayOf(arrayOf(a, b), arrayOf(p, z))

    final override val turnoutViews = listOf<TurnoutView>()
    final override val railViews = listOf(RailView(arrayOf(a, b)), RailView(arrayOf(p, z)))
    final override val graphPoints = enumeratePoints(turnoutViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 760.0, 130.0)
    override val title = "Südschleife Oben"

    init {
        update()
    }
}
