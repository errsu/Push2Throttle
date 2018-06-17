import tornadofx.Controller

class ThrottleController(
        private val name: String,
        private val address: Int,
        mapper: Push2Mapper) : Controller() {

    private val jmri: JmriWsClient = JmriWsClient()
    private val state: ThrottleState = ThrottleState(name, mapper)

    fun connectToJmri() {
        jmri.connect(this::messageFromJmri)
        // declare address and listen to incoming changes:
        jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"$name","address":$address}}""")
    }
    fun disconnectFromJmri() {
        jmri.disconnect()
    }

    fun printStates() {
        println(state)
    }

    private fun messageFromJmri(tree: Any?) {
        if (tree == hashMapOf("type" to "pong")) {
            return // ignore pongs
        }
        if (tree is Map<*, *>) {
            if (tree["type"] == "throttle") {
                val data = tree["data"]
                if (data is Map<*,*>) {
                    @Suppress("UNCHECKED_CAST")
                    val properties = data as Map<String, Any?>
                    val msgName = properties["throttle"]
                    if (msgName is String && msgName == name) {
                        state.updateFromJmri(properties)
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