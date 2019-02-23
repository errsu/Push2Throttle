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

    private val EW = TurnoutView("DKW-W", 2, e, e.leg("W", s1), e.leg("NW", s1))
    private val EE = TurnoutView("DKW-E", 10, e, e.leg("E", s1), e.leg("SE", s1))
    private val F  = TurnoutView("WR", 11, f, f.leg("W", s1), f.leg("NW", s1))
    private val IJ = ThreeWaySwitchView("W3L", "W3R", 6, j, j.leg("SW", s1), j.leg("W", s1), j.leg("NW", s1))
    private val K  = TurnoutView("WL", 13, k, k.leg("E", s1), k.leg("NE", s1))

    final override val turnoutViews = listOf(EW, EE, F, IJ, K)

    final override val railViews = listOf(
            RailView(arrayOf(a, d, EW.pThrown)),
            RailView(arrayOf(b, EW.pClosed)),
            RailView(arrayOf(c, F.pClosed)),
            RailView(arrayOf(EE.pClosed, IJ.pMid)),
            RailView(arrayOf(EE.pThrown, F.pThrown)),
            RailView(arrayOf(F.pCenter, K.pCenter)),
            RailView(arrayOf(g, h, IJ.pRight)),
            RailView(arrayOf(K.pThrown, IJ.pLeft)),
            RailView(arrayOf(IJ.pCenter, l)),
            RailView(arrayOf(K.pClosed, m)))

    final override val graphPoints = enumeratePoints(turnoutViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Testanlage" // "Bf Obendruff"

    init {
        update()
    }
}
