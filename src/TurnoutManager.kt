class TurnoutManager {
    val turnouts = JmriTurnouts(this::turnoutsChangedCallback)
    var turnoutsReassigned : () -> Unit = {}
    var activePanel: PanelView? = null

    fun turnoutWithUserName(userName: String) : JmriTurnout? {
        for (turnout in turnouts.turnouts.values) {
            if (turnout.userName.value == userName) {
                return turnout
            }
        }
        return null
    }

    private fun turnoutsChangedCallback() {
        activePanel?.update()
        turnoutsReassigned()
    }
}
