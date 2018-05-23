import tornadofx.*

class MyApp: App(MyView::class)

class MyView: View() {

    private val jmri = JmriWsClient()

    override val root = vbox {
        button("TestRun") {
            action {
                val midi = Push2Midi()
                midi.test()
                test(jmri)
            }
        }
    }
}

fun test(jmri: JmriWsClient) {

    jmri.connect()
    // RSUMSG: {"type":"hello","data":{"JMRI":"4.10+R419243e","json":"4.0",
    //          "heartbeat":13500,"railroad":"Meine JMRI Bahngesellschaft",
    //          "node":"jmri-309C233EFC0E-3df9f267","activeProfile":"ErrsuSimulationsAG"}}

    jmri.sendTextMessage("""{"type":"ping"}""")
    // RSUMSG: {"type":"pong"}

    jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"L0","address":0}}""")
    // RSUMSG: {"type":"throttle","data":{"address":0,"speed":0.0,"forward":true,
    //          "F0":false,"F1":false,"F2":false,"F3":false,"F4":false,"F5":false,
    //          "F6":false,"F7":false,"F8":false,"F9":false,"F10":false,"F11":false,
    //          "F12":false,"F13":false,"F14":false,"F15":false,"F16":false,"F17":false,
    //          "F18":false,"F19":false,"F20":false,"F21":false,"F22":false,"F23":false,
    //          "F24":false,"F25":false,"F26":false,"F27":false,"F28":false,
    //          "speedSteps":126,"clients":1,"throttle":"L0"}}

    jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"L0","speed":1.0}}""")
    // RSUMSG: {"type":"throttle","data":{"speed":1.0,"throttle":"L0"}}
    jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"L0","speed":0.8}}""")
    // RSUMSG: {"type":"throttle","data":{"speed":0.8,"throttle":"L0"}}
    jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"L0","speed":0.4}}""")
    // RSUMSG: {"type":"throttle","data":{"speed":0.4,"throttle":"L0"}}
    jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"L0","speed":0.2}}""")
    // RSUMSG: {"type":"throttle","data":{"speed":0.2,"throttle":"L0"}}

    // listen to incoming changes:
    jmri.sendTextMessage("""{"type":"throttle","data":{"throttle":"L0"}}""")
    // RSUMSG: {"type":"throttle","data":{"speed":0.75396824,"throttle":"L0"}}

    // responses are coming in asynchronously, so keep the web socket open
    // jmri.disconnect()
}
