package push2throttle

class JmriThrottle(val slot: Int, val name: String) {

    var loco: Loco? = null
    var throttleController: Push2ThrottleController? = null
    var locoFunctionController: Push2LocoFunctionController? = null

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
                                throttleController?.locoAttrChanged(attrName, value)
                                locoFunctionController?.locoAttrChanged(this, attrName, value)
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
