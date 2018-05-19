
import javax.sound.midi.Synthesizer
import javax.sound.midi.MidiUnavailableException
import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiDevice
import java.util.Vector

class Push2Midi () {
    fun x() {
        println("looking for MIDI devices")
        val synthInfos: Vector<*>
        var device: MidiDevice
        val infos = MidiSystem.getMidiDeviceInfo()
        for (i in infos.indices) {
            try {
                device = MidiSystem.getMidiDevice(infos[i])
                val device_ins = device.getMaxTransmitters()
                val device_outs = device.getMaxReceivers()
                val device_ins_text = if (device_ins == -1) "unlimited" else device_ins.toString()
                val device_outs_ext = if (device_outs == -1) "unlimited" else device_outs.toString()

                println("$i: ${device.deviceInfo.name} / ${device.deviceInfo.description}")
                println("   ${device.maxReceivers} ${device.maxTransmitters} ${device.javaClass}")
                println("   ${device.javaClass.name}")
                println("   ins: ${device_ins_text} outs: ${device_outs_ext}")

                if (device.deviceInfo.name == "Ableton Push 2")
                {
                    if (device.getMaxTransmitters() != 0)
                    {
                        println("---> PUSH2 MIDI input port - receive midi from here")
                    }
                    if (device.getMaxReceivers() != 0)
                    {
                        println("---> PUSH2 MIDI output port - send midi here")
                    }
                }

            } catch (e: MidiUnavailableException) {
                println(e.message)
            }
//            if (device is Synthesizer) {
//                synthInfos.add(infos[i])
//            }
        }

    }
}
