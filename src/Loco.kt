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
}

class Loco(val name: String) {

    var attrs = HashMap<String, Attribute<*>>()

    val test       = addAttr(Attribute("Test", false))
    val address    = addAttr(Attribute("address", 0))
    val speed      = addAttr(Attribute("speed", 0.0f))
    val forward    = addAttr(Attribute("forward", false))
    val f0         = addAttr(Attribute("F0", false))
    val f1         = addAttr(Attribute("F1", false))
    val f2         = addAttr(Attribute("F2", false))
    val f3         = addAttr(Attribute("F3", false))
    val f4         = addAttr(Attribute("F4", false))
    val f5         = addAttr(Attribute("F5", false))
    val f6         = addAttr(Attribute("F6", false))
    val f7         = addAttr(Attribute("F7", false))
    val speedSteps = addAttr(Attribute("speedSteps", 126))
    val maxSpeed   = addAttr(Attribute("maxSpeed", 80))
    val slot       = addAttr(Attribute("slot", 0))  // 0 is "no slot", valid slots start at 1
    val color      = addAttr(Attribute("color", 123)) // see Push2Display for valid colors

    private fun <T : Any> addAttr(attr: Attribute<T>) : Attribute<T>{
        attrs[attr.name] = attr
        return attr
    }

    fun assignAttr(attrName: String, value: Any?) : Boolean {
        return attrs[attrName]?.assign(value) ?: false
    }

    override fun toString() : String {
        fun b(attr: Attribute<Boolean>): String {
            return if (attr.value) "T" else "F"
        }
        return """Loco{name=$name, addr=$address, speed=$speed, fwd=${b(forward)}, """ +
                """F0-F7=[${b(f0)}, ${b(f1)}, ${b(f2)}, ${b(f3)}, """ +
                """${b(f4)}, ${b(f5)}, ${b(f6)}, ${b(f7)}], """ +
                """maxSpeed=$maxSpeed, slot=$slot}, color=$color}"""
    }
}
