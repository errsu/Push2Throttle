import kotlin.reflect.KProperty

class Push2Mapper(val midi: Push2Midi, val elements: Push2Elements) {

    fun <T> changed(stateName: String, property: KProperty<*>, oldValue: T, newValue: T) {
        println("State $stateName changed: ${property.name} from $oldValue to $newValue")
        when (stateName) {
            "T0" -> when (property.name) {
                "speed" -> elements.getElement("pot_t1")?.updateState(newValue as Any, midi)
                "forward" -> elements.getElement("dispb_t1")?.updateState(newValue as Any, midi)
            }
        }
    }
}
