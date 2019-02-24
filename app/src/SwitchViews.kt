package push2throttle

// Turnouts is what we have in JMRI, switches is what we see on Push2 surface.
// See doc/SwitchTypes.pdf for details.

interface SwitchViewInterface {
    fun connectTurnouts(turnoutGetter: (String) -> Turnout?)
    fun disconnectTurnouts()
    fun addPointsToSet(set: MutableSet<Point>)
    fun addEdgeToGraph(graph: ConnectedTurnoutsGraph)
    fun turnoutGroup() : TurnoutGroup?
    val elementIndex: Int
}

class SingleSwitchView(private val turnoutName: String,
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

    override fun turnoutGroup() : TurnoutGroup? {
        return if (turnout != null) {
            SingleSwitch(turnout!!)
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

class ThreeWaySwitchView(private val leftTurnoutName: String,
                         private val rightTurnoutName: String,
                         override val elementIndex: Int,
                         val pCenter: Point,
                         val pLeft: Point,
                         val pMid: Point,
                         val pRight: Point) : SwitchViewInterface {

    private var leftTurnout: Turnout? = null
    private var rightTurnout: Turnout? = null

    override fun turnoutGroup() : TurnoutGroup? {
        return if (leftTurnout != null && rightTurnout != null) {
            ThreeWaySwitch(leftTurnout!!, rightTurnout!!)
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

    override fun turnoutGroup() : TurnoutGroup? {
        return if (westTurnout != null && eastTurnout != null) {
            DoubleSlipSwitch(westTurnout!!, eastTurnout!!)
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

class CrossoverSwitchView(private val westTurnoutName: String,
                          private val eastTurnoutName: String,
                          override val elementIndex: Int,
                          val pWestCenter: Point,
                          val pWestClosed: Point,
                          val pWestThrown: Point,
                          val pEastCenter: Point,
                          val pEastClosed: Point,
                          val pEastThrown: Point) : SwitchViewInterface {

    private var westTurnout: Turnout? = null
    private var eastTurnout: Turnout? = null

    override fun turnoutGroup() : TurnoutGroup? {
        return if (westTurnout != null && eastTurnout != null) {
            CrossoverSwitch(westTurnout!!, eastTurnout!!)
        } else {
            null
        }
    }

    init {
        pWestCenter.switchViewName = westTurnoutName
        pWestThrown.switchViewName = westTurnoutName
        pWestClosed.switchViewName = westTurnoutName
        pEastCenter.switchViewName = eastTurnoutName
        pEastClosed.switchViewName = eastTurnoutName
        pEastThrown.switchViewName = eastTurnoutName
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
        set.add(pWestCenter)
        set.add(pWestThrown)
        set.add(pWestClosed)
        set.add(pEastCenter)
        set.add(pEastClosed)
        set.add(pEastThrown)
    }

    override fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
        when (westTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pWestCenter.n, pWestClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pWestCenter.n, pWestThrown.n)
        }
        when (eastTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pEastCenter.n, pEastClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pEastCenter.n, pEastThrown.n)
        }
    }
}
