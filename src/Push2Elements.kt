interface MidiElement {
    fun register(midi: Push2Midi)
    fun update(midi: Push2Midi)
}

class Erp(val name: String, val turn_cc_number: Int, val touch_nn_number: Int) : MidiElement {
    var state = 0.0f
    var min = 0.0f
    var max = 1.0f
    var delta = 1.0f / 128.0f // clockwise == more
    var touched = false
    override fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("cc", turn_cc_number) { value ->
            if (value < 64) { // turn right
                state = minOf(state + delta * value, max)
            } else { // turn left
                state = maxOf(state + delta * (value - 128), min)
            }
            update(midi)
        }
        midi.registerElement("nn", touch_nn_number) { value ->
            touched = (value > 0)
            update(midi)
        }
    }
    override fun update(midi: Push2Midi) {
        println("pot $name state: $state touched: $touched")
    }
}

const val TOGGLE = true
const val MOMENTARY = false

abstract class Switch(val toggle: Boolean) : MidiElement {
    var state = false
    fun onMidi(midi: Push2Midi, value: Int) {
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

class Pad(val name: String, val number: Int, toggle: Boolean) : Switch(toggle) {
    override fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("nn", number) { value -> onMidi(midi, value) }
    }
    override fun update(midi: Push2Midi) {
        midi.sendNN(0, number, if (state) 122 else 0)
        println("pad $name state: $state")
    }
}

class ButtonWhite(val name: String, val number: Int, toggle: Boolean) : Switch(toggle) {
    override fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun update(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) 127 else 0)
        println("white button $name state: $state")
    }
}

class ButtonRgb(val name: String, val number: Int, toggle: Boolean) : Switch(toggle) {
    override fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun update(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) 122 else 0)
        println("rgb button $name state: $state")
    }
}

class Push2Elements {
    private val elements: Array<MidiElement> = arrayOf(
        Pad("pad_t1_s7", 44, TOGGLE),
        Pad("pad_t1_s8", 36, MOMENTARY),
        Erp("pot_t1", 71, 0),
        ButtonRgb("above_disp_t1", 102, MOMENTARY),
        ButtonRgb("below_disp_t1", 20, TOGGLE),
        ButtonWhite("repeat", 56, TOGGLE),
        ButtonWhite("select", 48, MOMENTARY)
    )
    fun register(midi: Push2Midi) {
        for (element in elements) {
            element.register(midi)
        }
    }
}
