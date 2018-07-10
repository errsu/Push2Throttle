class JmriThrottle(val name: String) {

    var loco: Loco? = null
    var controller: Push2ThrottleController? = null

    private val jmri: JmriWsClient = JmriWsClient()

    fun connectToLoco(newLoco: Loco) {
        check(loco == null) { "must disconnect before connecting to another |loco|" }
        loco = newLoco
        jmri.connect(this::messageFromJmri)
        jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"$name","address":${newLoco.address.value}}}""")
    }

    fun disconnect() {
        if (loco != null) {
            loco = null
            jmri.disconnect()
        }
    }

    private fun messageFromJmri(tree: Any?) {
        check(loco != null) { "no messages from JMRI expected unless connected" }
        if (tree is Map<*, *>) {
            if (tree["type"] == "throttle") {
                val data = tree["data"]
                if (data is Map<*,*>) {
                    val throttleName = data["throttle"]
                    if (throttleName is String && throttleName == name) {
                        for ((attrName, value) in data) {
                            if (attrName is String && loco!!.assignAttr(attrName, value)) {
                                controller?.locoAttrChanged(attrName, value)
                            }
                        }
                    }
                }
            }
        }
    }

    fun messageToJmri(propertyName: String, value: Any?) {
        jmri.sendTree(mapOf("type" to "throttle",
                            "data" to mapOf("throttle" to name,
                                            propertyName to value)))
    }
}
