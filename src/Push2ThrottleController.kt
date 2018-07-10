// The Push2 throttle controller manages the interaction between a subset of
// the Push2 elements and a certain JMRI throttle (thus the name).
// The JMRI throttle in turn is connected to the loco.

// The advantage of using the constructor is that the links need not be nullable.

// TODO: make sure no element controls more than one state, e.g. ERP - split into turn and touch

class Push2ThrottleController(
        private val elements: Push2Elements,
        private val throttle: JmriThrottle,
        private val erp:      Erp,
        private val dispA:    ButtonRgb,
        private val dispB:    ButtonRgb
//        private val pad1: Pad,
//        private val pad2: Pad,
//        private val pad3: Pad,
//        private val pad4: Pad,
//        private val pad5: Pad,
//        private val pad6: Pad,
//        private val pad7: Pad,
//        private val pad8: Pad
) : MidiController {

    fun connectToElements() {
        val loco = throttle.loco
        if (loco != null) {
            elements.connect(erp, this,
                    mapOf(),
                    loco.speed.value.coerceIn(0.0f, 1.0f))
            elements.connect(dispA, this,
                    mapOf("onColor" to "green", "offColor" to "red", "type" to "toggle"),
                    loco.forward.value)
            elements.connect(dispB, this,
                    mapOf("onColor" to "white", "offColor" to loco.color.value, "type" to "toggle"),
                    false)
        }
    }

    fun disconnectFromElements() {
        elements.disconnect(erp)
        elements.disconnect(dispA)
        elements.disconnect(dispB)
    }

    fun locoAttrChanged(attrName: String, newValue: Any?) {
        when(attrName) {
            "forward" -> elements.updateElementStateByJmri(dispA, newValue)
            // don't forward negative values (as received on stop!)
            "speed"   -> elements.updateElementStateByJmri(erp, (newValue as Float).coerceIn(0.0f, 1.0f))
        }
    }

    override fun <T: Any> elementStateChanged(element: MidiElement, newValue: T) {
        when (element) {
            erp   -> throttle.messageToJmri("speed", newValue)
            dispA -> throttle.messageToJmri("forward", newValue)
        }
    }
}
