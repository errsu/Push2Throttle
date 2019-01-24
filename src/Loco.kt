class Loco(val name: String) {

    var attrs = HashMap<String, Attribute<*>>()

    val address    = addAttr(Attribute("address", 0))
    val speed      = addAttr(Attribute("speed", 0.0f))
    val forward    = addAttr(Attribute("forward", false))

    init {
        for (i in 0..31) {
            addAttr(Attribute("F$i", false))
        }
    }
    val speedSteps = addAttr(Attribute("speedSteps", 126))
    val model      = addAttr(Attribute("model", ""))
    val mfg        = addAttr(Attribute("mfg", ""))
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
        return """Loco{name=$name, addr=$address, speed=$speed, fwd=${forward.b()}, """ +
                """F0-F7=[${attrs["f0"]?.b()}, ${attrs["f1"]?.b()}, ${attrs["f2"]?.b()}, ${attrs["f3"]?.b()}, """ +
                """${attrs["f4"]?.b()}, ${attrs["f5"]?.b()}, ${attrs["f6"]?.b()}, ${attrs["f7"]?.b()}], """ +
                """mfgModel=$model, mfg=$mfg, """ +
                """maxSpeed=$maxSpeed, slot=$slot, color=$color}"""
    }
}
