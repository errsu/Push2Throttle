const val TOGGLE = true
const val MOMENTARY = false

class Pad(val name: String, val number: Int, val toggle: Boolean) {
    var state = false
    fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("nn", number) {
            value ->
                var newState = state
                if (toggle) {
                    if (value > 0) newState = !state
                } else {
                    newState = (value > 0)
                }
                if (newState != state) {
                    state = newState
                    update(midi)
                }
        }
    }
    fun update(midi: Push2Midi) {
        midi.sendNN(0, number, if (state) 122 else 0)
        println("pad $name state: $state")
    }
}

class Erp(val name: String, val turn_cc_number: Int, val touch_nn_number: Int) {
    var state = 0.0f
    var min = 0.0f
    var max = 1.0f
    var delta = 1.0f / 128.0f // clockwise == more
    var touched = false
    fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("cc", turn_cc_number) {
            value ->
                if (value < 64) { // turn right
                    state = minOf(state + delta * value, max)
                } else { // turn left
                    state = maxOf(state + delta * (value - 128), min)
                }
                update(midi)
        }
        midi.registerElement("nn", touch_nn_number) {
            value ->
                touched = (value > 0)
                update(midi)
        }
    }
    fun update(midi: Push2Midi) {
        println("pot $name state: $state touched: $touched")
    }
}

class ButtonWhite(val name: String, val number: Int, val toggle: Boolean) {
    var state = false
    fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("cc", number) {
            value ->
                var newState = state
                if (toggle) {
                    if (value > 0) newState = !state
                } else {
                    newState = (value > 0)
                }
                if (newState != state) {
                    state = newState
                    update(midi)
                }
        }
    }
    fun update(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) 127 else 0)
        println("white button $name state: $state")
    }
}

class ButtonRgb(val name: String, val number: Int, val toggle: Boolean) {
    var state = false
    fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("cc", number) {
            value ->
                var newState = state
                if (toggle) {
                    if (value > 0) newState = !state
                } else {
                    newState = (value > 0)
                }
                if (newState != state) {
                    state = newState
                    update(midi)
            }
        }
    }
    fun update(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) 122 else 0)
        println("rgb button $name state: $state")
    }
}

class Push2Elements {
    private val pad1 = Pad("pad_t1_s7", 44, TOGGLE)
    private val pad2 = Pad("pad_t1_s8", 36, MOMENTARY)
    private val pot = Erp("pot_t1", 71, 0)
    private val button1 = ButtonRgb("above_disp_t1", 102, MOMENTARY)
    private val button2 = ButtonRgb("below_disp_t1", 20, TOGGLE)
    private val button3 = ButtonWhite("repeat", 56, TOGGLE)
    private val button4 = ButtonWhite("select", 48, MOMENTARY)
    fun register(midi: Push2Midi) {
        pad1.register(midi)
        pad2.register(midi)
        pot.register(midi)
        button1.register(midi)
        button2.register(midi)
        button3.register(midi)
        button4.register(midi)
    }
}
