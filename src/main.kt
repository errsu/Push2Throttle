import tornadofx.*

class Push2ThrottleApp: App(Push2ThrottleMainView::class)

class Push2ThrottleMainView: View() {
    private val ctrl: Push2ThrottleController by inject()

    init {
        title = "Push 2 Throttle"
        ctrl.connectToJmri()
    }

    override val root = hbox {
        minWidth = 320.0
        minHeight = 80.0

        button("MIDI") {
            action {
                Push2Midi().test()
            }
        }
        button("modify") {
            action {
                ctrl.modifyThrottle()
            }
        }
        button("reconnect") {
            action {
                ctrl.disconnectFromJmri()
                ctrl.connectToJmri()
            }
        }
    }
}

class Push2ThrottleController: Controller() {
    private val jmri = JmriWsClient()
    private val model = DataModel()

    fun connectToJmri() {
        jmri.connect(this::messageFromJmri)
        println("connected: ${jmri.is_connected()}")
        // declare address and listen to incoming changes:
        for (num in 0 until model.throttleCount) {
            jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"T$num","address":$num}}""")
        }
    }
    fun disconnectFromJmri() {
        jmri.disconnect()
        println("disconnected: ${!jmri.is_connected()}")
    }

    fun modifyThrottle() {
        jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"T0","speed":0.2}}""")
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
                    val name = properties["throttle"]
                    if (name is String && name.startsWith("T")) {
                        val num = name.substring(1).toInt()
                        if (num in 0 until model.throttleCount) {
                            model.updateThrottle(num, properties)
                        }
                    }
                }
            }
        }
    }
}
