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

    // TODO: update for 3way switches
    // Have an element with three states!

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
                    "turnWest" -> "blue"
                    "turnEast" -> "red"
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
                    else -> "invalid"
                }
            is TurnoutGroup.ThreeWayTurnout ->
                when (position) {
                    "left" -> "right"
                    "mid" -> "left"
                    "right" -> "mid"
                    else -> "invalid"
                }
            is TurnoutGroup.DoubleSlipTurnout ->
                when (position) {
                    "straight" -> "cross"
                    "cross" -> "turnWest"
                    "turnWest" -> "turnEast"
                    "turnEast" -> "straight"
                    else -> "invalid"
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

    fun connectToElements() {
        turnoutGroups.forEachIndexed { index, turnoutGroup ->
            if (turnoutGroup != null) {
                when(turnoutGroup) {
                    is TurnoutGroup.SingleTurnout ->
                        elements.connect(buttons[index], this,
                                mapOf("onColor" to turnoutGroupColor(turnoutGroup, "thrown"),
                                      "offColor" to turnoutGroupColor(turnoutGroup, "closed"),
                                      "type" to "toggle"),
                                turnoutGroup.turnout.state.value == TurnoutState.THROWN)
                    is TurnoutGroup.ThreeWayTurnout,
                    is TurnoutGroup.DoubleSlipTurnout -> {
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
                    when(turnoutGroup) {
                        is TurnoutGroup.SingleTurnout ->
                            if (turnoutGroupContainsTurnout(turnoutGroup, turnout)) {
                                elements.updateElementStateByJmri(buttons[index], newValue == TurnoutState.THROWN)
                                return
                            }
                        is TurnoutGroup.ThreeWayTurnout,
                        is TurnoutGroup.DoubleSlipTurnout -> {
                            if (turnoutGroupContainsTurnout(turnoutGroup, turnout)) {
                                val position = turnoutGroupPosition(turnoutGroup)
                                val nextPosition = getNextPosition(turnoutGroup, position)
                                buttons[index].setAttributes(
                                        mapOf("offColor" to turnoutGroupColor(turnoutGroup, position),
                                              "onColor" to turnoutGroupColor(turnoutGroup, nextPosition),
                                              "type" to "momentary"))
                                return
                            }
                        }
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
                    if (tog != null) {
                        when(tog) {
                            is TurnoutGroup.SingleTurnout -> {
                                turnoutTable.messageToJmri(
                                    tog.turnout, "state",
                                    if (newValue == true) TurnoutState.THROWN else TurnoutState.CLOSED
                                )
                            }
                            is TurnoutGroup.ThreeWayTurnout -> {
                                if (newValue == true) {
                                    val position = turnoutGroupPosition(tog)
                                    val nextPosition = getNextPosition(tog, position)
                                    when(nextPosition) {
                                        "left" -> {
                                            turnoutTable.messageToJmri(tog.leftTurnout, "state", TurnoutState.THROWN)
                                            turnoutTable.messageToJmri(tog.rightTurnout, "state", TurnoutState.CLOSED)
                                        }
                                        "mid" -> {
                                            turnoutTable.messageToJmri(tog.leftTurnout, "state", TurnoutState.CLOSED)
                                            turnoutTable.messageToJmri(tog.rightTurnout, "state", TurnoutState.CLOSED)
                                        }
                                        "right" -> {
                                            turnoutTable.messageToJmri(tog.leftTurnout, "state", TurnoutState.CLOSED)
                                            turnoutTable.messageToJmri(tog.rightTurnout, "state", TurnoutState.THROWN)
                                        }
                                    }
                                }
                            }
                            is TurnoutGroup.DoubleSlipTurnout -> {
                                if (newValue == true) {
                                    val position = turnoutGroupPosition(tog)
                                    val nextPosition = getNextPosition(tog, position)
                                    when(nextPosition) {
                                        "straight" -> {
                                            turnoutTable.messageToJmri(tog.westTurnout, "state", TurnoutState.CLOSED)
                                            turnoutTable.messageToJmri(tog.eastTurnout, "state", TurnoutState.CLOSED)
                                        }
                                        "cross" -> {
                                            turnoutTable.messageToJmri(tog.westTurnout, "state", TurnoutState.THROWN)
                                            turnoutTable.messageToJmri(tog.eastTurnout, "state", TurnoutState.THROWN)
                                        }
                                        "turnWest" -> {
                                            turnoutTable.messageToJmri(tog.westTurnout, "state", TurnoutState.THROWN)
                                            turnoutTable.messageToJmri(tog.eastTurnout, "state", TurnoutState.CLOSED)
                                        }
                                        "turnEast" -> {
                                            turnoutTable.messageToJmri(tog.westTurnout, "state", TurnoutState.CLOSED)
                                            turnoutTable.messageToJmri(tog.eastTurnout, "state", TurnoutState.THROWN)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return
                }
            }
        }
    }
}
