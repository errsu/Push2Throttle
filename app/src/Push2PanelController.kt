package push2throttle

// Manages the currently visible turnout panel as selected by pads.

class Push2PanelController(
        private val elements: Push2Elements,
        private val pads: Array<Array<Pad>>, // pads[row][col]
        private val pager: Pager) : MidiController {

    fun String.d0() = Push2Colors.nameToNumber[this] ?: 0
    fun String.d1() = Push2Colors.darkerLEDColor(this.d0())
    fun String.d2() = Push2Colors.darkestLEDColor(this.d0())

    private val panelColorsSelected = arrayOf(
            "dk-white"  .d0(), //  0
            "rgb-blue"  .d0(), //  1
            "mint"      .d0(), //  2
            "lt-blue"   .d0(), //  3
            "pale-pink" .d0(), //  4
            "lt-yellow" .d0(), //  5
            "lt-rose"   .d0(), //  6
            "lt-green"  .d0(), //  7
            "yellow"    .d0(), //  8
            "hi-blue"   .d0(), //  9
            "lt-indigo" .d0(), // 10
            "red"       .d0(), // 11
            "lt-blue"   .d0(), // 12
            "lime"      .d0()) // 13

    private val panelColorsUnselected = arrayOf(
            "gray"      .d0(), //  0
            "dk-blue"   .d1(), //  1
            "mint"      .d1(), //  2
            "pale-blue" .d0(), //  3
            "pale-pink" .d1(), //  4
            "lt-yellow" .d1(), //  5
            "lt-rose"   .d1(), //  6
            "lt-green"  .d1(), //  7
            "yellow"    .d1(), //  8
            "lt-blue"   .d1(), //  9
            "lt-indigo" .d1(), // 10
            "red"       .d1(), // 11
            "pale-blue" .d0(), // 12
            "lime"      .d1()) // 13

    private val panels = arrayOf(
            arrayOf( 0,  0,  3,  3,  3,  3,  3,  0),
            arrayOf( 0,  0,  3,  4,  7,  7,  7,  8),
            arrayOf( 1,  1,  3,  4,  5,  6,  7,  8),
            arrayOf( 2,  2,  3,  4,  4,  6,  8,  8),
            arrayOf( 0,  0,  0,  0,  0,  0,  0,  0),
            arrayOf( 9,  9,  9,  9,  0, 12, 12,  0),
            arrayOf(11, 10, 10, 10, 10, 12,  0,  0),
            arrayOf(11, 11, 11, 11, 11, 13, 13,  0))

    private fun connectPadToPanelSelector(pad: Pad, panel: Int) {
        val onColor = panelColorsSelected[panel]
        val offColor = panelColorsUnselected[panel]

        elements.connect(pad, this,
                mapOf("onColor" to onColor, "offColor" to offColor, "type" to "trigger"),
                pager.currentPage() == panel)
    }

    fun connectToElements() {
        repeat(8) { col ->
            repeat(8) { row ->
                val pad = pads[row][col] // rank is in range 1..8 by scanning regex
                val panel = panels[row][col]
                connectPadToPanelSelector(pad, panel)
            }
        }
    }

    fun disconnectFromElements() {
        pads.forEach { rowOfPads ->
            rowOfPads.forEach { pad ->
                elements.disconnect(pad)
            }
        }
    }

    fun selectedPanelChanged() {
        repeat(8) { col ->
            repeat(8) { row ->
                val pad = pads[row][col] // rank is in range 1..8 by scanning regex
                val panel = panels[row][col]
                elements.updateElementStateByJmri(pad, panel == pager.currentPage())
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
        }
    }
}
