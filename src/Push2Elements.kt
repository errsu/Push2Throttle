class Pad(val name: String, val number: Int) {
    var state = false
    fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("nn", number) {
            value ->
                state = (value > 0)
                update(midi)
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

class ButtonWhite(val name: String, val number: Int) {
    var state = false
    fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("cc", number) {
            value ->
                state = (value > 0)
                update(midi)
        }
    }
    fun update(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) 127 else 0)
        println("white button $name state: $state")
    }
}

class ButtonRgb(val name: String, val number: Int) {
    var state = false
    fun register(midi: Push2Midi) {
        update(midi)
        midi.registerElement("cc", number) {
            value ->
                state = (value > 0)
                update(midi)
        }
    }
    fun update(midi: Push2Midi) {
        midi.sendCC(0, number, if (state) 122 else 0)
        println("rgb button $name state: $state")
    }
}

class Push2Elements {
    private val pad = Pad("pad_t1_s8", 36)
    private val pot = Erp("pot_t1", 71, 0)
    private val button1 = ButtonRgb("below_disp_t1", 20)
    private val button2 = ButtonWhite("select", 48)
    fun register(midi: Push2Midi) {
        pad.register(midi)
        pot.register(midi)
        button1.register(midi)
        button2.register(midi)
    }
}
