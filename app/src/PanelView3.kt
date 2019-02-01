package push2throttle

import java.awt.*

open class PanelView3(rect: Rectangle): PanelView(rect) {

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

    val a = Point("a", x[0], y[0])
    val b = a.branch("b", "SW", s0, y[0] + d0 * 3.0)
    val c = Point("c", 2 * d0, b.y)
    val d = Point("d", d0, c.y - d0)
    val e = Point("e", d0, a.y + d0)
    val f = Point("f", 2 * d0, a.y)
    val g = Point("g", ww - d0, y[0])

    val h = Point("h", x[1], y[0])
    val i = h.branch("i", "SE", s0, y[1])
    val j = Point("j", ww - d0, y[1])

    val k = Point("k", x[2], y[1])
    val l = k.branch("l", "SE", s0, y[2])
    val m = Point("m", x[3], y[2])
    val n = Point("n", m.x + d1, m.y)

    val o = m.branch("o", "NE", s1, y[1])
    val p = m.branch("p", "SW", s1, y[3])
    val q = Point("q", p.x - d2, p.y)
    val r = Point("r", p.x + d1, p.y)

    val y01mid = (y[0] + y[1]) / 2
    val s = Point("", x[4], y01mid).branch("s", "NW", s0, y[0])
    val t = Point("", x[4], y01mid).branch("t", "SE", s0, y[1])

    val u = Point("", x[5], y01mid).branch("u", "SW", s0, y[1])
    val v = Point("", x[5], y01mid).branch("v", "NE", s0, y[0])

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

    val A = TurnoutView("W1", 0, a, a.leg("W", s0), a.leg("SW", s0))
    val H = TurnoutView("W2", 1, h, h.leg("E", s0), h.leg("SE", s0))
    val K = TurnoutView("W3", 2, k, k.leg("E", s0), k.leg("SE", s0))
    val M1 = TurnoutView("W5", 12, m, m.leg("W", s1), m.leg("SW", s1))
    val M2 = TurnoutView("W6", 4, m, m.leg("E", s1), m.leg("NE", s1))
    val O = TurnoutView("W7", 5, o, o.leg("W", s1), o.leg("SW", s1))
    val P = TurnoutView("W4", 11, p, p.leg("E", s1), p.leg("NE", s1))
    val S = TurnoutView("W8", 6, s, s.leg("E", s0), s.leg("SE", s0))
    val T = TurnoutView("W9", 14, t, t.leg("W", s0), t.leg("NW", s0))
    val U = TurnoutView("W10", 15, u, u.leg("E", s0), u.leg("NE", s0))
    val V = TurnoutView("W11", 7, v, v.leg("W", s0), v.leg("SW", s0))

    final override val turnoutViews = mutableListOf( // TODO: why mutable?
        A, H, K, M1, M2, O, P, S, T, U, V
    )

    final override val railViews = mutableListOf(  // TODO: why mutable?
        RailView(arrayOf(A.pThrown, b, c, d, e, f, A.pClosed)),
        RailView(arrayOf(A.pCenter, H.pCenter)),
        RailView(arrayOf(H.pClosed, S.pCenter)),
        RailView(arrayOf(S.pClosed, V.pClosed)),
        RailView(arrayOf(V.pCenter, g)),
        RailView(arrayOf(H.pThrown, i, K.pCenter)),
        RailView(arrayOf(K.pClosed, O.pClosed)),
        RailView(arrayOf(O.pCenter, T.pClosed)),
        RailView(arrayOf(S.pThrown, T.pThrown)),
        RailView(arrayOf(U.pThrown, V.pThrown)),
        RailView(arrayOf(T.pCenter, U.pCenter)),
        RailView(arrayOf(U.pClosed, j)),
        RailView(arrayOf(K.pThrown, l, M1.pClosed)),
        RailView(arrayOf(M2.pThrown, O.pThrown)),
        RailView(arrayOf(M2.pClosed, n)),
        RailView(arrayOf(P.pThrown, M1.pThrown)),
        RailView(arrayOf(q, P.pCenter)),
        RailView(arrayOf(P.pClosed, r))
    )

    final override val graphPoints = enumeratePoints(turnoutViews, railViews)
    override val graph = buildGraph(graphPoints)

    override val pTitle = Point("title", 860.0, 130.0)
    override val title = "Bf Obendruff"

    init {
        update()
    }
}
