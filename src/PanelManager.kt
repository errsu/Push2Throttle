class PanelManager {
    // - plays same role as ThrottleManager
    // - has information about panel geometries
    // - has relation between buttons and turnouts

    val turnoutTable = JmriTurnoutTable(this::turnoutTableChangedCallback, this::turnoutStateChangedCallback)
    var turnoutsReassigned : () -> Unit = {} // callback used by scene manager
    var turnoutsMoved : () -> Unit = {} // callback used by scene manager
    var activePanel: PanelView? = null

    // TODO add function returning turnout for panel/button combination

    private fun turnoutTableChangedCallback() {
        turnoutsReassigned()
    }

    private fun turnoutStateChangedCallback() {
        turnoutsMoved()
    }
}
