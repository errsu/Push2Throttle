import java.util.*
import kotlin.concurrent.schedule

interface MidiController {
    fun <T : Any> elementStateChanged(element: MidiElement, newValue: T)
}

abstract class MidiElement {
    var name: String = "noname"
    var controller: MidiController? = null
    open fun setAttributes(mapping: Map<String,Any?>) {}
    abstract fun reset()
    abstract fun registerForMidi(midi: Push2MidiDriver)
    abstract fun updatePush2(midi: Push2MidiDriver)
    abstract fun updateStateByJmri(value: Any?, midi: Push2MidiDriver)
    fun updateJmri(newValue: Any) {
        controller?.elementStateChanged(this, newValue)
    }
}

class Erp(private val turnCcNumber: Int, private val touchNnNumber: Int) : MidiElement() {
    private var state = 0.0f
    private val min = 0.0f
    private val max = 1.0f
    private var delta = 1.0f / 128.0f // clockwise == more
    private var touched = false
    private var suppressEcho = false
    private val resetTimer = Timer()
    private var resetTimerTask : TimerTask? = null

    override fun reset() {
        state = 0.0f
        delta = 1.0f / 128.0f
        touched = false
        suppressEcho = false
        resetTimerTask?.cancel()
        resetTimerTask = null
    }

    override fun setAttributes(mapping: Map<String,Any?>) {
        val newDelta = mapping["delta"]
        if (newDelta is Float && newDelta != 0.0f) {
            delta = newDelta
        }
    }

    override fun registerForMidi(midi: Push2MidiDriver) {
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
            updateJmri(state) // Q: why are we updating here but not on nn?
            updatePush2(midi) // Looks like a NOOP
        }
        midi.registerElement("nn", touchNnNumber) { value ->
            touched = (value > 0)
            // TODO: updateJmri2?
            updatePush2(midi)
        }
    }
    override fun updatePush2(midi: Push2MidiDriver) {
    }
    override fun updateStateByJmri(value: Any?, midi: Push2MidiDriver) {
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
    protected var offColor = if (isRgb) 0 else 0

    override fun reset() {
        toggle = false
        state = false
        onColor = if (isRgb) 122 else 127
        offColor = if (isRgb) 0 else 0
    }

    private fun colorIndex(color: Any?) : Int? {
        return when (color) {
            null -> null
            "red" -> if (isRgb) 127 else null
            "green" -> if (isRgb) 126 else null
            "blue" -> if (isRgb) 125 else null
            "yellow" -> if (isRgb) 7 else null
            "white" -> if (isRgb) 122 else 127
            "black" -> if (isRgb) 0 else 0
            is Int -> color
            else -> null
        }}

    fun onMidi(midi: Push2MidiDriver, value: Int) {
        var newState = state
        if (toggle) {
            if (value > 0) newState = !state
        } else {
            newState = (value > 0)
        }
        if (newState != state) {
            state = newState
            updateJmri(state)
            // TODO: for each Element, have a flag if it is reflecting a jmri state
            // if the flag is true, don't call updatePush2 here
            updatePush2(midi)
        }
    }

    override fun setAttributes(mapping: Map<String,Any?>) {
        onColor = colorIndex(mapping["onColor"]) ?: onColor
        offColor = colorIndex(mapping["offColor"]) ?: offColor
        if (mapping["type"] == "toggle") toggle = true
    }

    override fun updateStateByJmri(value: Any?, midi: Push2MidiDriver) {
        if (value is Boolean) {
            state = value
            updatePush2(midi)
        }
    }
}

class Pad(private val number: Int) : Switch(true) {

    override fun registerForMidi(midi: Push2MidiDriver) {
        midi.registerElement("nn", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2MidiDriver) {
        midi.sendNN(0, number, if (state) onColor else offColor)
    }
}

class ButtonWhite(private val number: Int) : Switch(false) {

    override fun registerForMidi(midi: Push2MidiDriver) {
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2MidiDriver) {
        midi.sendCC(0, number, if (state) onColor else offColor)
    }
}

class ButtonRgb(private val number: Int) : Switch(true) {

