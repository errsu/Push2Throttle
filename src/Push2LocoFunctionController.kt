import tornadofx.mapEach

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

    private val padColors = arrayOf(
            arrayOf("green", "orange", "lt-rose", "lt-yellow", "pale-cyan", "lt-yellow", "green", "pale-blue"),
            arrayOf("lt-blue", "indigo", "mint", "pale-pink", "dk-orange", "lt-blue", "indigo", "mint"),
            arrayOf("yellow", "red", "dk-blue", "cyan", "blue", "yellow", "red", "dk-blue"),
            arrayOf("lt-navy", "pink", "lime", "rose", "lt-green", "lt-navy", "pink", "lime"),
            arrayOf("lt-brown", "pale-cyan", "navy", "lt-indigo", "dk-brown", "lt-brown", "green", "navy"),
            arrayOf("green", "orange", "lt-rose", "lt-yellow", "pale-cyan", "rose", "lt-yellow", "lt-rose"),
            arrayOf("lt-blue", "indigo", "mint", "pale-pink", "dk-orange", "indigo", "cyan", "pale-pink"),
            arrayOf("rose", "yellow", "dk-blue", "cyan", "blue", "yellow", "dk-blue", "dk-lime"))
        .map{ it.map{ color -> elements.color2number[color] ?: 0 }}

    init {
        repeat(throttleManager.slotCount) { slot ->
            throttleManager.throttleAtSlot(slot).locoFunctionController = this
        }
    }

    fun ThrottleManager.getLocoInfo(loco: Loco) : LocoInfo? {
        return locoData.getInfoForMfgModel(loco.mfg.value, loco.model.value)
    }

    fun connectPadToLocoFunc(pad: Pad, locoFunc: LocoFunc, loco: Loco, color: Int) {
        val onColor : Any? = when (locoFunc.behavior) {
            FuncBehavior.M -> color
            FuncBehavior.T -> color
            FuncBehavior.I -> elements.darkestColor(color)
        }
        val offColor = when (locoFunc.behavior) {
            FuncBehavior.M -> elements.darkerColor(color)
            FuncBehavior.T -> elements.darkestColor(color)
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
        // have default settings for unconnected pads
        repeat(8) { row ->
            repeat(8) {col ->
                elements.connect(pads[row][col], null,
                    mapOf("onColor" to elements.color2number["lt-gray"],
                        "offColor" to elements.color2number["lt-black"],
                        "type" to "momentary"), false)
            }
        }

        selectionManager.getSelectedColumn()?.also {selectedColumn ->
            selectionManager.getThrottleAtColumn(selectedColumn).loco?.also { loco ->
                throttleManager.getLocoInfo(loco)?.functions?.forEach { _, locoFunc ->
                    // pad and color row/cols are identical
                    throttleManager.locoData.getRowColForFunction(locoFunc.stdName)?.also {(row, col) ->
                        connectPadToLocoFunc(pads[row][col], locoFunc, loco, padColors[row][col])
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
