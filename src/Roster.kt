class Roster(private val rosterCallback: () -> Unit) {

    val name = HashMap<Int,String>()
    val maxSpeed = HashMap<Int,Int>()

    private val jmri: JmriWsClient = JmriWsClient()

    fun connectToJmri() {
        jmri.connect(this::jmriCallback)
        jmri.sendTextMessage("""{"list":"roster"}""")
    }

    fun disconnectFromJmri() {
        jmri.disconnect()
    }

    private fun jmriCallback(tree: Any?) {
        var gotRoster = false
        if (tree is List<*>) {
            for (entry in tree) {
                if (entry is Map<*,*>) {
                    if (entry["type"] == "rosterEntry") {
                        gotRoster = true
                        val data = entry["data"] as Map<*,*>
                        val address = data["address"]
                        if (address is String) {
                            val intAddress = address.toInt()
                            val nameValue = data["name"]
                            if (nameValue is String) name[intAddress] = nameValue

                            // interpreting comments as "nnn km/h"
                            val comment = data["comment"]
                            if (comment is String) {
                                val regex = Regex("([0-9]+) km/h")
                                if (regex.containsMatchIn(comment)) {
                                    val speedString = regex.find(comment)!!.groups[1]!!.value
                                    maxSpeed[intAddress] = speedString.toInt()
                                }
                            }
                            // val maxSpeedValue = data["maxSpeedPct"] max speed in percent??
                            // if (maxSpeedValue is String) maxSpeed[intAddress] = maxSpeedValue.toInt()
                        }
                    }
                }
            }
        }
        if (gotRoster) rosterCallback()
    }
}
