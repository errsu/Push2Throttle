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
            state = if (value < 64) { // turn right
                minOf(state + delta * value, max)
            } else { // turn left
                maxOf(state + delta * (value - 128), min)
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
        // println("pot $name state: $state touched: $touched")
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

abstract class Switch(val isRgb: Boolean) : MidiElement() {
    private var toggle = false
    var state = false

    protected var onColor = if (isRgb) 122 else 127
    protected var offColor = if (isRgb) 124 else 6

    private fun colorIndex(color: Any?) : Int? {
        return when (color) {
            null -> null
            "red" -> if (isRgb) 127 else null
            "green" -> if (isRgb) 126 else null
            "blue" -> if (isRgb) 125 else null
            "white" -> if (isRgb) 122 else 127
            "black" -> if (isRgb) 0 else 0
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

class Pad(private val number: Int) : Switch(true) {

    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("nn", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendNN(0, number, if (state) onColor else offColor)
        // println("pad $name state: $state")
    }
}

class ButtonWhite(private val number: Int) : Switch(false) {

    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) onColor else offColor)
        // println("white button $name state: $state")
    }
}

class ButtonRgb(private val number: Int) : Switch(true) {

    override fun registerForMidi(midi: Push2Midi) {
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) onColor else offColor)
        // println("rgb button $name state: $state")
    }
}

class Push2Elements {
    private val elements: Map<String, MidiElement> = mapOf(
        "Pad 11" to Pad(92), "Pad 12" to Pad(93), "Pad 13" to Pad(94), "Pad 14" to Pad(95), "Pad 15" to Pad(96), "Pad 16" to Pad(97), "Pad 17" to Pad(98), "Pad 18" to Pad(99),
        "Pad 21" to Pad(84), "Pad 22" to Pad(85), "Pad 23" to Pad(86), "Pad 24" to Pad(87), "Pad 25" to Pad(88), "Pad 26" to Pad(89), "Pad 27" to Pad(90), "Pad 28" to Pad(91),
        "Pad 31" to Pad(76), "Pad 32" to Pad(77), "Pad 33" to Pad(78), "Pad 34" to Pad(79), "Pad 35" to Pad(80), "Pad 36" to Pad(81), "Pad 37" to Pad(82), "Pad 38" to Pad(83),
        "Pad 41" to Pad(68), "Pad 42" to Pad(69), "Pad 43" to Pad(70), "Pad 44" to Pad(71), "Pad 45" to Pad(72), "Pad 46" to Pad(73), "Pad 47" to Pad(74), "Pad 48" to Pad(75),
        "Pad 51" to Pad(60), "Pad 52" to Pad(61), "Pad 53" to Pad(62), "Pad 54" to Pad(63), "Pad 55" to Pad(64), "Pad 56" to Pad(65), "Pad 57" to Pad(66), "Pad 58" to Pad(67),
        "Pad 61" to Pad(52), "Pad 62" to Pad(53), "Pad 63" to Pad(54), "Pad 64" to Pad(55), "Pad 65" to Pad(56), "Pad 66" to Pad(57), "Pad 67" to Pad(58), "Pad 68" to Pad(59),
        "Pad 71" to Pad(44), "Pad 72" to Pad(45), "Pad 73" to Pad(46), "Pad 74" to Pad(47), "Pad 75" to Pad(48), "Pad 76" to Pad(49), "Pad 77" to Pad(50), "Pad 78" to Pad(51),
        "Pad 81" to Pad(36), "Pad 82" to Pad(37), "Pad 83" to Pad(38), "Pad 84" to Pad(39), "Pad 85" to Pad(40), "Pad 86" to Pad(41), "Pad 87" to Pad(42), "Pad 88" to Pad(43),

        "Pot T1"     to Erp(71, 0),
        "Pot T2"     to Erp(72, 1),
        "Pot T3"     to Erp(73, 2),
        "Pot T4"     to Erp(74, 3),
        "Pot T5"     to Erp(75, 4),
        "Pot T6"     to Erp(76, 5),
        "Pot T7"     to Erp(77, 6),
        "Pot T8"     to Erp(78, 7),
        "Pot master" to Erp(79, 8),
        "Pot swing"  to Erp(15, 9),
        "Pot tempo"  to Erp(14, 10),

        "Disp A T1" to ButtonRgb(102),
        "Disp A T2" to ButtonRgb(103),
        "Disp A T3" to ButtonRgb(104),
        "Disp A T4" to ButtonRgb(105),
        "Disp A T5" to ButtonRgb(106),
        "Disp A T6" to ButtonRgb(107),
        "Disp A T7" to ButtonRgb(108),
        "Disp A T8" to ButtonRgb(109),

        "Disp B T1" to ButtonRgb(20),
        "Disp B T2" to ButtonRgb(21),
        "Disp B T3" to ButtonRgb(22),
        "Disp B T4" to ButtonRgb(23),
        "Disp B T5" to ButtonRgb(24),
        "Disp B T6" to ButtonRgb(25),
        "Disp B T7" to ButtonRgb(26),
        "Disp B T8" to ButtonRgb(27),

        "Mute"     to ButtonRgb(60),
        "Solo"     to ButtonRgb(61),
        "Stop"     to ButtonRgb(29),
        "Automate" to ButtonRgb(89),
        "Record"   to ButtonRgb(86),
        "Play"     to ButtonRgb(85),
        "1/32t"    to ButtonRgb(43),
        "1/32"     to ButtonRgb(42),
        "1/16t"    to ButtonRgb(41),
        "1/16"     to ButtonRgb(40),
        "1/8t"     to ButtonRgb(39),
        "1/8"      to ButtonRgb(38),
        "1/4t"     to ButtonRgb(37),
        "1/4"      to ButtonRgb(36),

        "Tap Tempo"    to ButtonWhite(3),
        "Metronome"    to ButtonWhite(9),
        "Delete"       to ButtonWhite(118),
        "Undo"         to ButtonWhite(119),
        "Convert"      to ButtonWhite(35),
        "Double Loop"  to ButtonWhite(117),
        "Quantize"     to ButtonWhite(116),
        "Duplicate"    to ButtonWhite(88),
        "New"          to ButtonWhite(87),
        "Fixed Length" to ButtonWhite(90),
        "Setup"        to ButtonWhite(30),
        "User"         to ButtonWhite(59),
        "Add Device"   to ButtonWhite(52),
        "Add Track"    to ButtonWhite(53),
        "Device"       to ButtonWhite(110),
        "Mix"          to ButtonWhite(112),
        "Browse"       to ButtonWhite(111),
        "Clip"         to ButtonWhite(113),
        "Master"       to ButtonWhite(28),
        "Up"           to ButtonWhite(46),
        "Down"         to ButtonWhite(47),
        "Left"         to ButtonWhite(44),
        "Right"        to ButtonWhite(45),
        "Repeat"       to ButtonWhite(56),
        "Accent"       to ButtonWhite(57),
        "Scale"        to ButtonWhite(58),
        "Layout"       to ButtonWhite(31),
        "Note"         to ButtonWhite(50),
        "Session"      to ButtonWhite(51),
        "Octave Up"    to ButtonWhite(55),
        "Octave Down"  to ButtonWhite(54),
        "Page Left"    to ButtonWhite(62),
        "Page Right"   to ButtonWhite(63),
        "Shift"        to ButtonWhite(49),
        "Select"       to ButtonWhite(48))

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
