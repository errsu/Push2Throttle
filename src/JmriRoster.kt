class JmriRoster(private val rosterChangedCallback: () -> Unit) {

    val locos = HashMap<String, Loco>()
    private val jmri: JmriWsClient = JmriWsClient()

    fun connectToJmri() {
        locos.keys.clear()
        jmri.connect(this::jmriCallback)
        jmri.sendTextMessage("""{"list":"roster"}""")
    }

    fun disconnectFromJmri() {
        jmri.disconnect()
    }

    private val intAttrsInComment = mapOf(
            "maxSpeed" to Regex("([0-9]+) km/h"),
            "slot"     to Regex("slot ([0-9]+)"),
            "color"    to Regex("color ([0-9]+)"))

    private fun updateLocoFromData(loco: Loco, data: Map<*, *>) : Boolean {

        var updated = false
        val address = data["address"]
        if (address is String) {
            val intAddress = address.toInt()
            if (loco.assignAttr("address", intAddress)) {
                updated = true
            }
        }
        val comment = data["comment"]
        if (comment is String) {
            for ((attr, regex) in intAttrsInComment) {
                if (regex.containsMatchIn(comment)) {
                    val value = regex.find(comment)!!.groups[1]!!.value
                    if (loco.assignAttr(attr, value.toInt())) {
                        updated = true
                    }
                }
            }
        }
        return updated
    }

    private fun addOrUpdateLocoFromData(data: Map<*,*>) : Boolean {
        var rosterChanged = false
        val locoName = data["name"]
        if (locoName is String && locoName.length > 0) {
            var loco = locos[locoName]
            if (loco == null) {
                loco = Loco(locoName)
                locos[locoName] = loco
                rosterChanged = true
            }
            if (updateLocoFromData(loco, data)) {
                rosterChanged = true
            }
        }
        return rosterChanged
    }

    private fun jmriCallback(tree: Any?) {
        var rosterChanged = false
        if (tree is Map<*,*> && tree["type"] == "hello") {
            // hello ignored
        } else if (tree is List<*>) {
            // list of rosterEntries (or empty list)
            for (entry in tree) {
                if (entry is Map<*,*> && entry["type"] == "rosterEntry") {
                    val data = entry["data"] as Map<*,*>
                    if (addOrUpdateLocoFromData(data)) {
                        rosterChanged = true
                    }
                }
            }
        } else if (tree is Map<*,*> && tree["type"] == "rosterEntry") {
            // single entry -- changed (or added?)
            val data = tree["data"] as Map<*,*>
            if (addOrUpdateLocoFromData(data)) {
                rosterChanged = true
            }
        } else if (tree is Map<*,*> && tree["type"] == "roster") {
            // roster Map with add/remove entries in data
            val data = tree["data"] as Map<*,*>
            if (data.containsKey("remove")) {
                val entryToRemove = data["remove"] as Map<*,*>
                if (entryToRemove["type"] == "rosterEntry") {
                    val removeData = entryToRemove["data"] as Map<*, *>
                    val locoName = removeData["name"]
                    if (locos.remove(locoName) != null) {
                        rosterChanged = true
                    }
                }
            }
            if (data.containsKey("add")) {
                val entryToAdd = data["add"] as Map<*,*>
                if (entryToAdd["type"] == "rosterEntry") {
                    val dataToAdd = entryToAdd["data"] as Map<*, *>
                    if (addOrUpdateLocoFromData(dataToAdd)) {
                        rosterChanged = true
                    }
                }
            }
        }
        if (rosterChanged) {
            rosterChangedCallback()
        }
    }
}
