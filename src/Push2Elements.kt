class Pad(val name: String, val number: Int) {
    var state = false
    fun register(midi: Push2Midi) {
        midi.registerElement("nn", number) {
            value ->
                state = (value > 0)
                println("pad $name pressed: $value state: $state")
        }
    }
}

class Erp(val name: String, val turn_cc_number: Int, val touch_nn_number: Int) {
    var state = 0.0f
    var min = 0.0f
    var max = 1.0f
    var delta = 1.0f / 128.0f // clockwise == more
    var touched = false
    fun register(midi: Push2Midi) {
        midi.registerElement("cc", turn_cc_number) {
            value ->
                if (value < 64) { // turn right
                    state = minOf(state + delta * value, max)
                } else { // turn left
                    state = maxOf(state + delta * (value - 128), min)
                }
                println("pot $name turned: $value state: $state touched: $touched")
        }
        midi.registerElement("nn", touch_nn_number) {
            value ->
                touched = (value > 0)
                println("pot $name touched: $value state: $state touched: $touched")
        }
    }
}

class ButtonWhite(val name: String, val number: Int) {
    var state = false
    fun register(midi: Push2Midi) {
        midi.registerElement("cc", number) {
            value ->
                state = (value > 0)
                println("white button $name pressed: $value state: $state")
        }
    }
}

class ButtonRgb(val name: String, val number: Int) {
    var state = false
    fun register(midi: Push2Midi) {
        midi.registerElement("cc", number) {
            value ->
                state = (value > 0)
                println("rgb button $name pressed: $value state: $state")
        }
    }
}

class Push2Elements {
    private val pad = Pad("pad_t1_s8", 36)
    private val pot = Erp("pot_t1", 71, 0)
    private val button1 = ButtonWhite("below_disp_t1", 20)
    private val button2 = ButtonRgb("select", 48)
    fun register(midi: Push2Midi) {
        pad.register(midi)
        pot.register(midi)
        button1.register(midi)
        button2.register(midi)
    }
}
