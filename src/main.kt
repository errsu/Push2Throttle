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
        button("print states") {
            action {
                ctrl.printStates()
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
    private val jmri0 = JmriWsClient()
    private val jmri1 = JmriWsClient()
    private val jmri2 = JmriWsClient()
    private val model = DataModel()

    fun connectToJmri() {
        println("JMRI0")
        jmri0.connect(this::messageFromJmri)
        println("connected: ${jmri0.is_connected()}")
        // declare address and listen to incoming changes:
        jmri0.sendTextMessage("""{"type":"throttle","data":{"throttle":"T0","address":0}}""")
        println("connected now: ${jmri0.is_connected()}")

        println("JMRI1")
        jmri1.connect(this::messageFromJmri)
        println("connected: ${jmri1.is_connected()}")
        jmri1.sendTextMessage("""{"type":"throttle","data":{"throttle":"T1","address":1}}""")
        println("connected now: ${jmri1.is_connected()}")

        println("JMRI2")
        jmri2.connect(this::messageFromJmri)
        println("connected: ${jmri2.is_connected()}")
        // declare address and listen to incoming changes:
        jmri2.sendTextMessage("""{"type":"throttle","data":{"throttle":"T2","address":2}}""")
        println("connected now: ${jmri2.is_connected()}")
    }
    fun disconnectFromJmri() {
        jmri0.disconnect()
        println("disconnected: ${!jmri0.is_connected()}")
        jmri1.disconnect()
        println("disconnected: ${!jmri1.is_connected()}")
        jmri2.disconnect()
        println("disconnected: ${!jmri2.is_connected()}")
    }

    fun modifyThrottle() {
        jmri0.sendTextMessage("""{"type":"throttle","data":{"throttle":"T0","speed":0.2}}""")
        jmri1.sendTextMessage("""{"type":"throttle","data":{"throttle":"T1","speed":0.4}}""")
        jmri2.sendTextMessage("""{"type":"throttle","data":{"throttle":"T2","speed":0.6}}""")
    }

    fun printStates() {
        for (i in 0 until model.throttleCount) {
            println("T$i ${model.throttleStates[i]}")
        }
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
