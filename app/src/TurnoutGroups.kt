package push2throttle

// see doc/SwitchTypes.pdf

interface TurnoutGroup{
    fun containsTurnout(turnout: Turnout) : Boolean
    fun color(position: String) : String
    fun currentPosition() : String
    fun nextPosition(position: String) : String
    fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit)
}

class SingleSwitch(private val turnout: Turnout) : TurnoutGroup {
    override fun containsTurnout(turnout: Turnout) : Boolean {
        return this.turnout == turnout
    }
    override fun color(position: String) = when (position) {
        "closed" -> "green"
        "thrown" -> "red"
        "invalid" -> "orange"
        else -> "black"
    }
    override fun currentPosition() = when(turnout.state.value) {
        TurnoutState.THROWN -> "thrown"
        TurnoutState.CLOSED -> "closed"
        else -> "invalid"
    }
    override fun nextPosition(position: String) = when (position) {
        "closed" -> "thrown"
        "thrown" -> "closed"
        else -> "closed"
    }
    override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
        setState(turnout, if (position == "thrown") TurnoutState.THROWN else TurnoutState.CLOSED)
    }
}

class ThreeWaySwitch(private val leftTurnout: Turnout, private val rightTurnout: Turnout) : TurnoutGroup {
    override fun containsTurnout(turnout: Turnout) : Boolean {
        return leftTurnout == turnout || rightTurnout == turnout
    }
    override fun color(position: String) = when (position) {
        "left" -> "red"
        "mid" -> "green"
        "right" -> "dk-blue"
        "invalid" -> "orange"
        else -> "black"
    }
    override fun currentPosition() : String {
        val stateLeft = leftTurnout.state.value
        val stateRight = rightTurnout.state.value
        return when {
            stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.CLOSED -> "mid"
            stateLeft == TurnoutState.THROWN && stateRight == TurnoutState.CLOSED -> "left"
            stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.THROWN -> "right"
            else -> "invalid"
        }
    }
    override fun nextPosition(position: String) = when (position) {
        "left" -> "right"
        "mid" -> "left"
        "right" -> "mid"
        else -> "mid"
    }
    override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
        setState(leftTurnout, if (position == "left") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(rightTurnout, if (position == "right") TurnoutState.THROWN else TurnoutState.CLOSED)
    }
}

class DoubleSlipSwitch(private val westTurnout: Turnout, private val eastTurnout: Turnout) : TurnoutGroup {
    override fun containsTurnout(turnout: Turnout) : Boolean {
        return eastTurnout == turnout || westTurnout == turnout
    }
    override fun color(position: String) = when (position) {
        "straight" -> "green"
        "cross"    -> "yellow"
        "turnNorth" -> "red"
        "turnSouth" -> "dk-blue"
        "invalid"  -> "orange"
        else -> "black"
    }
    override fun currentPosition() : String {
        val stateWest = westTurnout.state.value
        val stateEast = eastTurnout.state.value
        return when {
            stateWest == TurnoutState.CLOSED && stateEast == TurnoutState.CLOSED -> "straight"
            stateWest == TurnoutState.THROWN && stateEast == TurnoutState.THROWN -> "cross"
            stateWest == TurnoutState.THROWN && stateEast == TurnoutState.CLOSED -> "turnNorth"
            stateWest == TurnoutState.CLOSED && stateEast == TurnoutState.THROWN -> "turnSouth"
            else -> "invalid"
        }
    }
    override fun nextPosition(position: String) = when (position) {
        "straight" -> "cross"
        "cross" -> "turnNorth"
        "turnNorth" -> "turnSouth"
        "turnSouth" -> "straight"
        else -> "straight"
    }
    override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
        setState(westTurnout, when (position) {
            "cross", "turnNorth" -> TurnoutState.THROWN
            else -> TurnoutState.CLOSED
        })
        setState(eastTurnout, when (position) {
            "cross", "turnSouth" -> TurnoutState.THROWN
            else -> TurnoutState.CLOSED
        })
    }
}

class CrossoverSwitch(private val westTurnout: Turnout, private val eastTurnout: Turnout) : TurnoutGroup {
    override fun containsTurnout(turnout: Turnout) : Boolean {
        return eastTurnout == turnout || westTurnout == turnout
    }
    override fun color(position: String) = when (position) {
        "straight" -> "green"
        "cross"    -> "red"
        "invalid"  -> "orange"
        else -> "black"
    }
    override fun currentPosition() : String {
        val stateWest = westTurnout.state.value
        val stateEast = eastTurnout.state.value
        return when {
            stateWest == TurnoutState.CLOSED && stateEast == TurnoutState.CLOSED -> "straight"
            stateWest == TurnoutState.THROWN && stateEast == TurnoutState.THROWN -> "cross"
            else -> "invalid"
        }
    }
    override fun nextPosition(position: String) = when (position) {
        "straight" -> "cross"
        "cross" -> "straight"
        else -> "straight"
    }
    override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
        setState(westTurnout, if (position == "cross") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(eastTurnout, if (position == "cross") TurnoutState.THROWN else TurnoutState.CLOSED)
    }
}
