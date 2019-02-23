package push2throttle

// Manages the interaction between the Push2 display button elements and
// the turnouts of the currently visible panel, if it is visible.

class Push2TurnoutController(
        private val elements: Push2Elements,
        private val turnoutTable: JmriTurnoutTable,
        private val buttons: Array<ButtonRgb>) : MidiController {

    sealed class TurnoutGroup {
        data class SingleTurnout(val turnout: Turnout) : TurnoutGroup()
        data class ThreeWayTurnout(val leftTurnout: Turnout, val rightTurnout: Turnout) : TurnoutGroup()
        data class DoubleSlipTurnout(val westTurnout: Turnout, val eastTurnout: Turnout) : TurnoutGroup()
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

    private fun turnoutGroupContainsTurnout(turnoutGroup: TurnoutGroup, turnout: Turnout) : Boolean {
        return when(turnoutGroup) {
            is TurnoutGroup.SingleTurnout ->
                turnoutGroup.turnout == turnout
            is TurnoutGroup.ThreeWayTurnout ->
                turnoutGroup.leftTurnout == turnout || turnoutGroup.rightTurnout == turnout
            is TurnoutGroup.DoubleSlipTurnout ->
                turnoutGroup.eastTurnout == turnout || turnoutGroup.westTurnout == turnout
        }
    }


    private fun turnoutGroupColor(turnoutGroup: TurnoutGroup, position: String) : String {
        return when(turnoutGroup) {
            is TurnoutGroup.SingleTurnout ->
                when (position) {
                    "closed" -> "green"
                    "thrown" -> "red"
                    "invalid" -> "orange" // TODO: solve color table todo in Push2Elements
                    else -> "black"
                }
            is TurnoutGroup.ThreeWayTurnout ->
                when (position) {
                    "left" -> "red"
                    "mid" -> "green"
                    "right" -> "blue"
                    "invalid" -> "orange"
                    else -> "black"
                }
            is TurnoutGroup.DoubleSlipTurnout ->
                when (position) {
                    "straight" -> "green"
                    "cross"    -> "yellow"
                    "turnWest" -> "red"
                    "turnEast" -> "blue"
                    "invalid"  -> "orange"
                    else -> "black"
                }
        }
    }

    private fun getNextPosition(turnoutGroup: TurnoutGroup, position: String): String {
        return when(turnoutGroup) {
            is TurnoutGroup.SingleTurnout ->
                when (position) {
                    "closed" -> "thrown"
                    "thrown" -> "closed"
                    else -> "closed"
                }
            is TurnoutGroup.ThreeWayTurnout ->
                when (position) {
                    "left" -> "right"
                    "mid" -> "left"
                    "right" -> "mid"
                    else -> "mid"
                }
            is TurnoutGroup.DoubleSlipTurnout ->
                when (position) {
                    "straight" -> "cross"
                    "cross" -> "turnWest"
                    "turnWest" -> "turnEast"
                    "turnEast" -> "straight"
                    else -> "straight"
                }
        }
    }

    private fun turnoutGroupPosition(turnoutGroup: TurnoutGroup) : String {
        return when(turnoutGroup) {
            is TurnoutGroup.SingleTurnout ->
                when(turnoutGroup.turnout.state.value) {
                    TurnoutState.THROWN -> "thrown"
                    TurnoutState.CLOSED -> "closed"
                    else -> "invalid"
                }
            is TurnoutGroup.ThreeWayTurnout -> {
                val stateLeft = turnoutGroup.leftTurnout.state.value
                val stateRight = turnoutGroup.rightTurnout.state.value
                when {
                    stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.CLOSED -> "mid"
                    stateLeft == TurnoutState.THROWN && stateRight == TurnoutState.CLOSED -> "left"
                    stateLeft == TurnoutState.CLOSED && stateRight == TurnoutState.THROWN -> "right"
                    else -> "invalid"
                }
            }
            is TurnoutGroup.DoubleSlipTurnout -> {
                val stateWest = turnoutGroup.westTurnout.state.value
                val stateEast = turnoutGroup.eastTurnout.state.value
                when {
                    stateWest == TurnoutState.CLOSED && stateEast == TurnoutState.CLOSED -> "straight"
                    stateWest == TurnoutState.THROWN && stateEast == TurnoutState.THROWN -> "cross"
                    stateWest == TurnoutState.THROWN && stateEast == TurnoutState.CLOSED -> "turnWest"
                    stateWest == TurnoutState.CLOSED && stateEast == TurnoutState.THROWN -> "turnEast"
                    else -> "invalid"
                }
            }
        }
    }

    private fun setTurnoutStates(turnoutGroup: TurnoutGroup, position: String, setState: (Turnout, Int) -> Unit) {
        when (turnoutGroup) {
            is TurnoutGroup.SingleTurnout ->
                setState(turnoutGroup.turnout,
                   when(position) {"thrown" -> TurnoutState.THROWN else -> TurnoutState.CLOSED })
            is TurnoutGroup.ThreeWayTurnout -> {
                setState(turnoutGroup.leftTurnout,
                   when (position) {"left" -> TurnoutState.THROWN else -> TurnoutState.CLOSED })
                setState(turnoutGroup.rightTurnout,
                   when (position) {"right" -> TurnoutState.THROWN else -> TurnoutState.CLOSED })
            }
            is TurnoutGroup.DoubleSlipTurnout -> {
                setState(turnoutGroup.westTurnout,
                   when(position) {"cross", "turnWest" -> TurnoutState.THROWN else -> TurnoutState.CLOSED })
                setState(turnoutGroup.eastTurnout,
                   when(position) {"cross", "turnEast" -> TurnoutState.THROWN else -> TurnoutState.CLOSED })
            }
        }
    }

    fun connectToElements() {
        turnoutGroups.forEachIndexed { index, turnoutGroup ->
            if (turnoutGroup != null) {
                val position = turnoutGroupPosition(turnoutGroup)
                val nextPosition = getNextPosition(turnoutGroup, position)
                elements.connect(buttons[index], this,
                        mapOf("onColor" to  turnoutGroupColor(turnoutGroup, nextPosition),
                              "offColor" to turnoutGroupColor(turnoutGroup, position),
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
            turnoutGroups.forEachIndexed { index, turnoutGroup ->
                if (turnoutGroup != null) {
                    if (turnoutGroupContainsTurnout(turnoutGroup, turnout)) {
                        val position = turnoutGroupPosition(turnoutGroup)
                        val nextPosition = getNextPosition(turnoutGroup, position) // is this correct? what about newValue??
                        buttons[index].setAttributes(
                                mapOf("offColor" to turnoutGroupColor(turnoutGroup, position),
                                      "onColor" to turnoutGroupColor(turnoutGroup, nextPosition)))
                        return
                    }
                }
            }
        }
    }

    override fun <T: Any> elementStateChanged(element: MidiElement, newValue: T) {
        if (element is ButtonRgb) {
            for (index in buttons.indices) {
                if (element == buttons[index]) {
                    val tog = turnoutGroups[index]
                    if (tog != null && newValue == true) {
                        val position = turnoutGroupPosition(tog)
                        val nextPosition = getNextPosition(tog, position)
                        setTurnoutStates(tog, nextPosition) { turnout, state ->
                            turnoutTable.messageToJmri(turnout, "state", state)
                        }
                    }
                    return
                }
            }
        }
    }
}
