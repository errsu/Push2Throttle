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

class CrossoverBranchForward(
        private val turnout: Turnout,
        private val inTurnout: Turnout,
        private val branchTurnout: Turnout) : TurnoutGroup {

    override fun containsTurnout(turnout: Turnout) : Boolean {
        return this.turnout == turnout || inTurnout == turnout || branchTurnout == turnout
    }
    override fun color(position: String) = when (position) {
        "straight" -> "green"
        "branch"   -> "yellow"
        "cross"    -> "dk-blue"
        "change"   -> "red"
        "invalid"  -> "orange"
        else -> "black"
    }
    override fun currentPosition() : String {
        val state = turnout.state.value
        val stateIn = inTurnout.state.value
        val stateBranch = branchTurnout.state.value
        return when {
            state == TurnoutState.CLOSED &&
            stateIn == TurnoutState.CLOSED &&
            stateBranch == TurnoutState.CLOSED -> "straight"

            state == TurnoutState.CLOSED &&
            stateIn == TurnoutState.CLOSED &&
            stateBranch == TurnoutState.THROWN -> "branch"

            state == TurnoutState.THROWN &&
            stateIn == TurnoutState.THROWN &&
            stateBranch == TurnoutState.THROWN -> "cross"

            state == TurnoutState.THROWN &&
            stateIn == TurnoutState.THROWN &&
            stateBranch == TurnoutState.CLOSED -> "change"
            else -> "invalid"
        }
    }
    override fun nextPosition(position: String) = when (position) {
        "straight" -> "branch"
        "branch"   -> "cross"
        "cross"    -> "change"
        "change"   -> "straight"
        else -> "straight"
    }
    override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
        setState(turnout, if (position == "cross" || position == "change") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(inTurnout, if (position == "cross" || position == "change") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(branchTurnout, if (position == "cross" || position == "branch") TurnoutState.THROWN else TurnoutState.CLOSED)
    }
}

class CrossoverBranchBackward(
        private val leftTurnout: Turnout,
        private val rightTurnout: Turnout,
        private val inTurnout: Turnout) : TurnoutGroup {

    override fun containsTurnout(turnout: Turnout) : Boolean {
        return leftTurnout == turnout || rightTurnout == turnout || inTurnout == turnout
    }
    override fun color(position: String) = when (position) {
        "straight" -> "green"
        "branch"   -> "yellow"
        "change"   -> "red"
        "invalid"  -> "orange"
        else -> "black"
    }
    override fun currentPosition() : String {
        val stateLeft = leftTurnout.state.value
        val stateRight = rightTurnout.state.value
        val stateIn = inTurnout.state.value
        return when {
            stateLeft == TurnoutState.CLOSED &&
            stateRight == TurnoutState.CLOSED &&
            stateIn == TurnoutState.CLOSED -> "straight"

            stateLeft == TurnoutState.CLOSED &&
            stateRight == TurnoutState.THROWN &&
            stateIn == TurnoutState.CLOSED -> "branch"

            stateLeft == TurnoutState.THROWN &&
            stateRight == TurnoutState.CLOSED &&
            stateIn == TurnoutState.THROWN -> "change"
            else -> "invalid"
        }
    }
    override fun nextPosition(position: String) = when (position) {
        "straight" -> "branch"
        "branch"   -> "change"
        "change"   -> "straight"
        else -> "straight"
    }
    override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
        setState(leftTurnout, if (position == "change") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(rightTurnout, if (position == "branch") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(inTurnout, if (position == "change") TurnoutState.THROWN else TurnoutState.CLOSED)
    }
}

class MiddlePassing(
        private val midTurnout: Turnout,
        private val northTurnout: Turnout,
        private val southTurnout: Turnout) : TurnoutGroup {

    override fun containsTurnout(turnout: Turnout) : Boolean {
        return midTurnout == turnout || northTurnout == turnout || southTurnout == turnout
    }
    override fun color(position: String) = when (position) {
        "straight" -> "green"
        "right"    -> "dk-blue"
        "left"     -> "red"
        "invalid"  -> "orange"
        else -> "black"
    }
    override fun currentPosition() : String {
        val stateMid = midTurnout.state.value
        val stateNorth = northTurnout.state.value
        val stateSouth = southTurnout.state.value
        return when {
            stateNorth == TurnoutState.CLOSED &&
            stateSouth == TurnoutState.CLOSED -> "straight"

            stateMid == TurnoutState.CLOSED &&
            stateNorth == TurnoutState.THROWN &&
            stateSouth == TurnoutState.CLOSED -> "right"

            stateMid == TurnoutState.THROWN &&
            stateNorth == TurnoutState.CLOSED &&
            stateSouth == TurnoutState.THROWN -> "left"
            else -> "invalid"
        }
    }
    override fun nextPosition(position: String) = when (position) {
        "straight" -> "right"
        "right"    -> "left"
        "left"     -> "straight"
        else -> "straight"
    }
    override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
        setState(midTurnout, if (position == "left") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(northTurnout, if (position == "right") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(southTurnout, if (position == "left") TurnoutState.THROWN else TurnoutState.CLOSED)
    }
}

class DoubleCrossoverBranch(
        private val leftTurnout: Turnout,
        private val rightTurnout: Turnout,
        private val firstTurnout: Turnout,
        private val secondTurnout: Turnout,
        private val thirdTurnout: Turnout) : TurnoutGroup {

    override fun containsTurnout(turnout: Turnout) : Boolean {
        return leftTurnout == turnout || rightTurnout == turnout ||
                firstTurnout == turnout || secondTurnout == turnout || thirdTurnout == turnout
    }
    override fun color(position: String) = when (position) {
        "straight" -> "green"
        "branch"   -> "yellow"
        "change"   -> "red"
        "cross"    -> "dk-blue"
        "invalid"  -> "orange"
        else -> "black"
    }
    override fun currentPosition() : String {
        val stateLeft = leftTurnout.state.value
        val stateRight = rightTurnout.state.value
        val stateFirst = firstTurnout.state.value
        val stateSecond = secondTurnout.state.value
        val stateThird = thirdTurnout.state.value
        return when {
            stateLeft == TurnoutState.CLOSED &&
            stateRight == TurnoutState.CLOSED &&
            stateFirst == TurnoutState.CLOSED &&
            stateSecond == TurnoutState.CLOSED &&
            stateThird == TurnoutState.CLOSED -> "straight"

            stateLeft == TurnoutState.CLOSED &&
            stateRight == TurnoutState.THROWN &&
            stateSecond == TurnoutState.CLOSED &&
            stateThird == TurnoutState.CLOSED -> "branch"

            stateLeft == TurnoutState.CLOSED &&
            stateRight == TurnoutState.THROWN &&
            stateFirst == TurnoutState.THROWN &&
            stateThird == TurnoutState.THROWN -> "change"

            stateLeft == TurnoutState.THROWN &&
            stateRight == TurnoutState.CLOSED &&
            stateFirst == TurnoutState.THROWN &&
            stateSecond == TurnoutState.THROWN &&
            stateThird == TurnoutState.THROWN -> "cross"

            else -> "invalid"
        }
    }
    override fun nextPosition(position: String) = when (position) {
        "straight" -> "branch"
        "branch"   -> "change"
        "change"   -> "cross"
        "cross"    -> "straight"
        else -> "straight"
    }
    override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
        setState(leftTurnout, if (position == "cross") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(rightTurnout, if (position == "branch" || position == "change") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(firstTurnout, if (position == "change" || position == "cross") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(secondTurnout, if (position == "cross") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(thirdTurnout, if (position == "change" || position == "cross") TurnoutState.THROWN else TurnoutState.CLOSED)
    }
}

class ThreeWayCrossover(
        private val leftTurnout: Turnout,
        private val rightTurnout: Turnout,
        private val northTurnout: Turnout,
        private val southTurnout: Turnout) : TurnoutGroup {

    override fun containsTurnout(turnout: Turnout) : Boolean {
        return leftTurnout == turnout || this.rightTurnout == turnout ||
                northTurnout == turnout || southTurnout == turnout
    }
    override fun color(position: String) = when (position) {
        "straight" -> "green"
        "right"    -> "dk-blue"
        "left"     -> "red"
        "invalid"  -> "orange"
        else -> "black"
    }
    override fun currentPosition() : String {
        val stateLeft = leftTurnout.state.value
        val stateRight = rightTurnout.state.value
        val stateNorth = northTurnout.state.value
        val stateSouth = southTurnout.state.value
        return when {
            stateLeft == TurnoutState.CLOSED &&
            stateRight == TurnoutState.CLOSED &&
            stateNorth == TurnoutState.CLOSED &&
            stateSouth == TurnoutState.CLOSED -> "straight"

            stateLeft == TurnoutState.CLOSED &&
            stateRight == TurnoutState.THROWN &&
            stateNorth == TurnoutState.THROWN &&
            stateSouth == TurnoutState.CLOSED -> "right"

            stateLeft == TurnoutState.THROWN &&
            stateRight == TurnoutState.CLOSED &&
            stateNorth == TurnoutState.CLOSED &&
            stateSouth == TurnoutState.THROWN -> "left"

            else -> "invalid"
        }
    }
    override fun nextPosition(position: String) = when (position) {
        "straight" -> "right"
        "right"    -> "left"
        "left"     -> "straight"
        else -> "straight"
    }
    override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
        setState(leftTurnout, if (position == "left") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(rightTurnout, if (position == "right") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(northTurnout, if (position == "right") TurnoutState.THROWN else TurnoutState.CLOSED)
        setState(southTurnout, if (position == "left") TurnoutState.THROWN else TurnoutState.CLOSED)
    }
}
