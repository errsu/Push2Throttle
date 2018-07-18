class TurnoutManager {
    val turnouts = JmriTurnouts(this::turnoutsChangedCallback)
    var turnoutsReassigned : () -> Unit = {}

    private fun turnoutsChangedCallback() {
        // TODO: arrange the turnouts on the panels/buttons
        turnoutsReassigned()
    }
}
