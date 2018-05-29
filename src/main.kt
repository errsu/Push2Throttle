import tornadofx.*

class Push2ThrottleApp: App(Push2ThrottleMainView::class)

class Push2ThrottleMainView: View() {
    private val controllers: Array<ThrottleController> = Array(8) { ThrottleController("T$it", it) }
    private val midi = Push2Midi()

    init {
        title = "Push 2 Throttle"
        for (ctrl in controllers) {
            ctrl.connectToJmri()
        }
    }

    override val root = hbox {
        minWidth = 320.0
        minHeight = 80.0

        button("MIDI") {
            action {
                if (midi.isOpen) midi.close()
                midi.open()
                midi.test()
                midi.registerElement("nn", 36) {
                    nn, num, value -> println("lower left pad pressed ($nn, $num, $value)")}
            }
        }
        button("modify") {
            action {
                for (ctrl in controllers) {
                    ctrl.modifyThrottle()
                }
            }
        }
        button("print states") {
            action {
                for (ctrl in controllers) {
                    ctrl.printStates()
                }
            }
        }
        button("reconnect") {
            action {
                for (ctrl in controllers) {
                    ctrl.disconnectFromJmri()
                    ctrl.connectToJmri()
                }
            }
        }
    }
}

class ThrottleController(private val name: String, private val address: Int) : Controller() {
    private val jmri: JmriWsClient = JmriWsClient()
    private val state: ThrottleState = ThrottleState(name)

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
}
