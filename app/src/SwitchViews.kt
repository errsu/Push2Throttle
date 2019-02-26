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

class CrossoverBranchForwardView(
        private val turnoutName: String,
        private val inTurnoutName: String,
        private val branchTurnoutName: String,
        override val elementIndex: Int,
        val pCenter: Point,
        val pClosed: Point,
        val pThrown: Point,
        val pInCenter: Point,
        val pInClosed: Point,
        val pInThrown: Point,
        val pBranchClosed: Point,
        val pBranchThrown: Point) : SwitchViewInterface {

    private var turnout: Turnout? = null
    private var inTurnout: Turnout? = null
    private var branchTurnout: Turnout? = null

    override fun turnoutGroup() : TurnoutGroup? {
        return if (turnout != null && inTurnout != null && branchTurnout != null) {
            CrossoverBranchForward(turnout!!, inTurnout!!, branchTurnout!!)
        } else {
            null
        }
    }

    init {
        val crossoverName = "$turnoutName##$branchTurnoutName"
        pCenter.switchViewName = crossoverName
        pThrown.switchViewName = crossoverName
        pClosed.switchViewName = crossoverName
        pInCenter.switchViewName = inTurnoutName
        pInClosed.switchViewName = inTurnoutName
        pInThrown.switchViewName = inTurnoutName
        pBranchClosed.switchViewName = crossoverName
        pBranchThrown.switchViewName = crossoverName
    }

    override fun connectTurnouts(turnoutGetter: (String) -> Turnout?) {
        turnout = turnoutGetter(turnoutName)
        inTurnout = turnoutGetter(inTurnoutName)
        branchTurnout = turnoutGetter(branchTurnoutName)
    }

    override fun disconnectTurnouts() {
        turnout = null
        inTurnout = null
        branchTurnout = null
    }

    override fun addPointsToSet(set: MutableSet<Point>) {
        set.add(pCenter)
        set.add(pThrown)
        set.add(pClosed)
        set.add(pInCenter)
        set.add(pInClosed)
        set.add(pInThrown)
        set.add(pBranchClosed)
        set.add(pBranchThrown)
    }

    override fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
        when (turnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pCenter.n, pThrown.n)
        }
        when (inTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pInCenter.n, pInClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pInCenter.n, pInThrown.n)
        }
        when (branchTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pBranchClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pCenter.n, pBranchThrown.n)
        }
    }
}

class CrossoverBranchBackwardView(
        private val leftTurnoutName: String,
        private val rightTurnoutName: String,
        private val inTurnoutName: String,
        override val elementIndex: Int,
        val pCenter: Point,
        val pLeft: Point,
        val pMid: Point,
        val pRight: Point,
        val pInCenter: Point,
        val pInClosed: Point,
        val pInThrown: Point) : SwitchViewInterface {

    private var leftTurnout: Turnout? = null
    private var rightTurnout: Turnout? = null
    private var inTurnout: Turnout? = null

    override fun turnoutGroup() : TurnoutGroup? {
        return if (leftTurnout != null && rightTurnout != null && inTurnout != null) {
            CrossoverBranchBackward(leftTurnout!!, rightTurnout!!, inTurnout!!)
        } else {
            null
        }
    }

    init {
        val threeWayName = "$leftTurnoutName##$rightTurnoutName"
        pCenter.switchViewName = threeWayName
        pLeft.switchViewName = threeWayName
        pMid.switchViewName = threeWayName
        pRight.switchViewName = threeWayName
        pInCenter.switchViewName = inTurnoutName
        pInClosed.switchViewName = inTurnoutName
        pInThrown.switchViewName = inTurnoutName
    }

    override fun connectTurnouts(turnoutGetter: (String) -> Turnout?) {
        leftTurnout = turnoutGetter(leftTurnoutName)
        rightTurnout = turnoutGetter(rightTurnoutName)
        inTurnout = turnoutGetter(inTurnoutName)
    }

    override fun disconnectTurnouts() {
        leftTurnout = null
        rightTurnout = null
        inTurnout = null
    }

    override fun addPointsToSet(set: MutableSet<Point>) {
        set.add(pCenter)
        set.add(pLeft)
        set.add(pMid)
        set.add(pRight)
        set.add(pInCenter)
        set.add(pInClosed)
        set.add(pInThrown)
    }

    override fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
        val stateLeft = leftTurnout?.state?.value ?: TurnoutState.UNKNOWN
        val stateRight = rightTurnout?.state?.value ?: TurnoutState.UNKNOWN
        when {
            stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pMid.n)
            stateLeft == TurnoutState.THROWN && stateRight == TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pLeft.n)
            stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.THROWN -> graph.addEdge(pCenter.n, pRight.n)
            else -> {}
        }
        when (inTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pInCenter.n, pInClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pInCenter.n, pInThrown.n)
        }
    }
}

