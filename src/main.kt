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

