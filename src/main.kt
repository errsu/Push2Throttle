import tornadofx.*
import java.io.*
import com.neovisionaries.ws.client.*

class MyApp: App(MyView::class)

class MyView: View() {

    override val root = vbox {
        button("TestRun") {
            action {
                val midi = Push2Midi()
                midi.x()
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

        ws.sendText("""{"type":"ping"}""")
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","address":0}}""")
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","speed":1.0}}""")
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","speed":0.8}}""")
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","speed":0.4}}""")
        ws.sendText("""{"type":"throttle","data":{"throttle":"L0","speed":0.2}}""")

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
            // Send the text to the server.
            ws.sendText(text)
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
                        println(message)
                    }
                })
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                .connect()
    }
}