class MiddlePassingView(
        private val midTurnoutName: String,
        private val northTurnoutName: String,
        private val southTurnoutName: String,
        override val elementIndex: Int,
        val pCenter: Point,
        val pClosed: Point,
        val pThrown: Point,
        val pNorthCenter: Point,
        val pNorthClosed: Point,
        val pNorthThrown: Point,
        val pSouthCenter: Point,
        val pSouthClosed: Point,
        val pSouthThrown: Point) : SwitchViewInterface {

    private var midTurnout: Turnout? = null
    private var northTurnout: Turnout? = null
    private var southTurnout: Turnout? = null

    override fun turnoutGroup() : TurnoutGroup? {
        return if (midTurnout != null && northTurnout != null && southTurnout != null) {
            MiddlePassing(midTurnout!!, northTurnout!!, southTurnout!!)
        } else {
            null
        }
    }

    init {
        pCenter.switchViewName = midTurnoutName
        pClosed.switchViewName = midTurnoutName
        pThrown.switchViewName = midTurnoutName
        pNorthCenter.switchViewName = northTurnoutName
        pNorthClosed.switchViewName = northTurnoutName
        pNorthThrown.switchViewName = northTurnoutName
        pSouthCenter.switchViewName = southTurnoutName
        pSouthClosed.switchViewName = southTurnoutName
        pSouthThrown.switchViewName = southTurnoutName
    }

    override fun connectTurnouts(turnoutGetter: (String) -> Turnout?) {
        midTurnout = turnoutGetter(midTurnoutName)
        northTurnout = turnoutGetter(northTurnoutName)
        southTurnout = turnoutGetter(southTurnoutName)
    }

    override fun disconnectTurnouts() {
        midTurnout = null
        northTurnout = null
        southTurnout = null
    }

    override fun addPointsToSet(set: MutableSet<Point>) {
        set.add(pCenter)
        set.add(pClosed)
        set.add(pThrown)
        set.add(pNorthCenter)
        set.add(pNorthClosed)
        set.add(pNorthThrown)
        set.add(pSouthCenter)
        set.add(pSouthClosed)
        set.add(pSouthThrown)
    }

    override fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
        when (midTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pCenter.n, pThrown.n)
        }
        when (northTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pNorthCenter.n, pNorthClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pNorthCenter.n, pNorthThrown.n)
        }
        when (southTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pSouthCenter.n, pSouthClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pSouthCenter.n, pSouthThrown.n)
        }
    }
}

class DoubleCrossoverBranchView(
        private val leftTurnoutName: String,
        private val rightTurnoutName: String,
        private val firstTurnoutName: String,
        private val secondTurnoutName: String,
        private val thirdTurnoutName: String,
        override val elementIndex: Int,
        val pCenter: Point,
        val pLeft: Point,
        val pMid: Point,
        val pRight: Point,
        val pFirstCenter: Point,
        val pFirstClosed: Point,
        val pFirstThrown: Point,
        val pSecondCenter: Point,
        val pSecondClosed: Point,
        val pSecondThrown: Point,
        val pThirdCenter: Point,
        val pThirdClosed: Point,
        val pThirdThrown: Point) : SwitchViewInterface {

    private var leftTurnout: Turnout? = null
    private var rightTurnout: Turnout? = null
    private var firstTurnout: Turnout? = null
    private var secondTurnout: Turnout? = null
    private var thirdTurnout: Turnout? = null

    override fun turnoutGroup() : TurnoutGroup? {
        return if (leftTurnout != null && rightTurnout != null &&
                   firstTurnout != null && secondTurnout != null && thirdTurnout != null) {
            DoubleCrossoverBranch(leftTurnout!!, rightTurnout!!, firstTurnout!!, secondTurnout!!, thirdTurnout!!)
        } else {
            null
        }
    }

    init {
        val threeWaySwitchName = "$leftTurnoutName##$rightTurnoutName"
        pCenter.switchViewName = threeWaySwitchName
        pLeft.switchViewName = threeWaySwitchName
        pMid.switchViewName = threeWaySwitchName
        pRight.switchViewName = threeWaySwitchName
        pFirstCenter.switchViewName = firstTurnoutName
        pFirstClosed.switchViewName = firstTurnoutName
        pFirstThrown.switchViewName = firstTurnoutName
        pSecondCenter.switchViewName = secondTurnoutName
        pSecondClosed.switchViewName = secondTurnoutName
        pSecondThrown.switchViewName = secondTurnoutName
        pThirdCenter.switchViewName = thirdTurnoutName
        pThirdClosed.switchViewName = thirdTurnoutName
        pThirdThrown.switchViewName = thirdTurnoutName
    }

    override fun connectTurnouts(turnoutGetter: (String) -> Turnout?) {
        leftTurnout = turnoutGetter(leftTurnoutName)
        rightTurnout = turnoutGetter(rightTurnoutName)
        firstTurnout = turnoutGetter(firstTurnoutName)
        secondTurnout = turnoutGetter(secondTurnoutName)
        thirdTurnout = turnoutGetter(thirdTurnoutName)
    }

    override fun disconnectTurnouts() {
        leftTurnout = null
        rightTurnout = null
        firstTurnout = null
        secondTurnout = null
        thirdTurnout = null
    }

    override fun addPointsToSet(set: MutableSet<Point>) {
        set.add(pCenter)
        set.add(pLeft)
        set.add(pMid)
        set.add(pRight)
        set.add(pFirstCenter)
        set.add(pFirstClosed)
        set.add(pFirstThrown)
        set.add(pSecondCenter)
        set.add(pSecondClosed)
        set.add(pSecondThrown)
        set.add(pThirdCenter)
        set.add(pThirdClosed)
        set.add(pThirdThrown)
    }

    override fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
        val stateLeft = leftTurnout?.state?.value ?: TurnoutState.UNKNOWN
        val stateRight = rightTurnout?.state?.value ?: TurnoutState.UNKNOWN
        when {
            stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pMid.n)
            stateLeft == TurnoutState.THROWN && stateRight == TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pLeft.n)
            stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.THROWN -> graph.addEdge(pCenter.n, pRight.n)
            else -> {}
        }
        when (firstTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pFirstCenter.n, pFirstClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pFirstCenter.n, pFirstThrown.n)
        }
        when (secondTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pSecondCenter.n, pSecondClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pSecondCenter.n, pSecondThrown.n)
        }
        when (thirdTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pThirdCenter.n, pThirdClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pThirdCenter.n, pThirdThrown.n)
        }
    }
}

