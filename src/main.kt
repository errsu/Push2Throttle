import tornadofx.*

class Push2ThrottleApp: App(Push2ThrottleMainView::class)

class Push2ThrottleMainView: View() {

    private val midi = Push2MidiDriver()
    private val libUsbHelper = LibUsbHelper()
    private val display = Push2Display()
    private val displayDriver = Push2DisplayDriver(libUsbHelper, display)
    private val elements = Push2Elements(midi)
    private val throttleManager = ThrottleManager()
    private val sceneManager = SceneManager(display, elements, throttleManager)
    private val turnoutManager = TurnoutManager()

    init {
        title = "Push 2 Throttle"
        midi.open()
        displayDriver.open()
        elements.register()
        throttleManager.roster.connectToJmri()
        turnoutManager.turnouts.connectToJmri()
        sceneManager.gotoScene("throttles")
        throttleManager.throttlesReassigned = sceneManager::throttlesReassigned
    }

    override val root = hbox {
        minWidth = 320.0
        minHeight = 80.0

        button("reconnect Push2") {
            action {
                if (midi.isOpen) midi.close()
                midi.open()
                if (displayDriver.isOpen) displayDriver.close()
                displayDriver.open()
                sceneManager.gotoScene("throttles")
            }
        }
        button("print states") {
            action {
                for (loco in throttleManager.roster.locos.toSortedMap()) {
                    println(loco)
                }
                for (turnout in turnoutManager.turnouts.turnouts.toSortedMap()) {
                    println(turnout)
                }
            }
        }
        button("reconnect JMRI") {
            action {
                throttleManager.roster.disconnectFromJmri()
                turnoutManager.turnouts.disconnectFromJmri()
                throttleManager.roster.connectToJmri() // will call rosterChangedCallback
                turnoutManager.turnouts.connectToJmri()
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
