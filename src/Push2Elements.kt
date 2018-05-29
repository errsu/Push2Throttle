class Pad(val name: String, val number: Int) {
    fun register(midi: Push2Midi) {
        midi.registerElement("nn", number) {
            _, _, value -> println("pad $name pressed: $value")}
    }
}

class Erp(val name: String, val number: Int) {
    fun register(midi: Push2Midi) {
        midi.registerElement("cc", number) {
            _, _, value -> println("pot $name turned: $value")}
    }
}

class ButtonWhite(val name: String, val number: Int) {
    fun register(midi: Push2Midi) {
        midi.registerElement("cc", number) {
            _, _, value -> println("white button $name pressed: $value")}
    }
}

class ButtonRgb(val name: String, val number: Int) {
    fun register(midi: Push2Midi) {
        midi.registerElement("cc", number) {
            _, _, value -> println("rgb button $name pressed: $value")}
    }
}

class Push2Elements {
    private val pad = Pad("pad_t1_s8", 36)
    private val pot = Erp("pot_t1", 71)
    private val button1 = ButtonWhite("below_disp_t1", 20)
    private val button2 = ButtonRgb("select", 48)
    fun register(midi: Push2Midi) {
        pad.register(midi)
        pot.register(midi)
        button1.register(midi)
        button2.register(midi)
    }
}
