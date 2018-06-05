import java.io.File
import kotlin.reflect.KProperty

class Push2Mapper(
        private val midi: Push2Midi,
        private val elements: Push2Elements,
        private val controllers: Map<String, ThrottleController>) {

    private var jmriToPush2Mappings: Map<String,Map<String,String>>? = null
    private var push2ToJmriMappings: Map<String,Map<String,Any>>? = null

    private fun checkMappings(tree: Any?) : Boolean {
        if (tree !is Map<*,*> || tree.any{it.key !is String}) {
            return false
        }
        for (value in tree.values) {
            if (value !is Map<*,*> || value.any{it.key !is String || (it.value !is String && it.value !is Int)}) {
                return false
            }
        }
        return true
    }

    init {
        val file = File("mappings.json")
        val js = file.readText()
        val parserZ = JsmnParser(0)
        val n = parserZ.parse(js)
        if (n > 0) {
            val parser = JsmnParser(n)
            val tree = parser.parseToTree(js)
            if (checkMappings(tree)) {
                @Suppress("UNCHECKED_CAST")
                val mappings = tree as Map<String,Map<String, String>>?

                val inverseMappings:  MutableMap<String,MutableMap<String,String>> = HashMap()
                for ((elementName, mapping) in mappings ?: emptyMap()) {
                    val throttleName = mapping.get("throttle")
                    val propertyName = mapping.get("property")
                    if (throttleName != null && propertyName != null) {
                        inverseMappings.putIfAbsent(throttleName, HashMap())
                        inverseMappings[throttleName]?.put(propertyName, elementName)
                    }
                    elements.getElement(elementName)?.setAttributes(mapping)
                }
                push2ToJmriMappings = mappings
                jmriToPush2Mappings = inverseMappings
            } else {
                println("bad format of mappings.json file")
            }
        }
        else {
            parserZ.printError(js, n)
        }
    }
    // TODO: what if an element controls more than one property, e.g. ERP(turn, touch)?
    fun <T> jmriThrottleStateChanged(throttleName: String, property: KProperty<*>, newValue: T) {
        // println("State $throttleName jmriThrottleStateChanged: ${property.name} from $oldValue to $newValue")
        val elementName = jmriToPush2Mappings?.get(throttleName)?.get(property.name)
        if (elementName != null) {
            elements.getElement(elementName)?.updateStateByJmri(newValue as Any, midi)
        }
    }

    fun <T> push2ElementStateChanged(elementName: String, newValue: T) {
        // println("State $elementName push2ElementStateChanged from to $newValue")
        val throttleName = push2ToJmriMappings?.get(elementName)?.get("throttle")
        val propertyName = push2ToJmriMappings?.get(elementName)?.get("property")
        if (throttleName is String && propertyName is String) {
            controllers[throttleName]?.messageToJmri(propertyName, newValue)
        }
    }
}
