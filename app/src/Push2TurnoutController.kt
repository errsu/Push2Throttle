package push2throttle

// Manages the interaction between the Push2 display button elements and
// the turnouts of the currently visible panel, if it is visible.

class Push2TurnoutController(
        private val elements: Push2Elements,
        private val turnoutTable: JmriTurnoutTable,
        private val buttons: Array<ButtonRgb>) : MidiController {

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
