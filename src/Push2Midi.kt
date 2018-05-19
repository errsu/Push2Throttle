
import javax.sound.midi.*


class Push2Midi () : Receiver {
    var push2Opened = false
    var push2InPort: MidiDevice? = null
    var push2OutPort: MidiDevice? = null

    override fun send(msg: MidiMessage, timeStamp: Long) {
      println(msg.message.asList())
    }

    override fun close() {

    }

    fun test () {
        println("looking for MIDI devices")
        val infos = MidiSystem.getMidiDeviceInfo()
        for (i in infos.indices) {
            try {
                if (infos[i].name == "Ableton Push 2") {
                    val device = MidiSystem.getMidiDevice(infos[i])
                    if (device.maxTransmitters != 0) {
                        device.open()
                        push2InPort = device
                    }
                    else if (device.maxReceivers != 0) {
                        device.open()
                        push2OutPort = device
                    }
                }
            } catch (e: MidiUnavailableException) {
                println(e.message)
            }
        }

        println("$push2OutPort open: ${push2OutPort?.isOpen}")
        if (push2OutPort != null) {
            println("push2InPort")
            println("receivers: ${push2OutPort?.receivers?.size}")
            println("transmitters: ${push2OutPort?.transmitters?.size}")
            val rx = push2OutPort?.getReceiver()
            println("rx: ${rx}")
            val myMsg = ShortMessage()
            val white = 122
            myMsg.setMessage(ShortMessage.NOTE_ON, 0, 36, white)
            val timeStamp: Long = -1
            rx?.send(myMsg, timeStamp)
        }

        println("$push2InPort open: ${push2InPort?.isOpen}")
        if (push2InPort != null) {
            println("push2OutPort")
            println("receivers: ${push2InPort?.receivers?.size}")
            println("transmitters: ${push2InPort?.transmitters?.size}")
            val tx = push2InPort?.getTransmitter()
            println("tx: ${tx}")
            tx?.receiver = this
        }
    }
}
