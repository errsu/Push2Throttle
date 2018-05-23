import tornadofx.*
import java.io.*
import com.neovisionaries.ws.client.*

class MyApp: App(MyView::class)

class MyView: View() {

    override val root = vbox {
        button("TestRun") {
            action {
                val midi = Push2Midi()
                midi.test()
                try {
                    EchoClient.main(arrayOf())
                }
                catch (e: Exception) {
                    println(e.message)
                }
            }
        }
    }
}

object EchoClient {
    /**
     * The echo server on websocket.org.
     */
    // private val SERVER = "ws://echo.websocket.org"
    private val SERVER = "ws://localhost:12080/json/"

    /**
     * The timeout value in milliseconds for socket connection.
     */
    private val TIMEOUT = 5000


    /**
     * Wrap the standard input with BufferedReader.
     */
    private val input: BufferedReader
        @Throws(IOException::class)
        get() = BufferedReader(InputStreamReader(System.`in`))


    /**
     * The entry point of this command line application.
     */
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Connect to the echo server.
        val ws = connect()

        // RSUMSG: {"type":"hello","data":{"JMRI":"4.10+R419243e","json":"4.0",
        //          "heartbeat":13500,"railroad":"Meine JMRI Bahngesellschaft",
        //          "node":"jmri-309C233EFC0E-3df9f267","activeProfile":"ErrsuSimulationsAG"}}
        ws.sendText("""{"type":"ping"}""")
        // RSUMSG: {"type":"pong"}
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","address":0}}""")
        // RSUMSG: {"type":"throttle","data":{"address":0,"speed":0.0,"forward":true,
        //          "F0":false,"F1":false,"F2":false,"F3":false,"F4":false,"F5":false,
        //          "F6":false,"F7":false,"F8":false,"F9":false,"F10":false,"F11":false,
        //          "F12":false,"F13":false,"F14":false,"F15":false,"F16":false,"F17":false,
        //          "F18":false,"F19":false,"F20":false,"F21":false,"F22":false,"F23":false,
        //          "F24":false,"F25":false,"F26":false,"F27":false,"F28":false,
        //          "speedSteps":126,"clients":1,"throttle":"L0"}}
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","speed":1.0}}""")
        // RSUMSG: {"type":"throttle","data":{"speed":1.0,"throttle":"L0"}}
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","speed":0.8}}""")
        // RSUMSG: {"type":"throttle","data":{"speed":0.8,"throttle":"L0"}}
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","speed":0.4}}""")
        // RSUMSG: {"type":"throttle","data":{"speed":0.4,"throttle":"L0"}}
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","speed":0.2}}""")
        // RSUMSG: {"type":"throttle","data":{"speed":0.2,"throttle":"L0"}}

        // how to listen to incoming changes:
        // {"type":"throttle","data":{"throttle":"L0"}}
        // RSUMSG: {"type":"throttle","data":{"speed":0.75396824,"throttle":"L0"}}

        // The standard input via BufferedReader.
        val inStream = input

        // A text read from the standard input.


        // Read lines until "exit" is entered.
        while (true) {
            var text = inStream.readLine() ?: break
            if (text == "exit") {
                // Finish this application.
                break
            }
            if (text == "ping") {
                ws.sendPing()
            }
            else {
                // Send the text to the server.
                ws.sendText(text)
            }
        }
        // Close the WebSocket.
        ws.disconnect()
    }


    /**
     * Connect to the server.
     */
    @Throws(IOException::class, WebSocketException::class)
    private fun connect(): WebSocket {
        return WebSocketFactory()
                .setConnectionTimeout(TIMEOUT)
                .createSocket(SERVER)
                .addListener(object : WebSocketAdapter() {
                    // A text message arrived from the server.
                    override fun onTextMessage(websocket: WebSocket?, message: String?) {
                        println("RSUMSG: " + message)
                    }

                    override fun onPingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                        super.onPingFrame(websocket, frame)
                        println("ERRSUPING: ping received")
                    }

                    override fun onPongFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                        super.onPongFrame(websocket, frame)
                        println("ERRSUPONG: pong received")
                    }
                })
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                .connect()
    }
}