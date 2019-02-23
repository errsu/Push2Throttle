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

    private fun turnoutGroupColor(turnoutGroup: TurnoutGroup, position: String) : String {
        return when(turnoutGroup) {
            is TurnoutGroup.SingleTurnout ->
                when (position) {
                    "closed" -> "green"
                    "thrown" -> "red"
                    "invalid" -> "orange"
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
        }
    }

    private fun nextPosition(position: String): String = when(position) {
        "left" -> "right"
        "mid" -> "left"
        "right" -> "mid"
        else -> "invalid"
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
                    is TurnoutGroup.ThreeWayTurnout -> {
                        val position = turnoutGroupPosition(turnoutGroup)
                        elements.connect(buttons[index], this,
                                mapOf("onColor" to  turnoutGroupColor(turnoutGroup, nextPosition(position)),
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
                            if (turnout == turnoutGroup.turnout) {
                                elements.updateElementStateByJmri(buttons[index], newValue == TurnoutState.THROWN)
                                return
                            }
                        is TurnoutGroup.ThreeWayTurnout -> {
                            if (turnout == turnoutGroup.leftTurnout || turnout == turnoutGroup.rightTurnout) {
                                val position = turnoutGroupPosition(turnoutGroup)
                                buttons[index].setAttributes(
                                        mapOf("offColor" to turnoutGroupColor(turnoutGroup, position),
                                              "onColor" to turnoutGroupColor(turnoutGroup, nextPosition(position)),
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
                    val tg = turnoutGroups[index]
                    if (tg != null) {
                        when(tg) {
                            is TurnoutGroup.SingleTurnout -> {
                                turnoutTable.messageToJmri(
                                    tg.turnout, "state",
                                    if (newValue == true) TurnoutState.THROWN else TurnoutState.CLOSED
                                )
                            }
                            is TurnoutGroup.ThreeWayTurnout -> {
                                if (newValue == true) {
                                    val position = turnoutGroupPosition(tg)
                                    when(nextPosition(position)) {
                                        "left" -> {
                                            turnoutTable.messageToJmri(tg.leftTurnout, "state", TurnoutState.THROWN)
                                            turnoutTable.messageToJmri(tg.rightTurnout, "state", TurnoutState.CLOSED)
                                        }
                                        "mid" -> {
                                            turnoutTable.messageToJmri(tg.leftTurnout, "state", TurnoutState.CLOSED)
                                            turnoutTable.messageToJmri(tg.rightTurnout, "state", TurnoutState.CLOSED)
                                        }
                                        "right" -> {
                                            turnoutTable.messageToJmri(tg.leftTurnout, "state", TurnoutState.CLOSED)
                                            turnoutTable.messageToJmri(tg.rightTurnout, "state", TurnoutState.THROWN)
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
