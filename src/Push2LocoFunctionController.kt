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

    init {
        repeat(throttleManager.slotCount) { slot ->
            throttleManager.throttleAtSlot(slot).locoFunctionController = this
        }
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
            when (throttle) {
                selectionManager.getThrottleAtColumn(0) -> pads[0][0]
                selectionManager.getThrottleAtColumn(1) -> pads[1][0]
                selectionManager.getThrottleAtColumn(2) -> pads[2][0]
                selectionManager.getThrottleAtColumn(3) -> pads[3][0]
                selectionManager.getThrottleAtColumn(4) -> pads[4][0]
                selectionManager.getThrottleAtColumn(5) -> pads[5][0]
                selectionManager.getThrottleAtColumn(6) -> pads[6][0]
                selectionManager.getThrottleAtColumn(7) -> pads[7][0]
                else -> null
            }
        }
        if (pad != null) {
            when(attrName) {
                "f0" -> elements.updateElementStateByJmri(pad, newValue)
            }
        }
    }

    override fun <T: Any> elementStateChanged(element: MidiElement, newValue: T) {
        val selectedColumn = selectionManager.getSelectedColumn()
        if (selectedColumn != -1) {
            when (element) {
                pads[0][7] -> selectionManager.getThrottleAtColumn(selectedColumn).messageToJmri("F0", newValue)
            }
        } else {
            when (element) {
                pads[0][0] -> selectionManager.getThrottleAtColumn(0).messageToJmri("F0", newValue)
                pads[1][0] -> selectionManager.getThrottleAtColumn(1).messageToJmri("F0", newValue)
                pads[2][0] -> selectionManager.getThrottleAtColumn(2).messageToJmri("F0", newValue)
                pads[3][0] -> selectionManager.getThrottleAtColumn(3).messageToJmri("F0", newValue)
                pads[4][0] -> selectionManager.getThrottleAtColumn(4).messageToJmri("F0", newValue)
                pads[5][0] -> selectionManager.getThrottleAtColumn(5).messageToJmri("F0", newValue)
                pads[6][0] -> selectionManager.getThrottleAtColumn(6).messageToJmri("F0", newValue)
                pads[7][0] -> selectionManager.getThrottleAtColumn(7).messageToJmri("F0", newValue)
            }
        }
    }
}
