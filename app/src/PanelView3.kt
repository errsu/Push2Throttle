package push2throttle

import java.awt.*

open class PanelView3(rect: Rectangle): PanelView(rect) {

    // (ehemals obendruff)

    // other distances
    private val d1 = 160.0
    private val d2 = 280.0

    private val a = Point("a", x[0] + xl, y[0])
    private val b = a.branch("b", "SW", s0, y[0] + d0 * 3.0)
    private val c = Point("c", 2 * d0, b.y)
    private val d = Point("d", d0, c.y - d0)
    private val e = Point("e", d0, a.y + d0)
    private val f = Point("f", 2 * d0, a.y)
    private val g = Point("g", ww - d0, y[0])

    private val h = Point("h", x[1] - xl, y[0])
    private val i = h.branch("i", "SE", s0, y[1])
    private val j = Point("j", ww - d0, y[1])

    private val k = Point("k", x[2] - xl, y[1])
    private val l = k.branch("l", "SE", s0, y[2])
    private val m = Point("m", x[4], y[2])
    private val n = Point("n", m.x + d1, m.y)

    private val o = m.branch("o", "NE", s1, y[1])
    private val p = m.branch("p", "SW", s1, y[3])
    private val q = Point("q", p.x - d2, p.y)
    private val r = Point("r", p.x + d1, p.y)

    private val y01mid = (y[0] + y[1]) / 2
    private val s = Point("", x[6], y01mid).branch("s", "NW", s0, y[0])
    private val t = Point("", x[6], y01mid).branch("t", "SE", s0, y[1])

    private val u = Point("", x[7], y01mid).branch("u", "SW", s0, y[1])
    private val v = Point("", x[7], y01mid).branch("v", "NE", s0, y[0])

    init {
        g.preferredColor = 3
        j.preferredColor = 11
    }

    override val lines = arrayOf(
            arrayOf(a, b, c, d, e, f, g),
            arrayOf(h, i, j),
            arrayOf(k, l, n),
            arrayOf(o, p),
            arrayOf(q, r),
            arrayOf(s, t),
            arrayOf(u, v))

    private val A = SingleSwitchView("W1", 0, a, a.leg("W", s0), a.leg("SW", s0))
    private val H = SingleSwitchView("W2", 1, h, h.leg("E", s0), h.leg("SE", s0))
    private val K = SingleSwitchView("W3", 2, k, k.leg("E", s0), k.leg("SE", s0))
    private val M = DoubleSlipSwitchView("W5", "W6", 12, m, m.leg("SW", s1), m.leg("W", s1), m.leg("E", s1), m.leg("NE", s1))
    private val O = SingleSwitchView("W7", 5, o, o.leg("W", s1), o.leg("SW", s1))
    private val P = SingleSwitchView("W4", 11, p, p.leg("E", s1), p.leg("NE", s1))
    private val ST = CrossoverSwitchView("W8", "W9", 6, s, s.leg("E", s0), s.leg("SE", s0), t, t.leg("W", s0), t.leg("NW", s0))
    private val UV = CrossoverSwitchView("W10", "W11", 7, u, u.leg("E", s0), u.leg("NE", s0), v, v.leg("W", s0), v.leg("SW", s0))

    final override val switchViews = listOf(
        A, H, K, M, O, P, ST, UV
    )

    final override val railViews = listOf(
        RailView(arrayOf(A.pThrown, b, c, d, e, f, A.pClosed)),
        RailView(arrayOf(A.pCenter, H.pCenter)),
        RailView(arrayOf(H.pClosed, ST.pWestCenter)),
        RailView(arrayOf(ST.pWestClosed, UV.pEastClosed)),
        RailView(arrayOf(UV.pEastCenter, g)),
        RailView(arrayOf(H.pThrown, i, K.pCenter)),
        RailView(arrayOf(K.pClosed, O.pClosed)),
        RailView(arrayOf(O.pCenter, ST.pEastClosed)),
        RailView(arrayOf(ST.pWestThrown, ST.pEastThrown)),
        RailView(arrayOf(UV.pWestThrown, UV.pEastThrown)),
        RailView(arrayOf(ST.pEastCenter, UV.pWestCenter)),
        RailView(arrayOf(UV.pWestClosed, j)),
        RailView(arrayOf(K.pThrown, l, M.pWestClosed)),
        RailView(arrayOf(M.pEastThrown, O.pThrown)),
        RailView(arrayOf(M.pEastClosed, n)),
        RailView(arrayOf(P.pThrown, M.pWestThrown)),
        RailView(arrayOf(q, P.pCenter)),
        RailView(arrayOf(P.pClosed, r))
    )

    final override val graphPoints = enumeratePoints(switchViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 760.0, 130.0)
    override val title = "Sonnendorf/SBf Am Licht"

    init {
        update()
    }
}