class ThreeWayCrossoverView(
        private val leftTurnoutName: String,
        private val rightTurnoutName: String,
        private val northTurnoutName: String,
        private val southTurnoutName: String,
        override val elementIndex: Int,
        val pCenter: Point,
        val pLeft: Point,
        val pMid: Point,
        val pRight: Point,
        val pNorthCenter: Point,
        val pNorthClosed: Point,
        val pNorthThrown: Point,
        val pSouthCenter: Point,
        val pSouthClosed: Point,
        val pSouthThrown: Point) : SwitchViewInterface {

    private var leftTurnout: Turnout? = null
    private var rightTurnout: Turnout? = null
    private var northTurnout: Turnout? = null
    private var southTurnout: Turnout? = null

    override fun turnoutGroup() : TurnoutGroup? {
        return if (leftTurnout != null && rightTurnout != null && northTurnout != null && southTurnout != null) {
            ThreeWayCrossover(leftTurnout!!, rightTurnout!!, northTurnout!!, southTurnout!!)
        } else {
            null
        }
    }

    init {
        val threeWayName = "$leftTurnoutName##$rightTurnoutName"
        pCenter.switchViewName = threeWayName
        pLeft.switchViewName = threeWayName
        pMid.switchViewName = threeWayName
        pRight.switchViewName = threeWayName
        pNorthCenter.switchViewName = northTurnoutName
        pNorthClosed.switchViewName = northTurnoutName
        pNorthThrown.switchViewName = northTurnoutName
        pSouthCenter.switchViewName = southTurnoutName
        pSouthClosed.switchViewName = southTurnoutName
        pSouthThrown.switchViewName = southTurnoutName
    }

    override fun connectTurnouts(turnoutGetter: (String) -> Turnout?) {
        leftTurnout = turnoutGetter(leftTurnoutName)
        rightTurnout = turnoutGetter(rightTurnoutName)
        northTurnout = turnoutGetter(northTurnoutName)
        southTurnout = turnoutGetter(southTurnoutName)
    }

    override fun disconnectTurnouts() {
        leftTurnout = null
        rightTurnout = null
        northTurnout = null
        southTurnout = null
    }

    override fun addPointsToSet(set: MutableSet<Point>) {
        set.add(pCenter)
        set.add(pLeft)
        set.add(pMid)
        set.add(pRight)
        set.add(pNorthCenter)
        set.add(pNorthClosed)
        set.add(pNorthThrown)
        set.add(pSouthCenter)
        set.add(pSouthClosed)
        set.add(pSouthThrown)
    }

    override fun addEdgeToGraph(graph: ConnectedTurnoutsGraph) {
        val stateLeft = leftTurnout?.state?.value ?: TurnoutState.UNKNOWN
        val stateRight = rightTurnout?.state?.value ?: TurnoutState.UNKNOWN
        when {
            stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pMid.n)
            stateLeft == TurnoutState.THROWN && stateRight == TurnoutState.CLOSED -> graph.addEdge(pCenter.n, pLeft.n)
            stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.THROWN -> graph.addEdge(pCenter.n, pRight.n)
            else -> {}
        }
        when (northTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pNorthCenter.n, pNorthClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pNorthCenter.n, pNorthThrown.n)
        }
        when (southTurnout?.state?.value ?: TurnoutState.UNKNOWN) {
            TurnoutState.CLOSED -> graph.addEdge(pSouthCenter.n, pSouthClosed.n)
            TurnoutState.THROWN -> graph.addEdge(pSouthCenter.n, pSouthThrown.n)
        }
    }
}
