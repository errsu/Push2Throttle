import java.awt.*
import java.awt.geom.GeneralPath

class PanelView(rect: Rectangle): Push2View(rect) {

    //------------------------------------------------------------------------------------
    // turnout geometry

    data class Point(val name: String, val x: Double, val y: Double) {
        var n = 0
        override fun toString() : String {
            return "P$n($name){${x.toInt()},${y.toInt()}}"
        }

        fun branch(name: String, direction: String, slope: Double, targetY: Double) : Point {
            return when (direction) {
                "NW" -> Point(name, x - slope * (y - targetY) / 2.0, targetY)
                "NE" -> Point(name, x + slope * (y - targetY) / 2.0, targetY)
                "SW" -> Point(name, x - slope * (targetY - y) / 2.0, targetY)
                "SE" -> Point(name, x + slope * (targetY - y) / 2.0, targetY)
                else -> this
            }
        }

        fun leg(direction: String, slope: Double) : Point {
            val len = 24.0
            val realSlope = slope / 2.0 // slope is same as in branch
            val shrink = 1.0 / Math.sqrt(1.0 + realSlope * realSlope)
            val deltaY = shrink * len
            val deltaX = realSlope * deltaY

            return when (direction) {
                "E"  -> Point(name + "_e", x + len, y)
                "W"  -> Point(name + "_w", x - len, y)
                "NW" -> Point(name + "_nw", x - deltaX, y - deltaY)
                "NE" -> Point(name + "_ne", x + deltaX, y - deltaY)
                "SW" -> Point(name + "_sw", x - deltaX, y + deltaY)
                "SE" -> Point(name + "_se", x + deltaX, y + deltaY)
                else -> this
            }
        }
    }

