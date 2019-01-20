
import javax.sound.midi.*

class Push2MidiDriver : Receiver {
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

        // Windows: name "Ableton Push 2" for Live Ports
        //               "MIDIIN2 (Ableton Push 2)"
        //           and "MIDIOUT2 (Ableton Push 2)" for User Ports
        // Ubuntu:  description is "Ableton Push 2, USB MIDI, Ableton Push 2"
        //          name is "A2 [hw:1,0,0]" for Live Port
        //                  "A2 [hw:1,0,1]" for User Port
        val deviceName = "Ableton Push 2"
        val ubuntuNameRegex = Regex("A2 \\[hw:.,0,0\\]")

        for (i in infos.indices) {
            try {
                if (infos[i].name == deviceName ||
                        (infos[i].description.contains(deviceName) && infos[i].name.matches(ubuntuNameRegex))) {
                    val device = MidiSystem.getMidiDevice(infos[i])
                    if (push2InPort == null && device.maxTransmitters != 0) {
                        device.open()
                        push2InPort = device
                    } else if (push2OutPort == null && device.maxReceivers != 0) {
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
            println("Push2 MIDI opened")
        } else {
            close()
        }
    }

    fun sendCC(channel: Int, number: Int, value: Int) {
        val msg = ShortMessage()
        msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, number, value)
        val timeStamp: Long = -1
        outgoingPortReceiver?.send(msg, timeStamp)
    }

    fun sendNN(channel: Int, key: Int, velocity: Int) {
        val msg = ShortMessage()
        msg.setMessage(ShortMessage.NOTE_ON, channel, key, velocity)
        val timeStamp: Long = -1
        outgoingPortReceiver?.send(msg, timeStamp)
    }

    fun sendSysex(data: ByteArray) {
        val msg = SysexMessage(data, data.size)
        val timeStamp: Long = -1
        outgoingPortReceiver?.send(msg, timeStamp)
    }
}
