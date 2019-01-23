import tornadofx.*

class Push2ThrottleApp: App(Push2ThrottleMainView::class)

class Push2ThrottleMainView: View() {

    private val midi = Push2MidiDriver()
    private val libUsbHelper = LibUsbHelper()
    private val display = Push2Display()
    private val displayDriver = Push2DisplayDriver(libUsbHelper, display)
    private val elements = Push2Elements(midi)
    private val throttleManager = ThrottleManager()
    private val panelManager = PanelManager()
    private val sceneManager = SceneManager(display, elements, throttleManager, panelManager)

    init {
        title = "Push 2 Throttle"
        midi.open()
        displayDriver.open()
        elements.register()
        throttleManager.roster.connectToJmri()
        panelManager.turnoutTable.connectToJmri()
        sceneManager.gotoScene("throttles")
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
                println("------------------------------- Roster -------------------------------------------")
                for (loco in throttleManager.roster.locos.toSortedMap()) {
                    println(loco)
                }
                println("------------------------------- Turnouts -----------------------------------------")
                for (turnout in panelManager.turnoutTable.turnouts.toSortedMap()) {
                    println(turnout)
                }
                println("------------------------------- Loco Database ------------------------------------")
                throttleManager.locoData.print()
            }
        }
        button("reconnect JMRI") {
            action {
                throttleManager.roster.disconnectFromJmri()
                panelManager.turnoutTable.disconnectFromJmri()
                throttleManager.roster.connectToJmri() // will call rosterChangedCallback
                panelManager.turnoutTable.connectToJmri()
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
        button("memory test 0") {
            action {
                memoryTest("0")
            }
        }
        button("memory test 1") {
            action {
                memoryTest("1")
            }
        }
        button("memory test 2") {
            action {
                memoryTest("2")
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
    if (!jmri.isConnected()) jmri.connect(::messageFromJmri)
    jmri.sendTextMessage("""{"type":"power","data":{}}""")
    jmri.sendTextMessage("""{"list":"panels"}""")
    jmri.sendTextMessage("""{"type":"turnout","data":{"name":"NT1","state":4}}""")
    jmri.sendTextMessage("""{"type":"turnout","data":{"name":"NT22"}}""")
    jmri.sendTextMessage("""{"list":"turnouts"}""")
    // returns things like
    // {name=IT1, comment=null, state=0, userName=WI0, inverted=false}
    // {name=NT1, comment=null, state=4, userName=W0, inverted=false}
}

fun memoryTest(value: String) {
    if (!jmri.isConnected()) jmri.connect(::messageFromJmri)
    jmri.sendTextMessage("""{"type":"memory","data":{"name":"IM3WAYSTATE", "value":"$value"}}""")
    // returns things like
}

private fun messageFromJmri(tree: Any?) {
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
