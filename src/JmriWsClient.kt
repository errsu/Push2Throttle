import com.neovisionaries.ws.client.*
import java.io.IOException

// WebSocket client to JMRIs WebServer
//
// Uses https://github.com/TakahikoKawasaki/nv-websocket-client
// See also http://takahikokawasaki.github.io/nv-websocket-client/
// To start the web server, open DecoderPro and in the "Actions"
// menu select "Start Web Server".

class JmriWsClient : WebSocketAdapter() {
    private val wsFactory = WebSocketFactory().setConnectionTimeout(5000) // in ms
    private var ws: WebSocket? = null

    fun connect() {
        try {
            ws = wsFactory.createSocket("ws://localhost:12080/json/")
                    .addListener(this)
                    .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                    .connect()
        }
        catch(e: IOException) {
            println("JmriWsClient.connect(): IOException ${e.message}")
            ws = null
        }
        catch(e: WebSocketException) {
            println("JmriWsClient.connect(): WebSocketException ${e.message}")
            ws = null
        }
    }

    fun is_connected() : Boolean {
        return ws?.isOpen() ?: false
    }

    fun disconnect() {
        ws?.disconnect()
        ws?.removeListener(this)
        ws = null
    }

    override fun onTextMessage(websocket: WebSocket?, message: String?) {
        println("ERRRSUMSG: " + message)
    }

    override fun onPingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        super.onPingFrame(websocket, frame)
        println("ERRSUPING: ping received")
    }

    override fun onPongFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        super.onPongFrame(websocket, frame)
        println("ERRSUPONG: pong received")
    }

    fun sendTextMessage(message: String) {
        ws?.sendText(message)
    }
}
