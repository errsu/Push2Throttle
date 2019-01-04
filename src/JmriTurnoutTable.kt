// For turnouts, plays the roles of JmriRoster and JmriThrottle (communication part)
class JmriTurnoutTable(
        private val turnoutTableChangedCallback: () -> Unit,
        private val turnoutStateChangedCallback: () -> Unit) {

    private enum class Change { NO_CHANGE, STATE_CHANGED, TABLE_CHANGED }
    private fun Change.raiseTo(level: Change) : Change {
        return when(this) {
            Change.NO_CHANGE -> level
            Change.STATE_CHANGED -> if (level == Change.TABLE_CHANGED) level else this
            Change.TABLE_CHANGED -> this
        }
    }

    val turnouts = HashMap<String, Turnout>()
    var controller: Push2TurnoutController? = null

    val jmri: JmriWsClient = JmriWsClient()

    fun turnoutWithUserName(userName: String) : Turnout? {
        for (turnout in turnouts.values) {
            if (turnout.userName.value == userName) {
                return turnout
            }
        }
        return null
    }

    fun connectToJmri() {
        turnouts.keys.clear()
        jmri.connect(this::messageFromJmri)
        jmri.sendTextMessage("""{"list":"turnouts"}""")
    }

    fun disconnectFromJmri() {
        jmri.disconnect()
    }

    private val intAttrsInComment = mapOf<String,Regex>()

    private fun updateTurnoutFromData(turnout: Turnout, data: Map<*, *>) : Change {

        var change = Change.NO_CHANGE
        for (attrName in listOf("userName", "state", "inverted")) {
            var value = data[attrName]
            if (attrName == "userName" && value == null) {
                value = ""
            }
            if (turnout.assignAttr(attrName, value)) {
                controller?.turnoutAttrChanged(turnout, attrName, value)
                change = change.raiseTo(if (attrName == "state") Change.STATE_CHANGED else Change.TABLE_CHANGED)
            }
        }
        val comment = data["comment"]
        if (comment is String) {
            for ((attr, regex) in intAttrsInComment) {
                if (regex.containsMatchIn(comment)) {
                    val value = regex.find(comment)!!.groups[1]!!.value
                    if (turnout.assignAttr(attr, value.toInt())) {
                        change = change.raiseTo(Change.TABLE_CHANGED)
                    }
                }
            }
        }
        return change
    }

    private fun addOrUpdateTurnoutFromData(data: Map<*,*>) : Change {
        var change = Change.NO_CHANGE
        val turnoutName = data["name"]
        if (turnoutName is String && turnoutName.length > 0) {
            var turnout = turnouts[turnoutName]
            if (turnout == null) {
                turnout = Turnout(turnoutName)
                turnouts[turnoutName] = turnout
                change = change.raiseTo(Change.TABLE_CHANGED)
            }
            change = change.raiseTo(updateTurnoutFromData(turnout, data))
        }
        return change
    }

    private fun messageFromJmri(tree: Any?) {
        var change = Change.NO_CHANGE
        if (tree is Map<*,*> && tree["type"] == "hello") {
            // hello ignored
        } else if (tree is List<*>) {
            // tree is list of turnouts, or empty list (ignored).
            if (tree.isNotEmpty()) {
                val newNames = mutableListOf<String>()
                for (entry in tree) {
                    if (entry is Map<*,*> && entry["type"] == "turnout") {
                        val data = entry["data"] as Map<*,*>
                        val name = data["name"]
                        if (name is String) {
                            newNames.add(name)
                        }
                        change = change.raiseTo(addOrUpdateTurnoutFromData(data))
                    }
                }
                // Deletion is sent as updated list only, so we have to
                // remove all entries that are missing from the updated list:
                for (turnoutName in turnouts.keys.iterator()) {
                    if (turnoutName !in  newNames) {
                        turnouts.keys.remove(turnoutName)
                    }
                }
            }
        } else if (tree is Map<*,*> && tree["type"] == "turnout") {
            // single entry -- changed (or added)
            val data = tree["data"] as Map<*,*>
            change = change.raiseTo(addOrUpdateTurnoutFromData(data))
        }
        when(change) {
            Change.NO_CHANGE -> {}
            Change.STATE_CHANGED -> turnoutStateChangedCallback()
            Change.TABLE_CHANGED -> turnoutTableChangedCallback()
        }
    }

    fun messageToJmri(turnout: Turnout, propertyName: String, value: Any?) {
        jmri.sendTree(mapOf("type" to "turnout",
                "data" to mapOf("name" to turnout.name, propertyName to value)))
    }
}
