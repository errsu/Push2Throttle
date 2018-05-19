
import javax.sound.midi.MidiUnavailableException
import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiDevice
import java.util.Vector
import javax.sound.midi.ShortMessage



class Push2Midi () {
    fun x() {
        println("looking for MIDI devices")
        var push2InPort: MidiDevice? = null
        var push2OutPort: MidiDevice? = null
        val infos = MidiSystem.getMidiDeviceInfo()
        for (i in infos.indices) {
            try {
                if (infos[i].name == "Ableton Push 2") {
                    val push2 = MidiSystem.getMidiDevice(infos[i])
                    if (push2.maxTransmitters != 0) {
                        push2InPort = push2
                        push2InPort.open()
                    }
                    else if (push2.maxReceivers != 0) {
                        push2OutPort = push2
                        push2OutPort.open()
                    }
                }
            } catch (e: MidiUnavailableException) {
                println(e.message)
            }
        }

        println("$push2OutPort open: ${push2OutPort?.isOpen}")
        if (push2OutPort != null) {
            println("push2InPort")
            println("receivers: ${push2OutPort.receivers.size}")
            println("transmitters: ${push2OutPort.transmitters.size}")
            val rx = push2OutPort.getReceiver()
            println("rx: ${rx}")
            val myMsg = ShortMessage()
            val white = 122
            myMsg.setMessage(ShortMessage.NOTE_ON, 0, 36, white)
            val timeStamp: Long = -1
            rx.send(myMsg, timeStamp)
        }

        println("$push2InPort open: ${push2InPort?.isOpen}")
        if (push2InPort != null) {
            println("push2OutPort")
            println("receivers: ${push2InPort.receivers.size}")
            println("transmitters: ${push2InPort.transmitters.size}")
            val tx = push2InPort.getTransmitter()
            println("tx: ${tx}")
        }
    }
}
