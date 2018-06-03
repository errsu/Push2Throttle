abstract class MidiElement() {
    var name: String = "noname"
    var mapper: Push2Mapper? = null
    abstract fun registerForMidi(midi: Push2Midi)
    abstract fun updatePush2(midi: Push2Midi)
    abstract fun updateState(value: Any, midi: Push2Midi)
    fun updateJmri(newValue: Any) {
        mapper?.push2ElementStateChanged(name, newValue)
    }
}

class Erp(val turn_cc_number: Int, val touch_nn_number: Int) : MidiElement() {
    var state = 0.0f
    var min = 0.0f
    var max = 1.0f
    var delta = 1.0f / 128.0f // clockwise == more
    var touched = false

    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("cc", turn_cc_number) { value ->
            if (value < 64) { // turn right
                state = minOf(state + delta * value, max)
            } else { // turn left
                state = maxOf(state + delta * (value - 128), min)
            }
            updateJmri(state)
            updatePush2(midi)
        }
        midi.registerElement("nn", touch_nn_number) { value ->
            touched = (value > 0)
            updatePush2(midi)
        }
    }
    override fun updatePush2(midi: Push2Midi) {
        println("pot $name state: $state touched: $touched")
    }
    override fun updateState(value: Any, midi: Push2Midi) {
        if (value is Float) { // TODO: how to update touch?
            state = value
            updatePush2(midi)
        }
    }
}

const val TOGGLE = true
const val MOMENTARY = false

abstract class Switch(val toggle: Boolean) : MidiElement() {
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
            updateJmri(state)
            updatePush2(midi)
        }
    }
    override fun updateState(value: Any, midi: Push2Midi) {
        if (value is Boolean) {
            state = value
            updatePush2(midi)
        }
    }
}

class Pad(val number: Int, toggle: Boolean) : Switch(toggle) {
    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("nn", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendNN(0, number, if (state) 122 else 0)
        println("pad $name state: $state")
    }
}

class ButtonWhite(val number: Int, toggle: Boolean) : Switch(toggle) {
    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) 127 else 0)
        println("white button $name state: $state")
    }
}

class ButtonRgb(val number: Int, toggle: Boolean) : Switch(toggle) {
    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) 122 else 0)
        println("rgb button $name state: $state")
    }
}

class Push2Elements {
    private val elements: Map<String, MidiElement> = mapOf(
        "pad_t1_s7" to Pad(44, TOGGLE),
        "pad_t1_s8" to Pad(36, MOMENTARY),
        "pot_t1" to Erp(71, 0),
        "dispa_t1" to ButtonRgb(102, MOMENTARY),
        "dispb_t1" to ButtonRgb(20, TOGGLE),
        "repeat" to ButtonWhite(56, TOGGLE),
        "select" to ButtonWhite(48, MOMENTARY)
    )
    fun register(midi: Push2Midi, mapper: Push2Mapper) {
        for ((key, element) in elements.entries) {
            element.name = key
            element.mapper = mapper
            element.updatePush2(midi)
            element.registerForMidi(midi)
        }
    }
    fun getElement(name: String) : MidiElement? {
        return elements.get(name)
    }
}
