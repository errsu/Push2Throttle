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
    private val ping_timer = Timer()
    private var ping_timer_task : TimerTask? = null

    fun connect() {
        if (is_connected()) {
            disconnect()
        }

        try {
            ws = wsFactory.createSocket("ws://localhost:12080/json/")
                    .addListener(this)
                    .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                    .connect()

            ping_timer_task = ping_timer.scheduleAtFixedRate(5000, 5000) {
                sendTextMessage("""{"type":"ping"}""")
            }
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
        ping_timer_task?.cancel()
        ping_timer_task = null
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
            println("ERRRSUMSG: " + message)
            val parser = JsmnParser(100)
            val n = parser.parse(message)
            if (n > 0)
            {
                val tokens = parser.tokens
                if (tokens[0].type == JsmnType.OBJECT
                    && tokens[0].size >= 1
                    && tokens[1].type == JsmnType.STRING
                    && message.substring(tokens[1].start, tokens[1].end) == "type"
                    && tokens[1].size == 1
                    && tokens[2].type == JsmnType.STRING) {

                    println("type = ${message.substring(tokens[2].start, tokens[2].end)}")

                    if (tokens[0].size == 2
                        && tokens[3].type == JsmnType.STRING
                        && message.substring(tokens[3].start, tokens[3].end) == "data"
                        && tokens[3].size == 1
                        && tokens[4].type == JsmnType.OBJECT) {

                        println("data = ${message.substring(tokens[4].start, tokens[4].end)}")
                        var data : MutableMap<String, Any?> = HashMap()
                        for (i in 0 .. tokens[4].size - 1) {
                            val keyToken = tokens[5 + 2 * i]
                            if (keyToken.type == JsmnType.STRING
                                && keyToken.size == 1) {
                                val key = message.substring(keyToken.start, keyToken.end)
                                val valueToken = tokens[5 + 2 * i + 1]
                                val valueString = message.substring(valueToken.start, valueToken.end)
                                when(valueToken.type) {
                                    JsmnType.STRING -> data[key] = valueString
                                    JsmnType.PRIMITIVE -> {
                                        when {
                                            valueString == "true"  -> data[key] = true
                                            valueString == "false" -> data[key] = false
                                            valueString == "null"  -> data[key] = null
                                            '.' in valueString     -> data[key] = valueString.toFloat()
                                            else                   -> data[key] = valueString.toInt()
                                        }
                                    }
                                    else -> println("unexpected value type")
                                }
                            }
                        }
                        println(data)
                    }
                }
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
}
