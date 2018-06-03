import tornadofx.*

class Push2ThrottleApp: App(Push2ThrottleMainView::class)

class Push2ThrottleMainView: View() {
    private val midi = Push2Midi()
    private val elements = Push2Elements()
    private var controllers: MutableMap<String, ThrottleController> = HashMap()
    private val mapper: Push2Mapper = Push2Mapper(midi, elements, controllers)

    init {
        title = "Push 2 Throttle"
        repeat(8) {
            val ctrl = ThrottleController("T$it", it, mapper)
            controllers["T$it"] = ctrl
            ctrl.connectToJmri()
        }
        midi.open()
        elements.register(midi, mapper)
    }

    override val root = hbox {
        minWidth = 320.0
        minHeight = 80.0

        button("reconnect MIDI") {
            action {
                if (midi.isOpen) midi.close()
                midi.open()
            }
        }
        button("modify") {
            action {
                for (ctrl in controllers.values) {
                    ctrl.modifyThrottle()
                }
            }
        }
        button("print states") {
            action {
                for (ctrl in controllers.values) {
                    ctrl.printStates()
                }
            }
        }
        button("reconnect JMRI") {
            action {
                for (ctrl in controllers.values) {
                    ctrl.disconnectFromJmri()
                    ctrl.connectToJmri()
                }
            }
        }
        button("test JSMN") {
            action {
                testJsmn()
            }
        }
    }
}

class ThrottleController(
        private val name: String,
        private val address: Int,
        mapper: Push2Mapper) : Controller() {

    private val jmri: JmriWsClient = JmriWsClient()
    val state: ThrottleState = ThrottleState(name, mapper)

    fun connectToJmri() {
        jmri.connect(this::messageFromJmri)
        println("$name connected: ${jmri.is_connected()}")
        // declare address and listen to incoming changes:
        jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"$name","address":$address}}""")
        println("$name connected now: ${jmri.is_connected()}")
    }
    fun disconnectFromJmri() {
        jmri.disconnect()
        println("$name disconnected: ${!jmri.is_connected()}")
    }

    fun modifyThrottle() {
        jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"$name","speed":0.2}}""")
    }

    fun printStates() {
        println("$name $state")
    }

    fun messageFromJmri(tree: Any?) {
        if (tree == hashMapOf("type" to "pong")) {
            return // ignore pongs
        }
        if (tree is Map<*, *>) {
            if (tree["type"] == "hello") {
                val data = tree["data"]
                if (data is Map<*,*>) {
                    println("""JMRI:    ${data["JMRI"]}""")
                    println("""RR:      ${data["railroad"]}""")
                    println("""Profile: ${data["activeProfile"]}""")
                }
            }
            if (tree["type"] == "throttle") {
                val data = tree["data"]
                if (data is Map<*,*>) {
                    @Suppress("UNCHECKED_CAST")
                    val properties = data as Map<String, Any?>
                    val msgName = properties["throttle"]
                    if (msgName is String && msgName == name) {
                        state.update(properties)
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
