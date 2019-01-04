// Manages the interaction between the Push2 display button elements and
// the turnouts of the currently visible panel, if it is visible.

class Push2TurnoutController(
        private val elements: Push2Elements,
        private val turnoutTable: JmriTurnoutTable,
        private val buttons: Array<ButtonRgb>) : MidiController {

    var turnouts = arrayOfNulls<Turnout?>(16)

    fun connectToElements() {
        turnouts.forEachIndexed { index, turnout ->
            if (turnout != null) {
                elements.connect(buttons[index], this,
                        mapOf("onColor" to "red", "offColor" to "green", "type" to "toggle"),
                        turnout.state.value)
            }
        }
    }

    fun disconnectFromElements() {
        turnouts.forEachIndexed { index, _ ->
            elements.disconnect(buttons[index])
        }
    }

    fun turnoutAttrChanged(turnout: Turnout, attrName: String, newValue: Any?) {
        for (index in turnouts.indices) {
            if (turnout == turnouts[index]) {
                when(attrName) {
                    "state" -> elements.updateElementStateByJmri(
                            buttons[index],
                            newValue == TurnoutState.THROWN)
                }
                break
            }
        }
    }

    override fun <T: Any> elementStateChanged(element: MidiElement, newValue: T) {
        for (index in buttons.indices) {
            if (element == buttons[index]) {
                if (turnouts[index] != null) {
                    turnoutTable.messageToJmri(
                            turnouts[index]!!, "state",
                            if (newValue == true) TurnoutState.THROWN else TurnoutState.CLOSED)
                }
            }
        }
    }
}
