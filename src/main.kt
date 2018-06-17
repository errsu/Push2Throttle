import tornadofx.*

class Push2ThrottleApp: App(Push2ThrottleMainView::class)

class Push2ThrottleMainView: View() {

    private val midi = Push2Midi()
    private val libUsbHelper = LibUsbHelper()
    private val displayContent = Push2DisplayContent()
    private val display = Push2Display(libUsbHelper, displayContent)
    private val elements = Push2Elements()
    private var controllers: MutableMap<String, ThrottleController> = HashMap()
    private val mapper: Push2Mapper = Push2Mapper(midi, elements, controllers, displayContent)
    private val roster = Roster(this::rosterCallback)

    init {
        title = "Push 2 Throttle"
        repeat(8) {
            val ctrl = ThrottleController("T${it + 1}", it, mapper)
            controllers["T${it + 1}"] = ctrl
            ctrl.connectToJmri()
        }
        roster.connectToJmri()
        midi.open()
        display.open()
        elements.register(midi, mapper)
    }

    private fun rosterCallback() {
        displayContent.rosterChanged(roster)
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
        button("list USB devices") {
            action {
                libUsbHelper.listDevices()
            }
        }
        button("JSMN get more data") {
            action {
                getMoreData()
            }
        }
    }
}

val jmri: JmriWsClient = JmriWsClient()

fun getMoreData() {
    // to see the turnouts here, open panel pro from
    // Decoder Pro -> File -> Open Panel Pro Window
    // Then load the panels with Panel Pro -> Panels -> OpenPanels
    // This can be automated:
    // Decoder Pro -> Edit -> Preferences
    // Aufstartverhalten -> Add (pull down) -> Open File
    // and choose the panels/tournouts xml file
    if (!jmri.isConnected()) jmri.connect(::jmriCallback)
    jmri.sendTextMessage("""{"type":"power","data":{}}""")
    jmri.sendTextMessage("""{"list":"panels"}""")
    jmri.sendTextMessage("""{"type":"turnout","data":{"name":"NT1","state":4}}""")
    jmri.sendTextMessage("""{"type":"turnout","data":{"name":"NT22"}}""")
    jmri.sendTextMessage("""{"list":"turnouts"}""")
    // returns things like
    // {name=IT1, comment=null, state=0, userName=WI0, inverted=false}
    // {name=NT1, comment=null, state=4, userName=W0, inverted=false}
}
private fun jmriCallback(tree: Any?) {
    println("$tree")
    if (tree is Map<*,*> && tree["type"] != "pong") println("> $tree")
    if (tree is List<*>) {
        for (it in tree) {
            println("---------------")
            if (it is Map<*,*>) {
                if (it["type"] == "turnout") {
                    val data = it["data"] as Map<*,*>
                    println("$data")
                }
            }
        }
    }
}
