import kotlin.reflect.KProperty

class Push2Mapper(
        private val midi: Push2Midi,
        private val elements: Push2Elements,
        private val controllers: Map<String, ThrottleController>) {

    // TODO: what if an element controls more than one property, e.g. ERP(turn, touch)?
    fun <T> jmriThrottleStateChanged(throttleName: String, property: KProperty<*>, oldValue: T, newValue: T) {
        // println("State $throttleName jmriThrottleStateChanged: ${property.name} from $oldValue to $newValue")
        when (throttleName) {
            "T1" -> when (property.name) {
                "speed" -> elements.getElement("pot_t1")?.updateStateByJmri(newValue as Any, midi)
                "forward" -> elements.getElement("dispb_t1")?.updateStateByJmri(newValue as Any, midi)
            }
        }
    }

    fun <T> push2ElementStateChanged(elementName: String, newValue: T) {
        // println("State $elementName push2ElementStateChanged from to $newValue")
        when (elementName) {
            "pot_t1" -> controllers["T1"]?.messageToJmri("speed", newValue)
            "dispb_t1" -> controllers["T1"]?.messageToJmri("forward", newValue)
        }
    }
}
