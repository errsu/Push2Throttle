import com.neovisionaries.ws.client.*
import java.io.IOException
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

// WebSocket client to JMRIs WebServer
//
// Uses https://github.com/TakahikoKawasaki/nv-websocket-client
// See also http://takahikokawasaki.github.io/nv-websocket-client/
// To start the web server, open DecoderPro and in the "Actions"
// menu select "Start Web Server".

class JmriWsClient : WebSocketAdapter() {
    private val wsFactory = WebSocketFactory().setConnectionTimeout(5000) // in ms
    private var ws: WebSocket? = null
    private val pingTimer = Timer()
    private var pingTimerTask : TimerTask? = null
    private var messageCallback: (Any?) -> Unit = {}

    fun connect(callback: (Any?) -> Unit ) {
        if (isConnected()) {
            disconnect()
        }

        try {
            ws = wsFactory.createSocket("ws://localhost:12080/json/")
                    .addListener(this)
                    .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                    .connect()

            pingTimerTask = pingTimer.scheduleAtFixedRate(5000, 5000) {
                sendTextMessage("""{"type":"ping"}""")
            }

            messageCallback = callback
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

    fun isConnected() : Boolean {
        return ws?.isOpen ?: false
    }

    fun disconnect() {
        messageCallback = {}
        pingTimerTask?.cancel()
        pingTimerTask = null
        ws?.disconnect()
        ws?.removeListener(this)
        ws = null
    }

    /* how to work with map to Any?

    fun typeOf(value: Any?): String {
        return when(value) {
            null -> "null"
            else -> value::class.toString()
        }
    }

    fun main(args: Array<String>) {
        var m : MutableMap<String,Any?> = HashMap()
        m["a"] = "1"
        m["b"] = 1
        m["c"] = true
        m["d"] = null
        m["e"] = -1.0
        for (key in m.keys) {
          println("$key: ${m[key]} ${typeOf(m[key])}")
        }
    }
    */

    override fun onTextMessage(websocket: WebSocket?, message: String?) {
        if (message != null)
        {
            val n = JsmnParser(0).parse(message)
            if (n > 0)
            {
                val parser = JsmnParser(n)
                val tree = parser.parseToTree(message)
                messageCallback(tree)
            }
        }
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

    fun sendTree(tree: Any?) {
        val text = JsmnFormatter().formatTree(tree)
        sendTextMessage(text)
    }
}
