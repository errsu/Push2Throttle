class JmriTurnouts(private val turnoutsChangedCallback: () -> Unit) {

    val turnouts = HashMap<String, JmriTurnout>()
    val jmri: JmriWsClient = JmriWsClient()

    fun connectToJmri() {
        turnouts.keys.clear()
        jmri.connect(this::jmriCallback)
        jmri.sendTextMessage("""{"list":"turnouts"}""")
    }

    fun disconnectFromJmri() {
        jmri.disconnect()
    }

    private val intAttrsInComment = mapOf<String,Regex>()

    private fun updateTurnoutFromData(turnout: JmriTurnout, data: Map<*, *>) : Boolean {

        var updated = false
        for (attrName in listOf("userName", "state", "inverted")) {
            var value = data[attrName]
            if (attrName == "userName" && value == null) {
                value = ""
            }
            if (turnout.assignAttr(attrName, value)) {
//            if (turnout.attrs[attrName]?.assign(value) == true) {
                updated = true
            }
        }
        val comment = data["comment"]
        if (comment is String) {
            for ((attr, regex) in intAttrsInComment) {
                if (regex.containsMatchIn(comment)) {
                    val value = regex.find(comment)!!.groups[1]!!.value
                    if (turnout.assignAttr(attr, value.toInt())) {
                        updated = true
                    }
                }
            }
        }
        return updated
    }

    private fun addOrUpdateTurnoutFromData(data: Map<*,*>) : Boolean {
        var turnoutListChanged = false
        val turnoutName = data["name"]
        if (turnoutName is String && turnoutName.length > 0) {
            var turnout = turnouts[turnoutName]
            if (turnout == null) {
                turnout = JmriTurnout(turnoutName)
                turnouts[turnoutName] = turnout
                turnoutListChanged = true
            }
            if (updateTurnoutFromData(turnout, data)) {
                turnoutListChanged = true
            }
        }
        return turnoutListChanged
    }

    private fun jmriCallback(tree: Any?) {
        var turnoutListChanged = false
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
                        if (addOrUpdateTurnoutFromData(data)) {
                            turnoutListChanged = true
                        }
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
            if (addOrUpdateTurnoutFromData(data)) {
                turnoutListChanged = true
            }
        }
        if (turnoutListChanged) {
            turnoutsChangedCallback()
        }
    }
}
