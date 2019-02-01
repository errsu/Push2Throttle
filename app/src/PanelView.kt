package push2throttle

import java.awt.*
import java.awt.geom.GeneralPath

abstract class PanelView(rect: Rectangle): Push2View(rect) {

    //------------------------------------------------------------------------------------
    // turnout geometry

    data class Point(val name: String, val x: Double, val y: Double) {
        var n = 0
        var turnoutName: String? = null
        var preferredColor = -1

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
                      val elementIndex: Int,
                      val pCenter: Point,
                      val pClosed: Point,
                      val pThrown: Point) {
        // var state: Int = TurnoutState.UNKNOWN
        var turnout: Turnout? = null

        init {
            pCenter.turnoutName = name
            pClosed.turnoutName = name
            pThrown.turnoutName = name
        }

        fun addPointsToSet(set: MutableSet<Point>) {
            set.add(pCenter)
            set.add(pClosed)
            set.add(pThrown)
        }

        fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
            val state = turnout?.state?.value ?: TurnoutState.UNKNOWN
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

        fun addEdgesToGraph(graph: ConnectedTurnoutsGraph) {
            val pIter = points.iterator()
            var p0 = pIter.next()
            for (p1 in pIter) {
                graph.addEdge(p0.n, p1.n)
                p0 = p1
            }
        }
    }

    //------------------------------------------------------------------------------------
    // building graph

    fun buildGraph(points: Array<Point>): ConnectedTurnoutsGraph {
        val graph = ConnectedTurnoutsGraph()
        points.forEachIndexed { index, point ->
            graph.addVertex(point.name, index)
        }
        return graph
    }

    fun updateGraph(graph: ConnectedTurnoutsGraph, turnoutViews: List<TurnoutView>, railViews: List<RailView>) {
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

    private fun determineColor(path: List<Int>, points: Array<Point>): Int {
        // see if we have a preferred color
        var preferredColor = -1
        for (pnum in path) {
            if (points[pnum].preferredColor != -1) {
                if (preferredColor == -1) {
                    preferredColor = points[pnum].preferredColor
                } else {
                    if (preferredColor != points[pnum].preferredColor) {
                        preferredColor = -2 // conflict
                    }
                }
            }
        }
        if (preferredColor >= 0) {
            return preferredColor
        }

        // see if we are crossing a turnout
        var crossingTurnoutCount = 0
        for (i in 0 until path.lastIndex) {
            val p0 = points[path[i]]
            val p1 = points[path[i+1]]
            if (p0.turnoutName != null && p0.turnoutName == p1.turnoutName) {
                crossingTurnoutCount++
            }
        }
        val yel = 7
        val blu = 125
        return if (crossingTurnoutCount > 0) yel else blu
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

    private val font24b  = Font("SansSerif", Font.BOLD, 24)

    private fun drawTitle(g: Graphics2D, title: String, color: Color?, point: Point) {
        g.font = font24b
        g.paint = color ?: Color.BLACK
        val wTitle = g.getFontMetrics(font24b).stringWidth(title).toFloat()
        val hTitle = g.getFontMetrics(font24b).height.toFloat()
        g.drawString(title, point.x.toFloat() - wTitle / 2.0f, point.y.toFloat() + hTitle / 2.0f)
    }

    val ww = rect.width.toDouble()
    val hh = rect.height.toDouble()

    abstract val pTitle: Point
    abstract val title: String

    abstract val turnoutViews: List<TurnoutView>
    abstract val railViews: List<RailView>

    abstract val graphPoints: Array<Point>
    abstract val graph: ConnectedTurnoutsGraph

    var components: List<List<Int>> = listOf()
    var colors: List<Int> = listOf()
    abstract val lines: Array<Array<Point>>

    fun update() {
        updateGraph(graph, turnoutViews, railViews)
        components = graph.findComponents()
        colors = components.map { determineColor(it, graphPoints)}
    }

    @Suppress("UNUSED_PARAMETER")
    override fun draw(g2: Graphics2D, frame: Int, display: Push2Display) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = display.push2Colors[20]
        g2.fillRect(0, 0, rect.width, rect.height)

        for (l in lines) {
            strokePath(g2, makePath(l))
        }

        components.forEachIndexed { index, path ->
            fillPath(g2,
                display.push2Colors[colors[index]],
                makePath(path.map{graphPoints[it]}.toTypedArray())
            )
        }

        drawTitle(g2, title, null, pTitle)
    }
}
