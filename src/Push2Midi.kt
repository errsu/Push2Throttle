
import javax.sound.midi.*

class Push2Midi () : Receiver {
    var isOpen = false
    private var push2InPort: MidiDevice? = null
    private var push2OutPort: MidiDevice? = null
    private var outgoingPortReceiver: Receiver? = null

    private var nnCallbacks: Array<((Int) -> Unit)?> = arrayOfNulls(128)
    private var ccCallbacks: Array<((Int) -> Unit)?> = arrayOfNulls(128)

    fun registerElement(type: String, number: Int, callback: (Int) -> Unit) {
        when (type) {
            "nn" -> nnCallbacks[number] = callback
            "cc" -> ccCallbacks[number] = callback
        }
    }

    override fun send(msg: MidiMessage, timeStamp: Long) {
        // this is where MIDI is coming in
        val data = msg.message
        if (data.size == 3)
        {
            // using integers because of Kotlins issues with unsigned constants
            val status: Int = data[0].toInt()
            val number: Int = data[1].toInt()
            val value:  Int = data[2].toInt()
            when (status and 0xF0) {
                0x90 -> nnCallbacks[number]?.invoke(value) ?: println("unhandled note-on $number $value")
                0x80 -> nnCallbacks[number]?.invoke(0)     ?: println("unhandled note-off $number $value")
                0xB0 -> ccCallbacks[number]?.invoke(value) ?: println("unhandled cc $number $value")
                else -> println("unhandled midi ${msg.message.asList()}")
            }
        } else {
            println("unhandled midi ${msg.message.asList()}")
        }
    }

    override fun close() {
        outgoingPortReceiver = null
        push2OutPort?.close()
        push2OutPort = null

        push2InPort?.transmitter?.receiver = null
        push2InPort?.close()
        push2InPort = null
    }

    fun open () {
        val infos = MidiSystem.getMidiDeviceInfo()
        for (i in infos.indices) {
            try {
                if (infos[i].name == "Ableton Push 2") {
                    val device = MidiSystem.getMidiDevice(infos[i])
                    if (device.maxTransmitters != 0) {
                        device.open()
                        push2InPort = device
                    } else if (device.maxReceivers != 0) {
                        device.open()
                        push2OutPort = device
                    }
                }
            } catch (e: MidiUnavailableException) {
                println(e.message)
            }
        }
        isOpen = (push2OutPort?.isOpen ?: false )&& (push2InPort?.isOpen ?: false)
        if (isOpen) {
            outgoingPortReceiver = push2OutPort?.receiver
            push2InPort?.transmitter?.receiver = this
        } else {
            close()
        }
    }

    fun test () {
        if (isOpen) {
            val myMsg = ShortMessage()
            val white = 122
            myMsg.setMessage(ShortMessage.NOTE_ON, 0, 36, white)
            val timeStamp: Long = -1
            outgoingPortReceiver?.send(myMsg, timeStamp)
        }
    }
}
