package push2throttle

// Manages the interaction between the Push2 display button elements and
// the turnouts of the currently visible panel, if it is visible.

class Push2TurnoutController(
        private val elements: Push2Elements,
        private val turnoutTable: JmriTurnoutTable,
        private val buttons: Array<ButtonRgb>) : MidiController {

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
            "invalid" -> "orange" // TODO: solve color table todo in Push2Elements
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
            "right" -> "blue"
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
            "turnWest" -> "red"
            "turnEast" -> "blue"
            "invalid"  -> "orange"
            else -> "black"
        }
        override fun currentPosition() : String {
            val stateWest = westTurnout.state.value
            val stateEast = eastTurnout.state.value
            return when {
                stateWest == TurnoutState.CLOSED && stateEast == TurnoutState.CLOSED -> "straight"
                stateWest == TurnoutState.THROWN && stateEast == TurnoutState.THROWN -> "cross"
                stateWest == TurnoutState.THROWN && stateEast == TurnoutState.CLOSED -> "turnWest"
                stateWest == TurnoutState.CLOSED && stateEast == TurnoutState.THROWN -> "turnEast"
                else -> "invalid"
            }
        }
        override fun nextPosition(position: String) = when (position) {
            "straight" -> "cross"
            "cross" -> "turnWest"
            "turnWest" -> "turnEast"
            "turnEast" -> "straight"
            else -> "straight"
        }
        override fun setTurnoutStates(position: String, setState: (Turnout, Int) -> Unit) {
            setState(westTurnout, when (position) {
                "cross", "turnWest" -> TurnoutState.THROWN
                else -> TurnoutState.CLOSED
            })
            setState(eastTurnout, when (position) {
                "cross", "turnEast" -> TurnoutState.THROWN
                else -> TurnoutState.CLOSED
            })
        }
    }

    private var turnoutGroups = arrayOfNulls<TurnoutGroup?>(16)

    fun connectPositionToTurnouts(index: Int, turnoutGroup: TurnoutGroup) {
        turnoutGroups[index] = turnoutGroup
    }

    fun disconnectFromTurnouts() {
        for (index in turnoutGroups.indices) {
            turnoutGroups[index] = null
        }
    }

    fun connectToElements() {
        turnoutGroups.forEachIndexed { index, turnoutGroup ->
            if (turnoutGroup != null) {
                val position = turnoutGroup.currentPosition()
                val nextPosition = turnoutGroup.nextPosition(position)
                elements.connect(buttons[index], this,
                        mapOf("onColor" to  turnoutGroup.color(nextPosition),
                              "offColor" to turnoutGroup.color(position),
                              "type" to "momentary"),
                              false
                        )
            }
        }
    }

    fun disconnectFromElements() {
        buttons.forEachIndexed { index, _ ->
            elements.disconnect(buttons[index])
        }
    }

    fun turnoutAttrChanged(turnout: Turnout, attrName: String, newValue: Any?) {
        if (attrName == "state") {
            val index = turnoutGroups.indexOfFirst { it != null && it.containsTurnout(turnout) }
            turnoutGroups.getOrNull(index)?.apply {
                val position = currentPosition()
                val nextPosition = nextPosition(position) // is this correct? what about newValue??
                buttons[index].setAttributes(
                        mapOf("offColor" to color(position),
                              "onColor" to color(nextPosition)))
            }
        }
    }

    override fun <T: Any> elementStateChanged(element: MidiElement, newValue: T) {
        if (element is ButtonRgb && newValue == true) {
            turnoutGroups.getOrNull(buttons.indexOf(element))?.apply{
                val position = currentPosition()
                val nextPosition = nextPosition(position)
                setTurnoutStates(nextPosition) { turnout, state ->
                    turnoutTable.messageToJmri(turnout, "state", state)
                }
            }
        }
    }
}