    class TurnoutView(val name: String,
                      val pCenter: Point,
                      val pClosed: Point,
                      val pThrown: Point) {
        // var state: Int = TurnoutState.UNKNOWN
        var jmriTurnout: JmriTurnout? = null

        fun addPointsToSet(set: MutableSet<Point>) {
            set.add(pCenter)
            set.add(pClosed)
            set.add(pThrown)
        }

        fun addEdgeToGraph(graph: Graph) {
            val state = jmriTurnout?.state?.value ?: TurnoutState.UNKNOWN
            when (state) {
                TurnoutState.UNKNOWN -> {}
                TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pClosed.n)
                TurnoutState.THROWN -> graph.addEdge(pCenter.n, pThrown.n)
                TurnoutState.INCONSISTENT -> {}
            }
        }
    }

    class RailView(val points: Array<Point>) {
        fun addPointsToSet(set: MutableSet<Point>) {
            set.addAll(points)
        }

        fun addEdgesToGraph(graph: Graph) {
            val pIter = points.iterator()
            var p0 = pIter.next()
            for (p1 in pIter) {
                graph.addEdge(p0.n, p1.n)
                p0 = p1
            }
        }
    }

    //------------------------------------------------------------------------------------
    // graph representation of selected path

    // We have an undirected graph where no vertex
    // has more than two edges connected.
    // Also, there are no cycles by construction.
    // The main goal is to find the connected components,
    // which are printed in one go with a single color.

    class Graph {
        class Vertex(val name: String, val n: Int) {
            override fun toString() : String {
                return "V$n($name){$adj1,$adj2,$visited}"
            }
            var adj1: Int = -1
            var adj2: Int = -1
            var visited = false
            fun valency() : Int {
                return if (adj1 == -1) 0 else 1 + if (adj2 == -1) 0 else 1
            }

            fun reset() {
                adj1 = -1
                adj2 = -1
                visited = false
            }
            fun addEdge(to: Int) {
                when {
                    adj1 == -1 -> adj1 = to
                    adj2 == -1 -> adj2 = to
                    else -> println("Graph Error: more than two edges for vertex $n")
                }
            }
        }

        val vertices = arrayListOf<Vertex>()

        // how to use:
        // Building time
        // - add all vertexes
        // - enumerate vertexes (?)
        // On turnout state changed
        // - reset edges
        // - add all active edges
        // - find components

        fun addVertex(name: String, n: Int) {
            vertices.add(Vertex(name, n))
        }

        fun resetEdges() {
            vertices.forEach { it.reset() }
        }

        fun addEdge(n0: Int, n1: Int) {
            vertices[n0].addEdge(n1)
            vertices[n1].addEdge(n0)
        }

        fun tracePathes(startIndex: Int, list: MutableList<Int>) {
            list.add(startIndex)
            vertices[startIndex].visited = true
            val next1 = vertices[startIndex].adj1
            if (next1 != -1 && !vertices[next1].visited) {
                tracePathes(next1, list)
            }
            val next2 = vertices[startIndex].adj2
            if (next2 != -1 && !vertices[next2].visited) {
                tracePathes(next2, list)
            }
        }

        fun findComponents() : List<List<Int>> {
            val components = mutableListOf<List<Int>>()

            while (true) {
                val list = mutableListOf<Int>()

                // find corner vertex (cycles are ignored)
                for (startIndex in vertices.indices) {
                    if (!vertices[startIndex].visited && vertices[startIndex].valency() == 1) {
                        // recursively add next vertices starting with corner
                        tracePathes(startIndex, list)
                        break
                    }
                }

                if (list.isNotEmpty()) {
                    components.add(list)
                }
                else {
                    return components
                }
            }
        }
    }

    fun buildGraph(points: Array<Point>): Graph {
        val graph = Graph()
        points.forEachIndexed { index, point ->
            graph.addVertex(point.name, index)
        }
        return graph
    }

    fun updateGraph(graph: Graph, turnoutViews: List<TurnoutView>, railViews: List<RailView>) {
        graph.resetEdges()
        turnoutViews.forEach {
            it.addEdgeToGraph(graph)
        }
        railViews.forEach {
            it.addEdgesToGraph(graph)
        }
    }

    fun enumeratePoints(turnoutViews: List<TurnoutView>, railViews: List<RailView>): Array<Point> {
        val pointSet = mutableSetOf<Point>()

        turnoutViews.forEach {
            it.addPointsToSet(pointSet)
        }
        railViews.forEach {
            it.addPointsToSet(pointSet)
        }

        val points = pointSet.toTypedArray()
        pointSet.forEachIndexed {
            index, point -> point.n = index
        }

        return points
    }

    //------------------------------------------------------------------------------------
    // Drawing

    private fun makePath(points: Array<Point>) : GeneralPath {
        val path = GeneralPath(GeneralPath.WIND_EVEN_ODD, points.size)
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        return path
    }

    private fun strokePath(g: Graphics2D, path: GeneralPath) {
        g.paint = Color.BLACK
        g.stroke = BasicStroke(6.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3.0f)
        g.draw(path)
    }

    private fun fillPath(g: Graphics2D, color: Color?, path: GeneralPath) {
        g.paint = color ?: Color.WHITE
        g.stroke = BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3.0f)
        g.draw(path)
    }

    // 1:2  branch length stretching

    val ww = rect.width.toDouble()
    val hh = rect.height.toDouble()

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


    //------------------------------------------------------------------------------------
    // building graph

    val A = TurnoutView("W1", a, a.leg("W", s0), a.leg("SW", s0))
    val H = TurnoutView("W2", h, h.leg("E", s0), h.leg("SE", s0))
    val K = TurnoutView("W3", k, k.leg("E", s0), k.leg("SE", s0))
    val M1 = TurnoutView("W5", m, m.leg("W", s1), m.leg("SW", s1))
    val M2 = TurnoutView("W6", m, m.leg("E", s1), m.leg("NE", s1))
    val O = TurnoutView("W7", o, o.leg("W", s1), o.leg("SW", s1))
    val P = TurnoutView("W4", p, p.leg("E", s1), p.leg("NE", s1))
    val S = TurnoutView("W8", s, s.leg("E", s0), s.leg("SE", s0))
    val T = TurnoutView("W9", t, t.leg("W", s0), t.leg("NW", s0))
    val U = TurnoutView("W10", u, u.leg("E", s0), u.leg("NE", s0))
    val V = TurnoutView("W11", v, v.leg("W", s0), v.leg("SW", s0))

    val turnoutViews = mutableListOf(
        A, H, K, M1, M2, O, P, S, T, U, V
    )

    val rails = mutableListOf(
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

    val points = enumeratePoints(turnoutViews, rails)
    val graph = buildGraph(points)

    var components: List<List<Int>> = listOf()

    init {
        // TODO: make sure we get a real initial turnout state, not UNKNOWN
        updateGraph(graph, turnoutViews, rails)
        components = graph.findComponents()
    }

    fun update() {
        updateGraph(graph, turnoutViews, rails)
        components = graph.findComponents()
    }

    //------------------------------------------------------------------------------------
    // drawing

    val lines = arrayOf(
            arrayOf(a, b, c, d, e, f, g),
            arrayOf(h, i, j),
            arrayOf(k, l, n),
            arrayOf(o, p),
            arrayOf(q, r),
            arrayOf(s, t),
            arrayOf(u, v))

    @Suppress("UNUSED_PARAMETER")
    override fun draw(g2: Graphics2D, frame: Int, display: Push2Display) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = display.push2Colors[20]
        g2.fillRect(0, 0, rect.width, rect.height)

        for (l in lines) {
            strokePath(g2, makePath(l))
        }

        val yel = display.push2Colors[7]
        val blu = display.push2Colors[125]

        for (path in components) {
            fillPath(g2,
                if (path.size > 2) yel else blu,
                makePath(path.map{points[it]}.toTypedArray())
            )
        }
    }
}
