import com.sun.org.apache.xpath.internal.operations.Bool

// The Push2 throttle controller manages the interaction between a subset of
// the Push2 elements and a certain JMRI throttle (thus the name).
// The JMRI throttle in turn is connected to the loco.

// The advantage of using the constructor is that the links need not be nullable.

// TODO: make sure no element controls more than one state, e.g. ERP - split into turn and touch

class Push2TurnoutController(
        private val elements: Push2Elements,
        private val turnouts: JmriTurnouts,
        private val button: ButtonRgb) : MidiController {

    var turnout:  JmriTurnout? = null

    fun connectToElements() {
        if (turnout != null) {
            elements.connect(button, this,
                    mapOf("onColor" to "red", "offColor" to "green", "type" to "toggle"),
                    turnout?.state?.value)
        }
    }

    fun disconnectFromElements() {
        elements.disconnect(button)
    }

    fun turnoutAttrChanged(attrName: String, newValue: Any?) {
        when(attrName) {
            "state" -> elements.updateElementStateByJmri(button, newValue == TurnoutState.THROWN)
        }
    }

    override fun <T: Any> elementStateChanged(element: MidiElement, newValue: T) {
        when (element) {
            button -> turnout?.messageToJmri(turnouts, "state",
                    if (newValue == true) TurnoutState.THROWN else TurnoutState.CLOSED)
        }
    }
}
