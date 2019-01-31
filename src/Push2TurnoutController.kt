// Manages the interaction between the Push2 display button elements and
// the turnouts of the currently visible panel, if it is visible.

class Push2TurnoutController(
        private val elements: Push2Elements,
        private val turnoutTable: JmriTurnoutTable,
        private val pads: Array<Array<Pad>>, // pads[row][col]
        private val buttons: Array<ButtonRgb>,
        private val pager: Pager) : MidiController {

    fun String.d0() = elements.color2number[this] ?: 0
    fun String.d1() = elements.darkerColor(this.d0())
    fun String.d2() = elements.darkestColor(this.d0())

    private val panelColorsSelected = arrayOf(
            "black"     .d0(), // 0
            "rgb-blue"  .d0(), // 1  125   0x0000FF
            "lt-blue"   .d0(), // 2  16    0x00bbad
            "yellow"    .d0(), // 3  8     0xe4c200
            "mint"      .d0(), // 4  13    0x00b955
            "lt-green"  .d0(), // 5  10    0x00e631
            "pale-pink" .d0(), // 6  24    0x88425b
            "lt-rose"   .d0(), // 7  1    0xed5938
            "lt-yellow" .d0(), // 8  7    0xedda3c
            "lt-blue"   .d0(), // 9  16   0x00bbad
            "lt-indigo" .d0(), // 10 23    0xe157e3
            "hi-blue"   .d0(), // 11 48
            "lime"      .d0(), // 12 9     0x94ff18
            "red"       .d0()) // 13 2     0xd1170a

    private val panelColorsUnselected = arrayOf(
            "black"     .d1(), // 0
            "dk-blue"   .d1(), // 1
            "pale-blue" .d0(), // 2
            "yellow"    .d1(), // 3
            "mint"      .d1(), // 4
            "lt-green"  .d1(), // 5
            "pale-pink" .d1(), // 6
            "lt-rose"   .d1(), // 7
            "lt-yellow" .d1(), // 8
            "pale-blue" .d0(), // 9
            "lt-indigo" .d1(), // 10
            "lt-blue"   .d1(), // 11
            "lime"      .d1(), // 12
            "red"       .d1()) // 13

    private val panels = arrayOf(
            arrayOf( 0, 0, 2, 2, 2, 2, 2, 0),
            arrayOf( 0, 0, 2, 6, 5, 5, 5, 3),
            arrayOf( 1, 1, 2, 6, 8, 7, 5, 3),
            arrayOf( 4, 4, 2, 6, 6, 7, 3, 3),
            arrayOf( 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(11,11,11,11, 0, 9, 9, 0),
            arrayOf(13,10,10,10,10, 9, 0, 0),
            arrayOf(13,13,13,13,13,12,12, 0))

//    private val panels = arrayOf(
//            arrayOf( 113, 115,   0,   0,   0,   0,   0,   0),
//            arrayOf(  97,  99, 101, 103, 105, 107, 109, 111),
//            arrayOf(  81,  83,  85,  87,  89,  91,  93,  95),
//            arrayOf(  65,  67,  69,  71,  73,  75,  77,  79),
//            arrayOf(  25,  26,   0,   0,   0,   0,   0,  48),
//            arrayOf(  17,  18,  19,  20,  21,  22,  23,  24),
//            arrayOf(   9,  10,  11,  12,  13,  14,  15,  16),
//            arrayOf(   1,   2,   3,   4,   5,   6,   7,   8))

    var turnouts = arrayOfNulls<Turnout?>(16)

    private fun connectPadToPanelSelector(pad: Pad, panel: Int) {
//        val onOffColor = panel
        val onOffColor = if (pager.currentPage() == panel) {
            panelColorsSelected[panel]
        } else {
            panelColorsUnselected[panel]
        }

        elements.connect(pad, this,
                mapOf("onColor" to onOffColor, "offColor" to onOffColor, "type" to "momentary"),
                false)
    }

    fun connectToElements() {
        turnouts.forEachIndexed { index, turnout ->
            if (turnout != null) {
                elements.connect(buttons[index], this,
                        mapOf("onColor" to "red", "offColor" to "green", "type" to "toggle"),
                        turnout.state.value == TurnoutState.THROWN)
            }
        }
        repeat(8) { col ->
            repeat(8) { row ->
                val pad = pads[row][col] // rank is in range 1..8 by scanning regex
                val panel = panels[row][col]
                connectPadToPanelSelector(pad, panel)
            }
        }
    }

    fun disconnectFromElements() {
        turnouts.forEachIndexed { index, _ ->
            elements.disconnect(buttons[index])
        }
        pads.forEach { rowOfPads ->
            rowOfPads.forEach { pad ->
                elements.disconnect(pad)
            }
        }
    }

    fun turnoutAttrChanged(turnout: Turnout, attrName: String, newValue: Any?) {
        for (index in turnouts.indices) {
            if (turnout == turnouts[index]) {
                when(attrName) {
                    "state" -> elements.updateElementStateByJmri(
                            buttons[index],
                            newValue == TurnoutState.THROWN)
                }
                break
            }
        }
    }

    override fun <T: Any> elementStateChanged(element: MidiElement, newValue: T) {
        if (element is Pad) {
            if (newValue == true) {
                val panel = panels[element.row()][element.col()]
                println("activating panel $panel")
                pager.gotoPage(panel)
            }
        } else if (element is ButtonRgb) {
            for (index in buttons.indices) {
                if (element == buttons[index]) {
                    if (turnouts[index] != null) {
                        turnoutTable.messageToJmri(
                                turnouts[index]!!, "state",
                                if (newValue == true) TurnoutState.THROWN else TurnoutState.CLOSED)
                    }
                }
            }
        }
    }
}
