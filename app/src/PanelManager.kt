package push2throttle

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock

class PanelManager(private val display: Push2Display) {
    // - plays same role as ThrottleManager
    // - has information about panel geometries
    // - has relation between buttons and turnouts

    val turnoutTable = JmriTurnoutTable(this::turnoutTableChangedCallback, this::turnoutStateChangedCallback)
    var turnoutsReassigned : () -> Unit = {} // callback used by scene manager
    var turnoutsMoved : () -> Unit = {} // callback used by scene manager

    private fun turnoutTableChangedCallback() {
        runBlocking {
            display.driverStateMutex.withLock {
                turnoutsReassigned()
            }
        }
    }

    private fun turnoutStateChangedCallback() {
        runBlocking {
            display.driverStateMutex.withLock {
                turnoutsMoved()
            }
        }
    }
}
