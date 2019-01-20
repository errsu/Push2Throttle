import kotlin.experimental.and

// The Push2 loco function controller uses the Push2 Pads to control
// the F0...F32 functions of locos (via throttle). The controlled functions/locos
// depend on the selection. Selection is a property of the throttle controller.
// A throttle can only be selected if it is visible. The "standby selection" for
// other than the visible page is hold by a save/restore mechanism of the ScenePager.
// If a throttle controller is selected, all pads are used for its loco.
// If none is selected, the pads of each Push2 track are used to control the most
// important functions of the loco visible in that track, if any.

class Push2LocoFunctionController(
        private val elements: Push2Elements,
        throttleManager: ThrottleManager,
        private val pads: Array<Array<Pad>>, // pads[column][row]
        private val selectionManager: SelectionManager
) : MidiController {

    private fun setColorPaletteEntry(n: Int, r: Int, g: Int, b: Int, w: Int) {
        val data = arrayOf(
            0xF0, // start of sysex
            0x00, 0x21, 0x1D, 0x01, 0x01, // header
            0x03, // cmd
            n,
            r.and(0x7F), r.and(0x80).shr(7),
            g.and(0x7F), g.and(0x80).shr(7),
            b.and(0x7F), b.and(0x80).shr(7),
            w.and(0x7F), w.and(0x80).shr(7),
            0xF7 // end of sysex
        )
        elements.midi.sendSysex(data.map{it.toByte()}.toByteArray())
    }

    private val colorNumbers = mapOf(
            "R1" to 1,
            "R3" to 2,
            "Y2" to 3,
            "R2" to 4,
            "Y5" to 5,
            "Y6" to 6,
            "Y1" to 7,
            "Y3" to 8,
            "Y4" to 9,
            "G4" to 10,
            "G1" to 11,
            "G5" to 12,
            "G3" to 13,
            "G2" to 14,
            "G6" to 15,
            "B2" to 16,
            "B4" to 17,
            "B3" to 18,
            "B6" to 19,
            "B1" to 20,
            "B5" to 21,
            "V1" to 22,
            "V4" to 23,
            "V2" to 24,
            "R4" to 25,
            "V3" to 26)

    private fun darkerColor(colorNumber: Int): Int {
        return 65 + colorNumber * 2
    }

    private fun darkestColor(colorNumber: Int): Int {
        return 66 + colorNumber * 2
    }

    private val padColors = arrayOf(
        arrayOf("G1", "Y2", "R1", "Y1", "G2", "Y1", "G1", "B1").map{colorNumbers[it]},
        arrayOf("B2", "V1", "G3", "V2", "R2", "B2", "V1", "G3").map{colorNumbers[it]},
        arrayOf("Y3", "R3", "B3", "G6", "B4", "Y3", "R3", "B3").map{colorNumbers[it]},
        arrayOf("B5", "V3", "Y4", "R4", "G4", "B5", "V3", "Y4").map{colorNumbers[it]},
        arrayOf("Y5", "G2", "B6", "V4", "Y6", "Y5", "G1", "B6").map{colorNumbers[it]},
        arrayOf("G1", "Y2", "R1", "Y1", "G2", "R4", "Y1", "R1").map{colorNumbers[it]},
        arrayOf("B2", "V1", "G3", "V2", "R2", "V1", "G6", "V2").map{colorNumbers[it]},
        arrayOf("R4", "Y3", "B3", "G6", "B4", "Y3", "B3", "G5").map{colorNumbers[it]})


    init {
        repeat(throttleManager.slotCount) { slot ->
            throttleManager.throttleAtSlot(slot).locoFunctionController = this
        }
        setColorPaletteEntry(2, 216, 8, 0, 4) // make R3 a little brighter (was: 128, 4, 0, 4)
    }

    fun connectToElements() {
        val selectedColumn = selectionManager.getSelectedColumn()
        if (selectedColumn == -1) {
            repeat(8) { column  ->
                val loco = selectionManager.getThrottleAtColumn(column).loco
                if (loco != null) {
                    elements.connect(pads[column][0], this,
                            mapOf("onColor" to "red", "offColor" to "blue", "type" to "toggle"),
                            loco.f0.value)
                }
            }
        } else {
            val loco = selectionManager.getThrottleAtColumn(selectedColumn).loco
            if (loco != null) {
                elements.connect(pads[0][7], this,
                        mapOf("onColor" to "green", "offColor" to "yellow", "type" to "toggle"),
                        loco.f0.value)
            }
        }
    }

    fun disconnectFromElements() {
        pads.forEach { columnOfPads ->
            columnOfPads.forEach { pad ->
                elements.disconnect(pad)
            }
        }
    }

    fun locoAttrChanged(throttle: JmriThrottle, attrName: String, newValue: Any?) {
        val selectedColumn = selectionManager.getSelectedColumn()
        val pad = if (selectedColumn != -1) {
            when (throttle) {
                selectionManager.getThrottleAtColumn(selectedColumn) -> pads[0][7]
                else -> null
            }
        } else {
            val col = selectionManager.getThrottleColumn(throttle)
            if (col == -1) null else pads[col][0]
        }
        if (pad != null) {
            when(attrName) {
                "f0" -> elements.updateElementStateByJmri(pad, newValue)
            }
        }
    }

    override fun <T: Any> elementStateChanged(element: MidiElement, newValue: T) {
        val pad = if (element is Pad) element else return
        val selectedColumn = selectionManager.getSelectedColumn()

        if (selectedColumn != -1) {
            when (element) {
                pads[0][7] -> selectionManager.getThrottleAtColumn(selectedColumn).messageToJmri("F0", newValue)
            }
        } else {
            if (pad.row() == 0) {
                selectionManager.getThrottleAtColumn(pad.col()).messageToJmri("F0", newValue)
            }
        }
    }
}
