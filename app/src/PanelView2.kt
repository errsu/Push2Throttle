package push2throttle

import java.awt.*

open class PanelView2(rect: Rectangle): PanelView(rect) {

    private val y0 = (y[1] + y[2]) / 2.0
    private val y1 = y[2]
    private val y2 = (y[2] + y[3]) / 2.0
    private val y3 = y[3]

    private val a = Point("a", d0, y1)
    private val b = Point("b", d0, y3)

    private val hp1 = Point("hp1", x[0], y2)
    private val c = hp1.branch("c", "NW", s0, a.y)
    private val d = hp1.branch("d", "SW", s0, b.y)
    private val e = c.branch("e", "NE", s0, y0)
    private val f = hp1.branch("f", "NE", s0, a.y)
    private val g = hp1.branch("g", "SE", s0, b.y)

    private val hp2 = Point("hp2", x[1], hp1.y)
    private val i = hp2.branch("i", "NW", s0, a.y)
    private val j = hp2.branch("j", "SE", s0, b.y)
    private val h = i.branch("h", "NE", s0, e.y)

    private val hp3 = Point("hp3", x[2], (a.y + e.y) / 2.0)
    private val k = hp3.branch("k", "NW", s0, e.y)
    private val l = hp3.branch("l", "SE", s0, y2)

    private val n = Point("n", x[3], l.y)
    private val m = n.branch("m", "SW", s0, b.y)
    private val o = n.branch("o", "NE", s0, y1)

    private val hp4 = Point("hp4", x[6], (n.y + m.y) / 2.0)
    private val p = hp4.branch("p", "SW", s0, m.y)
    private val q = hp4.branch("q", "NE", s0, n.y)
    private val r = q.branch("r", "NW", s0, o.y)

    private val hp5 = Point("hp5", x[7], hp4.y)
    private val s = hp5.branch("s", "SW", s0, m.y)
    private val t = hp5.branch("t", "NE", s0, n.y)
    private val u = t.branch("u", "NW", s0, o.y)

    private val w = Point("w", ww - d0, l.y)
    private val v = Point("v", ww - d0, b.y)

    init {
        w.preferredColor = 3
        v.preferredColor = 11
    }

    private val A = DoubleCrossoverBranchView(
            "WA2", "WA1", "WA3", "WA5", "WA4", 8,
            c, c.leg("SE", s0), c.leg("E", s0), c.leg("NE", s0),
            f, f.leg("W", s0), f.leg("SW", s0),
            g, g.leg("W", s0), g.leg("NW", s0),
            d, d.leg("E", s0), d.leg("NE", s0))

    private val B = MiddlePassingView("WB2", "WB1", "WB3", 9,
            i, i.leg("NE", s0, 16.0), i.leg("SE", s0, 16.0),
            h, h.leg("W", s0, 16.0), h.leg("SW", s0, 16.0),
            j, j.leg("W", s0), j.leg("NW", s0))

    private val C = CrossoverBranchForwardView("WC2", "WC1", "WC3", 11,
            n, n.leg("W", s0, 16.0), n.leg("SW", s0, 16.0),
            m, m.leg("E", s0), m.leg("NE", s0),
            n.leg("E", s0, 16.0), n.leg("NE", s0, 16.0))

    private val D = ThreeWayCrossoverView("WD3", "WD2", "WD1", "WD4", 14,
            q, q.leg("SW", s0, 16.0), q.leg("W", s0, 16.0), q.leg("NW", s0, 16.0),
            r, r.leg("E", s0, 16.0), r.leg("SE", s0, 16.0),
            p, p.leg("E", s0, 16.0), p.leg("NE", s0, 16.0))

    private val E = CrossoverBranchBackwardView("WE2", "WE1", "WE3", 15,
            t, t.leg("SW", s0, 16.0), t.leg("W", s0, 16.0), t.leg("NW", s0, 16.0),
            s, s.leg("E", s0, 16.0), s.leg("NE", s0, 16.0))

    override val lines = arrayOf(
            arrayOf(a, i, j),
            arrayOf(b, v),
            arrayOf(c, g),
            arrayOf(d, f),
            arrayOf(i, h),
            arrayOf(c, e, k, l, w),
            arrayOf(m, n, o, u, t, s),
            arrayOf(p, q, r))

    final override val switchViews = listOf(A, B, C, D, E)

    final override val railViews = listOf(
            RailView(arrayOf(a, c)),
            RailView(arrayOf(A.pThirdThrown, A.pFirstThrown)),
            RailView(arrayOf(A.pThirdClosed, A.pSecondClosed)),
            RailView(arrayOf(A.pFirstClosed, A.pMid)),
            RailView(arrayOf(A.pLeft, A.pSecondThrown)),
            RailView(arrayOf(A.pRight, e, B.pNorthClosed)),
            RailView(arrayOf(g, B.pSouthClosed)),
            RailView(arrayOf(B.pThrown, B.pSouthThrown)),
            RailView(arrayOf(B.pClosed, B.pNorthThrown)),
            RailView(arrayOf(b, d)),
            RailView(arrayOf(f, i)),
            RailView(arrayOf(h, k, l)),
            RailView(arrayOf(l, C.pClosed)),
            RailView(arrayOf(C.pInThrown, C.pThrown)),
            RailView(arrayOf(C.pBranchClosed, D.pMid)),
            RailView(arrayOf(C.pInClosed, p)),
            RailView(arrayOf(D.pNorthThrown, D.pRight)),
            RailView(arrayOf(D.pSouthThrown, D.pLeft)),
            RailView(arrayOf(E.pInThrown, E.pLeft)),
            RailView(arrayOf(D.pNorthClosed, u, E.pRight)),
            RailView(arrayOf(q, E.pMid)),
            RailView(arrayOf(D.pSouthClosed, s)),
            RailView(arrayOf(E.pInClosed, v)),
            RailView(arrayOf(j, m)),
            RailView(arrayOf(C.pBranchThrown, o, r)),
            RailView(arrayOf(t, w)))

    final override val graphPoints = enumeratePoints(switchViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 820.0, 20.0)
    override val title = "Multi Switch Test"

    init {
        update()
    }
}
