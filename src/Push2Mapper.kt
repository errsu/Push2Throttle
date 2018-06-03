import java.io.File
import kotlin.reflect.KProperty

class Push2Mapper(
        private val midi: Push2Midi,
        private val elements: Push2Elements,
        private val controllers: Map<String, ThrottleController>) {

    private var jmriToPush2Mappings: Map<String,Map<String,String>>? = null
    private var push2ToJmriMappings: Map<String,Pair<String,String>>? = null

    private fun checkMappings(tree: Any?) : Boolean {
        if (tree !is Map<*,*> || tree.any{it.key !is String}) {
            return false
        }
        for (value in tree.values) {
            if (value !is Map<*,*> || value.any{it.key !is String || it.value !is String}) {
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
                val inverseMappings:  MutableMap<String,Pair<String,String>> = HashMap()
                for ((throttleName, propertyMappings) in mappings ?: emptyMap()) {
                    for ((propertyName, elementName) in propertyMappings) {
                        inverseMappings[elementName] = Pair(throttleName, propertyName)
                    }
                }
                jmriToPush2Mappings = mappings
                push2ToJmriMappings = inverseMappings
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
        val throttleName = push2ToJmriMappings?.get(elementName)?.first
        val propertyName = push2ToJmriMappings?.get(elementName)?.second
        if (throttleName != null && propertyName != null) {
            controllers[throttleName]?.messageToJmri(propertyName, newValue)
        }
    }
}
