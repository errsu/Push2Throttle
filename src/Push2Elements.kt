import java.util.*
import kotlin.concurrent.schedule

abstract class MidiElement() {
    var name: String = "noname"
    var mapper: Push2Mapper? = null
    abstract fun registerForMidi(midi: Push2Midi)
    abstract fun updatePush2(midi: Push2Midi)
    abstract fun updateStateByJmri(value: Any, midi: Push2Midi)
    fun updateJmri(newValue: Any) {
        mapper?.push2ElementStateChanged(name, newValue)
    }
}

class Erp(private val turnCcNumber: Int, private val touchNnNumber: Int) : MidiElement() {
    private var state = 0.0f
    private var min = 0.0f
    private var max = 1.0f
    private var delta = 1.0f / 128.0f // clockwise == more
    private var touched = false
    private var suppressEcho = false
    private val resetTimer = Timer()
    private var resetTimerTask : TimerTask? = null

    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("cc", turnCcNumber) { value ->
            if (value < 64) { // turn right
                state = minOf(state + delta * value, max)
            } else { // turn left
                state = maxOf(state + delta * (value - 128), min)
            }
            resetTimerTask?.cancel()
            suppressEcho = true
            resetTimerTask = resetTimer.schedule(10) {
                suppressEcho = false
            }
            updateJmri(state)
            updatePush2(midi)
        }
        midi.registerElement("nn", touchNnNumber) { value ->
            touched = (value > 0)
            updatePush2(midi)
        }
    }
    override fun updatePush2(midi: Push2Midi) {
        println("pot $name state: $state touched: $touched")
    }
    override fun updateStateByJmri(value: Any, midi: Push2Midi) {
        if (value is Float && !suppressEcho) { // TODO: how to update touch?
            state = value
            updatePush2(midi)
        }
    }
}

const val TOGGLE = true
const val MOMENTARY = false

abstract class Switch(private val toggle: Boolean) : MidiElement() {
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
    override fun updateStateByJmri(value: Any, midi: Push2Midi) {
        if (value is Boolean) {
            state = value
            updatePush2(midi)
        }
    }
}

class Pad(private val number: Int, toggle: Boolean) : Switch(toggle) {
    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("nn", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendNN(0, number, if (state) 122 else 0)
        println("pad $name state: $state")
    }
}

class ButtonWhite(private val number: Int, toggle: Boolean) : Switch(toggle) {
    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) 127 else 0)
        println("white button $name state: $state")
    }
}

class ButtonRgb(private val number: Int, toggle: Boolean) : Switch(toggle) {
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
            "pot_t2" to Erp(72, 1),
            "pot_t3" to Erp(73, 2),
            "pot_t4" to Erp(74, 3),
            "pot_t5" to Erp(75, 4),
            "pot_t6" to Erp(76, 5),
            "pot_t7" to Erp(77, 6),
            "pot_t8" to Erp(78, 7),
            "dispa_t1" to ButtonRgb(102, MOMENTARY),
            "dispb_t1" to ButtonRgb(20, TOGGLE),
            "dispb_t2" to ButtonRgb(21, TOGGLE),
            "dispb_t3" to ButtonRgb(22, TOGGLE),
            "dispb_t4" to ButtonRgb(23, TOGGLE),
            "dispb_t5" to ButtonRgb(24, TOGGLE),
            "dispb_t6" to ButtonRgb(25, TOGGLE),
            "dispb_t7" to ButtonRgb(26, TOGGLE),
            "dispb_t8" to ButtonRgb(27, TOGGLE),
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
        return elements[name]
    }
}
