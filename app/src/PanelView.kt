package push2throttle

import java.awt.*
import java.awt.geom.GeneralPath

abstract class PanelView(rect: Rectangle): Push2View(rect) {

    //------------------------------------------------------------------------------------
    // turnout geometry

    data class Point(val name: String, val x: Double, val y: Double) {
        var n = 0
        var switchViewName: String? = null
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

    // Turnouts is what we have in JMRI, switches is what we see on Push2 surface.

    interface SwitchViewInterface {
        fun connectTurnouts(turnoutGetter: (String) -> Turnout?)
        fun disconnectTurnouts()
        fun addPointsToSet(set: MutableSet<Point>)
        fun addEdgeToGraph(graph: ConnectedTurnoutsGraph)
        fun turnoutGroup() : Push2TurnoutController.TurnoutGroup?
        val elementIndex: Int
    }

    // in case of left switch
    //                     --- center point
    //                     |
    //  closed point       V
    //          ====*======*====
    //          ====*====
    //  thrown point
    //
    // in case of right switch
    //                     --- center point
    //  thrown point       |
    //          ====*====  V
    //          ====*======*====
    //  closed point

    class SwitchView(private val turnoutName: String,
                     override val elementIndex: Int,
                     val pCenter: Point,
                     val pClosed: Point,
                     val pThrown: Point) : SwitchViewInterface {

        private var turnout: Turnout? = null

        init {
            pCenter.switchViewName = turnoutName
            pClosed.switchViewName = turnoutName
            pThrown.switchViewName = turnoutName
        }

        override fun turnoutGroup() : Push2TurnoutController.TurnoutGroup? {
            return if (turnout != null) {
                Push2TurnoutController.SingleSwitch(turnout!!)
            } else {
                null
            }
        }

        override fun connectTurnouts(turnoutGetter: (String) -> Turnout?) {
            turnout = turnoutGetter(turnoutName)
        }

        override fun disconnectTurnouts() {
            turnout = null
        }

        override fun addPointsToSet(set: MutableSet<Point>) {
            set.add(pCenter)
            set.add(pClosed)
            set.add(pThrown)
        }

        override fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
            val state = turnout?.state?.value ?: TurnoutState.UNKNOWN
            when (state) {
                TurnoutState.UNKNOWN -> {}
                TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pClosed.n)
                TurnoutState.THROWN -> graph.addEdge(pCenter.n, pThrown.n)
                TurnoutState.INCONSISTENT -> {}
            }
        }
    }

    //                           --- center point
    //         right point       |
    //  right turnout ====*====  V
    //  mid point     ====*======*====
    //  left turnout  ====*====
    //          left point

    class ThreeWaySwitchView(private val leftTurnoutName: String,
                             private val rightTurnoutName: String,
                             override val elementIndex: Int,
                             val pCenter: Point,
                             val pLeft: Point,
                             val pMid: Point,
                             val pRight: Point) : SwitchViewInterface {

        private var leftTurnout: Turnout? = null
        private var rightTurnout: Turnout? = null

        override fun turnoutGroup() : Push2TurnoutController.TurnoutGroup? {
            return if (leftTurnout != null && rightTurnout != null) {
                Push2TurnoutController.ThreeWaySwitch(leftTurnout!!, rightTurnout!!)
            } else {
                null
            }
        }

        init {
            val combinedName = "$leftTurnoutName##$rightTurnoutName"
            pCenter.switchViewName = combinedName
            pLeft.switchViewName = combinedName
            pMid.switchViewName = combinedName
            pRight.switchViewName = combinedName
        }

        override fun connectTurnouts(turnoutGetter: (String) -> Turnout?) {
            leftTurnout = turnoutGetter(leftTurnoutName)
            rightTurnout = turnoutGetter(rightTurnoutName)
        }

        override fun disconnectTurnouts() {
            leftTurnout = null
            rightTurnout = null
        }

        override fun addPointsToSet(set: MutableSet<Point>) {
            set.add(pCenter)
            set.add(pLeft)
            set.add(pMid)
            set.add(pRight)
        }

        override fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
            val stateLeft = leftTurnout?.state?.value ?: TurnoutState.UNKNOWN
            val stateRight = rightTurnout?.state?.value ?: TurnoutState.UNKNOWN
            when {
                // Note: There are some unclear states, for example if both sub-switches are thrown.
                // Their effect would depend on the order of them on the track, which is dangerous.
                // Therefore these states should be avoided.
                stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pMid.n)
                stateLeft == TurnoutState.THROWN && stateRight == TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pLeft.n)
                stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.THROWN -> graph.addEdge(pCenter.n, pRight.n)
                else -> {}
            }
        }
    }

    // Assuming the double-slip turnout consists of two right turnouts
    //                          ---- center point
    //                          |
    //  west thrown point       |
    //               ====*====  V       east closed point
    //  west turnout ====*======*======*==== east turnout
    //  west closed point          ====*====
    //                                  east thrown point

    class DoubleSlipSwitchView(private val westTurnoutName: String,
                               private val eastTurnoutName: String,
                               override val elementIndex: Int,
                               val pCenter: Point,
                               val pWestThrown: Point,
                               val pWestClosed: Point,
                               val pEastClosed: Point,
                               val pEastThrown: Point) : SwitchViewInterface {

        private var westTurnout: Turnout? = null
        private var eastTurnout: Turnout? = null

        override fun turnoutGroup() : Push2TurnoutController.TurnoutGroup? {
            return if (westTurnout != null && eastTurnout != null) {
                Push2TurnoutController.DoubleSlipSwitch(westTurnout!!, eastTurnout!!)
            } else {
                null
            }
        }

        init {
            val combinedName = "$westTurnoutName##$eastTurnoutName"
            pCenter.switchViewName = combinedName
            pWestThrown.switchViewName = combinedName
            pWestClosed.switchViewName = combinedName
            pEastClosed.switchViewName = combinedName
            pEastThrown.switchViewName = combinedName
        }

        override fun connectTurnouts(turnoutGetter: (String) -> Turnout?) {
            westTurnout = turnoutGetter(westTurnoutName)
            eastTurnout = turnoutGetter(eastTurnoutName)
        }

        override fun disconnectTurnouts() {
            westTurnout = null
            eastTurnout = null
        }

        override fun addPointsToSet(set: MutableSet<Point>) {
            set.add(pCenter)
            set.add(pWestThrown)
            set.add(pWestClosed)
            set.add(pEastClosed)
            set.add(pEastThrown)
        }

        override fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
            when (westTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
                TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pWestClosed.n)
                TurnoutState.THROWN -> graph.addEdge(pCenter.n, pWestThrown.n)
            }
            when (eastTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
                TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pEastClosed.n)
                TurnoutState.THROWN -> graph.addEdge(pCenter.n, pEastThrown.n)
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

    // branch length stretching (1:2)
    // slopes (x/y)
    val s0 = 3.0 // steep
    val s1 = 6.0 // easy
    // grid
    val y = arrayOf(20.0, 50.0, 95.0, 140.0)
    val x = arrayOf(60.0, 180.0, 300.0, 420.0, 540.0, 660.0, 780.0, 900.0)

    val d0 = 10.0 // distance to border
    val xl = 24.0 // horizontal turnout leg


    //------------------------------------------------------------------------------------
    // building graph

    fun buildGraph(points: Array<Point>): ConnectedTurnoutsGraph {
        val graph = ConnectedTurnoutsGraph()
        points.forEachIndexed { index, point ->
            graph.addVertex(point.name, index)
        }
        return graph
    }

    private fun updateGraph(graph: ConnectedTurnoutsGraph, switchViews: List<SwitchViewInterface>, railViews: List<RailView>) {
        graph.resetEdges()
        switchViews.forEach {
            it.addEdgeToGraph(graph)
        }
        railViews.forEach {
            it.addEdgesToGraph(graph)
        }
    }

    fun enumeratePoints(switchViews: List<SwitchViewInterface>, railViews: List<RailView>): Array<Point> {
        val pointSet = mutableSetOf<Point>()

        switchViews.forEach {
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
            if (p0.switchViewName != null && p0.switchViewName == p1.switchViewName) {
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

    abstract val switchViews: List<SwitchViewInterface>
    abstract val railViews: List<RailView>

    abstract val graphPoints: Array<Point>
    abstract val graph: ConnectedTurnoutsGraph

    private var components: List<List<Int>> = listOf()
    private var colors: List<Int> = listOf()
    abstract val lines: Array<Array<Point>>

    fun update() {
        updateGraph(graph, switchViews, railViews)
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
