package push2throttle

import java.awt.*

open class PanelView1(rect: Rectangle): PanelView(rect) {

    private val a = Point("a", d0, y[0])
    private val b = Point("b", d0, y[1])
    private val c = Point("c", d0, y[2])
    private val e = Point("e", x[1], y[1])
    private val g = Point("g", x[2], y[0])
    private val i = Point("i", x[3], y[1])
    private val j = Point("j", x[4], y[1])
    private val l = Point("l", ww - d0, y[1])
    private val m = Point("m", ww - d0, y[2])

    private val d = e.branch("d", "NW", s0, a.y)
    private val f = e.branch("f", "SE", s0, c.y)

    private val h = i.branch("h", "NW", s0, g.y)
    private val k = j.branch("k", "SW", s0, m.y)

    init {
        l.preferredColor = 3
        m.preferredColor = 11
    }

    override val lines = arrayOf(
            arrayOf(a, d, f),
            arrayOf(b, l),
            arrayOf(c, m),
            arrayOf(g, h, i),
            arrayOf(j, k))

    private val EW = TurnoutView("DKW-W", 1, e, e.leg("W", s0), e.leg("NW", s0))
    private val EE = TurnoutView("DKW-E", 9, e, e.leg("E", s0), e.leg("SE", s0))
    private val F  = TurnoutView("WR", 10, f, f.leg("W", s0), f.leg("NW", s0))
    private val I  = TurnoutView("W3R", 4, i, i.leg("W", s0), i.leg("NW", s0))
    private val J  = TurnoutView("W3L", 6, j, j.leg("W", s0), j.leg("SW", s0))
    private val K  = TurnoutView("WL", 13, k, k.leg("E", s0), k.leg("NE", s0))

    final override val turnoutViews = listOf(EW, EE, F, I, J, K)

    final override val railViews = listOf(
            RailView(arrayOf(a, d, EW.pThrown)),
            RailView(arrayOf(b, EW.pClosed)),
            RailView(arrayOf(c, F.pClosed)),
            RailView(arrayOf(EE.pClosed, I.pClosed)),
            RailView(arrayOf(EE.pThrown, F.pThrown)),
            RailView(arrayOf(F.pCenter, K.pCenter)),
            RailView(arrayOf(g, h, I.pThrown)),
            RailView(arrayOf(I.pCenter, J.pClosed)),
            RailView(arrayOf(K.pThrown, J.pThrown)),
            RailView(arrayOf(J.pCenter, l)),
            RailView(arrayOf(K.pClosed, m)))

    final override val graphPoints = enumeratePoints(turnoutViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Testanlage" // "Bf Obendruff"

    init {
        update()
    }
}
