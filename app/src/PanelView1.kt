package push2throttle

import java.awt.*

open class PanelView1(rect: Rectangle): PanelView(rect) {

    private val a = Point("a", d0, y[0])
    private val b = Point("b", d0, y[1])
    private val c = Point("c", d0, y[2])
    private val e = Point("e", x[2], y[1])
    private val g = Point("g", x[3], y[0])
    private val j = Point("j", x[6], y[1])
    private val l = Point("l", ww - d0, y[1])
    private val m = Point("m", ww - d0, y[2])

    private val d = e.branch("d", "NW", s1, a.y)
    private val f = e.branch("f", "SE", s1, c.y)

    private val h = j.branch("h", "NW", s1, g.y)
    private val k = j.branch("k", "SW", s1, m.y)

    init {
        l.preferredColor = 3
        m.preferredColor = 11
    }

    override val lines = arrayOf(
            arrayOf(a, d, f),
            arrayOf(b, l),
            arrayOf(c, m),
            arrayOf(g, h, j, k))

    private val E = DoubleSlipSwitchView("DKW-W", "DKW-E", 2, e, e.leg("NW", s1), e.leg("W", s1), e.leg("E", s1), e.leg("SE", s1))
    private val F = SingleSwitchView("WR", 11, f, f.leg("W", s1), f.leg("NW", s1))
    private val J = ThreeWaySwitchView("W3L", "W3R", 6, j, j.leg("SW", s1), j.leg("W", s1), j.leg("NW", s1))
    private val K = SingleSwitchView("WL", 13, k, k.leg("E", s1), k.leg("NE", s1))

    final override val switchViews = listOf(E, F, J, K)

    final override val railViews = listOf(
            RailView(arrayOf(a, d, E.pWestThrown)),
            RailView(arrayOf(b, E.pWestClosed)),
            RailView(arrayOf(c, F.pClosed)),
            RailView(arrayOf(E.pEastClosed, J.pMid)),
            RailView(arrayOf(E.pEastThrown, F.pThrown)),
            RailView(arrayOf(F.pCenter, K.pCenter)),
            RailView(arrayOf(g, h, J.pRight)),
            RailView(arrayOf(K.pThrown, J.pLeft)),
            RailView(arrayOf(J.pCenter, l)),
            RailView(arrayOf(K.pClosed, m)))

    final override val graphPoints = enumeratePoints(switchViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Testanlage" // "Bf Obendruff"

    init {
        update()
    }
}
