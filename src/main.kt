import tornadofx.*

class Push2ThrottleApp: App(Push2ThrottleMainView::class)

class Push2ThrottleMainView: View() {

    private val midi = Push2Midi()
    private val libUsbHelper = LibUsbHelper()
    private val display = Push2Display(libUsbHelper)
    private val elements = Push2Elements()
    private var controllers: MutableMap<String, ThrottleController> = HashMap()
    private val mapper: Push2Mapper = Push2Mapper(midi, elements, controllers)

    init {
        title = "Push 2 Throttle"
        repeat(8) {
            val ctrl = ThrottleController("T${it + 1}", it, mapper)
            controllers["T${it + 1}"] = ctrl
            ctrl.connectToJmri()
        }
        midi.open()
        display.open()
        elements.register(midi, mapper)
    }

    override val root = hbox {
        minWidth = 320.0
        minHeight = 80.0

        button("reconnect Push2") {
            action {
                if (midi.isOpen) midi.close()
                midi.open()
                if (display.isOpen) display.close()
                display.open()
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
                for (throttleName in controllers.keys.sorted()) {
                    controllers[throttleName]?.printStates()
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
        button("test Display") {
            action {
                libUsbHelper.listDevices()
            }
        }
        combobox(display.pattern.selectedPatternProperty, display.pattern.patterns) {
            cellFormat {
                text = it
            }
        }
    }
}

