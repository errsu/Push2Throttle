class ThrottleManager {
    val roster = JmriRoster(this::rosterChangedCallback)
    var throttlesReassigned : () -> Unit = {}  // callback used by scene manager
    private val throttles = Array(24) {JmriThrottle("T${it + 1}")}

    fun throttleAtSlot(slot: Int) : JmriThrottle {
        return throttles[slot]
    }

    private fun rosterChangedCallback() {
        // rearrange the locos into slots

        for (throttle in throttles) {
            throttle.disconnect()
        }
        // first, put all locos with slots into place
        val locosToPlace = roster.locos.values.sortedBy { it.name }.toMutableList()
        for (loco in roster.locos.values) {
            if (loco.slot.value != 0) {
                val slot = loco.slot.value - 1
                if (throttles[slot].loco == null) {
                    throttles[slot].connectToLoco(loco)
                    locosToPlace.remove(loco)
                } else {
                    println("Warning: loco slot conflict " +
                            "(${loco.name} vs. ${throttles[slot].loco!!.name}, slot ${loco.slot.value})")
                }
            }
        }
        for (slot in throttles.indices) {
            if (throttles[slot].loco == null) {
                // found free slot
                if (locosToPlace.isEmpty()) {
                    break // done
                }
                val loco = locosToPlace.removeAt(0)
                throttles[slot].connectToLoco(loco)
            }
        }
        if (locosToPlace.isNotEmpty()) {
            println("Warning: too many locos (only ${throttles.size} allowed)")
        }
        throttlesReassigned()
    }
}