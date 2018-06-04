import java.util.*
import kotlin.concurrent.schedule

abstract class MidiElement() {
    var name: String = "noname"
    var mapper: Push2Mapper? = null
    open fun setAttributes(mapping: Map<String,String>) {
    }
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

abstract class Switch : MidiElement() {
    private var toggle = false
    var state = false

    abstract fun isRgb() : Boolean
    protected var onColor = if (isRgb()) 122 else 127
    protected var offColor = 0

    private fun colorIndex(color: Any?) : Int? {
        return when (color) {
            null -> null
            "red" -> if (isRgb()) 127 else null
            "green" -> if (isRgb()) 126 else null
            "blue" -> if (isRgb()) 125 else null
            "white" -> if (isRgb()) 122 else 127
            "black" -> if (isRgb()) 0 else 0
            is Int -> color
            else -> null
        }}

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
    override fun setAttributes(mapping: Map<String,String>) {
        onColor = colorIndex(mapping["onColor"]) ?: onColor
        offColor = colorIndex(mapping["offColor"]) ?: offColor
        if (mapping["type"] == "toggle") toggle = true
    }

    override fun updateStateByJmri(value: Any, midi: Push2Midi) {
        if (value is Boolean) {
            state = value
            updatePush2(midi)
        }
    }
}

class Pad(private val number: Int) : Switch() {
    override fun isRgb() = true

    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("nn", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendNN(0, number, if (state) onColor else offColor)
        println("pad $name state: $state")
    }
}

class ButtonWhite(private val number: Int) : Switch() {
    override fun isRgb() = false

    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) onColor else offColor)
        println("white button $name state: $state")
    }
}

class ButtonRgb(private val number: Int) : Switch() {
    override fun isRgb() = true

    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) onColor else offColor)
        println("rgb button $name state: $state")
    }
}

class Push2Elements {
    private val elements: Map<String, MidiElement> = mapOf(
            "pad_11" to Pad(92), "pad_12" to Pad(93), "pad_13" to Pad(94), "pad_14" to Pad(95), "pad_15" to Pad(96), "pad_16" to Pad(97), "pad_17" to Pad(98), "pad_18" to Pad(99),
            "pad_21" to Pad(84), "pad_22" to Pad(85), "pad_23" to Pad(86), "pad_24" to Pad(87), "pad_25" to Pad(88), "pad_26" to Pad(89), "pad_27" to Pad(90), "pad_28" to Pad(91),
            "pad_31" to Pad(76), "pad_32" to Pad(77), "pad_33" to Pad(78), "pad_34" to Pad(79), "pad_35" to Pad(80), "pad_36" to Pad(81), "pad_37" to Pad(82), "pad_38" to Pad(83),
            "pad_41" to Pad(68), "pad_42" to Pad(69), "pad_43" to Pad(70), "pad_44" to Pad(71), "pad_45" to Pad(72), "pad_46" to Pad(73), "pad_47" to Pad(74), "pad_48" to Pad(75),
            "pad_51" to Pad(60), "pad_52" to Pad(61), "pad_53" to Pad(62), "pad_54" to Pad(63), "pad_55" to Pad(64), "pad_56" to Pad(65), "pad_57" to Pad(66), "pad_58" to Pad(67),
            "pad_61" to Pad(52), "pad_62" to Pad(53), "pad_63" to Pad(54), "pad_64" to Pad(55), "pad_65" to Pad(56), "pad_66" to Pad(57), "pad_67" to Pad(58), "pad_68" to Pad(59),
            "pad_71" to Pad(44), "pad_72" to Pad(45), "pad_73" to Pad(46), "pad_74" to Pad(47), "pad_75" to Pad(48), "pad_76" to Pad(49), "pad_77" to Pad(50), "pad_78" to Pad(51),
            "pad_81" to Pad(36), "pad_82" to Pad(37), "pad_83" to Pad(38), "pad_84" to Pad(39), "pad_85" to Pad(40), "pad_86" to Pad(41), "pad_87" to Pad(42), "pad_88" to Pad(43),
            "pot_t1" to Erp(71, 0),
            "pot_t2" to Erp(72, 1),
            "pot_t3" to Erp(73, 2),
            "pot_t4" to Erp(74, 3),
            "pot_t5" to Erp(75, 4),
            "pot_t6" to Erp(76, 5),
            "pot_t7" to Erp(77, 6),
            "pot_t8" to Erp(78, 7),
            "dispa_t1" to ButtonRgb(102),
            "dispa_t2" to ButtonRgb(103),
            "dispa_t3" to ButtonRgb(104),
            "dispa_t4" to ButtonRgb(105),
            "dispa_t5" to ButtonRgb(106),
            "dispa_t6" to ButtonRgb(107),
            "dispa_t7" to ButtonRgb(108),
            "dispa_t8" to ButtonRgb(109),
            "dispb_t1" to ButtonRgb(20),
            "dispb_t2" to ButtonRgb(21),
            "dispb_t3" to ButtonRgb(22),
            "dispb_t4" to ButtonRgb(23),
            "dispb_t5" to ButtonRgb(24),
            "dispb_t6" to ButtonRgb(25),
            "dispb_t7" to ButtonRgb(26),
            "dispb_t8" to ButtonRgb(27),
            "repeat" to ButtonWhite(56),
            "select" to ButtonWhite(48)
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
