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
        private val throttleManager: ThrottleManager,
        private val pads: Array<Array<Pad>>, // pads[row][col]
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

    fun ThrottleManager.getLocoInfo(loco: Loco) : LocoInfo? {
        return locoData.getInfoForMfgModel(loco.mfg.value, loco.model.value)
    }

    fun connectPadToLocoFunc(pad: Pad, locoFunc: LocoFunc, loco: Loco, color: Int) {
        val onColor : Any? = when (locoFunc.behavior) {
            FuncBehavior.M -> color
            FuncBehavior.T -> color
            FuncBehavior.I -> darkestColor(color)
        }
        val offColor = when (locoFunc.behavior) {
            FuncBehavior.M -> darkerColor(color)
            FuncBehavior.T -> darkestColor(color)
            FuncBehavior.I -> color
        }
        val padType = when (locoFunc.behavior) {
            FuncBehavior.M -> "momentary"
            FuncBehavior.T -> "toggle"
            FuncBehavior.I -> "toggle"
        }
        elements.connect(pad, this,
                mapOf("onColor" to onColor, "offColor" to offColor, "type" to padType),
                loco.attrs[locoFunc.function]?.value ?: 0)
    }

    fun connectToElements() {
        selectionManager.getSelectedColumn()?.also {selectedColumn ->
            selectionManager.getThrottleAtColumn(selectedColumn).loco?.also { loco ->
                throttleManager.getLocoInfo(loco)?.functions?.forEach { _, locoFunc ->
                    // pad and color row/cols are identical
                    throttleManager.locoData.getRowColForFunction(locoFunc.stdName)?.also {(row, col) ->
                        connectPadToLocoFunc(pads[row][col], locoFunc, loco, padColors[row][col] ?: 0)
                    }
                }
            }
        } ?: run {
            repeat(8) { col  ->
                selectionManager.getThrottleAtColumn(col).loco?.also { loco ->
                    throttleManager.getLocoInfo(loco)?.functions?.forEach { _, locoFunc ->
                        if (locoFunc.rank != null) {
                            val pad = pads[locoFunc.rank - 1][col] // rank is in range 1..8 by scanning regex
                            // colors from the function matrix are mapped to a column of pads for selected track
                            throttleManager.locoData.getRowColForFunction(locoFunc.stdName)?.also {(colorRow, colorCol) ->
                                val color = padColors[colorRow][colorCol] ?: 0
                                connectPadToLocoFunc(pad, locoFunc, loco, color)
                            }
                        }
                    }
                }
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

    fun locoAttrChanged(throttle: JmriThrottle, attrName: String, newValue: Any?) {
        selectionManager.getSelectedColumn()?.also { selectedColumn ->
            if (throttle == selectionManager.getThrottleAtColumn(selectedColumn)) {
                val loco = throttle.loco ?: throw Exception("there should be a loco if there is a selection")
                throttleManager.getLocoInfo(loco)?.functions?.get(attrName)?.also { locoFunc ->
                    throttleManager.locoData.getRowColForFunction(locoFunc.stdName)?.also { (row, col) ->
                        elements.updateElementStateByJmri(pads[row][col], newValue)
                    }
                }
            }
        } ?: run {
            selectionManager.getThrottleColumn(throttle)?.also { col ->
                throttle.loco?.also { loco ->
                    throttleManager.getLocoInfo(loco)?.functions?.get(attrName)?.also { locoFunc ->
                        val rank = locoFunc.rank  ?: throw Exception("no pad in unselected state without rank")
                        elements.updateElementStateByJmri(pads[rank - 1][col], newValue)
                    }
                }
            }
        }
    }

    private fun showFunction(column: Int, locoFunc: LocoFunc, value: Any?) {
        when (locoFunc.behavior) {
            FuncBehavior.T ->  value as Boolean
            FuncBehavior.I ->  !(value as Boolean)
            FuncBehavior.M ->  if (value == true) null else return
        }.also { action ->
            selectionManager.overlayView.showText(column, locoFunc.name, action)
        }
    }

    override fun <T: Any> elementStateChanged(element: MidiElement, newValue: T) {
        val pad = if (element is Pad) element else return
        val padRow = pad.row()
        val padCol = pad.col()
        selectionManager.getSelectedColumn()?.also { selectedColumn ->
            val throttle = selectionManager.getThrottleAtColumn(selectedColumn)
            val loco = throttle.loco ?: throw Exception("there should be a loco if there is a selection")
            throttleManager.locoData.getStdNameForRowCol(padRow, padCol)?.also { stdName ->
                throttleManager.getLocoInfo(loco)?.functions?.forEach { _, locoFunc ->
                    if (locoFunc.stdName == stdName) {
                        showFunction(selectedColumn, locoFunc, newValue)
                        throttle.messageToJmri(locoFunc.function, newValue)
                    }
                }
            }
        } ?: run {
            val throttle = selectionManager.getThrottleAtColumn(padCol)
            throttle.loco?.also { loco ->
                throttleManager.getLocoInfo(loco)?.functions?.forEach { _, locoFunc ->
                    if (locoFunc.rank == padRow + 1) {
                        showFunction(padCol, locoFunc, newValue)
                        throttle.messageToJmri(locoFunc.function, newValue)
                    }
                }
            }
        }
    }
}