    override fun registerForMidi(midi: Push2MidiDriver) {
        midi.registerElement("cc", number) { value -> onMidi(midi, value) }
    }
    override fun updatePush2(midi: Push2MidiDriver) {
        midi.sendCC(0, number, if (state) onColor else offColor)
    }
}

class Push2Elements(val midi: Push2MidiDriver) {
    // TODO: use members (see Loco)

    private val elements: Map<String, MidiElement> = mapOf(
        "Pad S8 T1" to Pad(36), "Pad S8 T2" to Pad(37), "Pad S8 T3" to Pad(38), "Pad S8 T4" to Pad(39),
        "Pad S8 T5" to Pad(40), "Pad S8 T6" to Pad(41), "Pad S8 T7" to Pad(42), "Pad S8 T8" to Pad(43),
        "Pad S7 T1" to Pad(44), "Pad S7 T2" to Pad(45), "Pad S7 T3" to Pad(46), "Pad S7 T4" to Pad(47),
        "Pad S7 T5" to Pad(48), "Pad S7 T6" to Pad(49), "Pad S7 T7" to Pad(50), "Pad S7 T8" to Pad(51),
        "Pad S6 T1" to Pad(52), "Pad S6 T2" to Pad(53), "Pad S6 T3" to Pad(54), "Pad S6 T4" to Pad(55),
        "Pad S6 T5" to Pad(56), "Pad S6 T6" to Pad(57), "Pad S6 T7" to Pad(58), "Pad S6 T8" to Pad(59),
        "Pad S5 T1" to Pad(60), "Pad S5 T2" to Pad(61), "Pad S5 T3" to Pad(62), "Pad S5 T4" to Pad(63),
        "Pad S5 T5" to Pad(64), "Pad S5 T6" to Pad(65), "Pad S5 T7" to Pad(66), "Pad S5 T8" to Pad(67),
        "Pad S4 T1" to Pad(68), "Pad S4 T2" to Pad(69), "Pad S4 T3" to Pad(70), "Pad S4 T4" to Pad(71),
        "Pad S4 T5" to Pad(72), "Pad S4 T6" to Pad(73), "Pad S4 T7" to Pad(74), "Pad S4 T8" to Pad(75),
        "Pad S3 T1" to Pad(76), "Pad S3 T2" to Pad(77), "Pad S3 T3" to Pad(78), "Pad S3 T4" to Pad(79),
        "Pad S3 T5" to Pad(80), "Pad S3 T6" to Pad(81), "Pad S3 T7" to Pad(82), "Pad S3 T8" to Pad(83),
        "Pad S2 T1" to Pad(84), "Pad S2 T2" to Pad(85), "Pad S2 T3" to Pad(86), "Pad S2 T4" to Pad(87),
        "Pad S2 T5" to Pad(88), "Pad S2 T6" to Pad(89), "Pad S2 T7" to Pad(90), "Pad S2 T8" to Pad(91),
        "Pad S1 T1" to Pad(92), "Pad S1 T2" to Pad(93), "Pad S1 T3" to Pad(94), "Pad S1 T4" to Pad(95),
        "Pad S1 T5" to Pad(96), "Pad S1 T6" to Pad(97), "Pad S1 T7" to Pad(98), "Pad S1 T8" to Pad(99),

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

    fun register() {
        for ((key, element) in elements.entries) {
            element.name = key
            element.updatePush2(midi)
            element.registerForMidi(midi)
        }
    }
    fun getElement(name: String) : MidiElement? {
        return elements[name]
    }
    fun updateElementStateByJmri(element: MidiElement, value: Any?) {
        element.updateStateByJmri(value, midi)
    }

    fun connect(element: MidiElement,
                controller: MidiController,
                attributes: Map<String, Any?>,
                value: Any?) {
        element.setAttributes(attributes)
        element.controller = controller
        element.updateStateByJmri(value, midi)
    }

    fun disconnect(element: MidiElement) {
        element.controller = null
        element.reset()
        element.updatePush2(midi)
    }
}
