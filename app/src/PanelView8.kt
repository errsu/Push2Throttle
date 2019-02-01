package push2throttle

import java.awt.*

open class PanelView8(rect: Rectangle): PanelView(rect) {

    // 1:2  branch length stretching
    // slopes (x/y)
    val s0 = 3.0 // steep
    val s1 = 6.0 // easy

    // obendruff

    val y = arrayOf(20.0, 50.0, 95.0, 140.0)
    val x = arrayOf(80.0, 160.0, 280.0, 540.0, 780.0, 900.0)
    val d0 = 10.0
    val d1 = 160.0
    val d2 = 280.0

    val a = Point("a", x[5], y[0])
    val z = Point("z", x[0], y[3])

    override val lines = arrayOf(arrayOf(a, z))

    override val turnoutViews = listOf<TurnoutView>()
    override val railViews = listOf(RailView(arrayOf(a, z)))

    override val graphPoints = enumeratePoints(turnoutViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Bf Obendruff"

    init {
        update()
    }
}
