package push2throttle

open class Attribute<T : Any>(val name: String, var value: T) {
    val type = value::class
    fun assign  (newValue: Any?) : Boolean {
        if (newValue != null && newValue::class == type && value != newValue) {
            // value is T -> value::class -> type -> newValue::class -> newValue is T
            @Suppress("UNCHECKED_CAST")
            value = newValue as T
            return true
        }
        return false
    }
    override fun toString() : String {
        return value.toString()
    }
    fun b(): String {
        if (value is Boolean) {
            return if (value == true) "T" else "F"
        }
        return "?"
    }
}
