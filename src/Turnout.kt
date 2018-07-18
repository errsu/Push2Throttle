class Turnout(val name: String) {

    var attrs = HashMap<String, Attribute<*>>()

    val userName   = addAttr(Attribute("userName", ""))
    val state      = addAttr(Attribute("state", 0))
    val inverted   = addAttr(Attribute("inverted", false))

    private fun <T : Any> addAttr(attr: Attribute<T>) : Attribute<T>{
        attrs[attr.name] = attr
        return attr
    }

    fun assignAttr(attrName: String, value: Any?) : Boolean {
        return attrs[attrName]?.assign(value) ?: false
    }

    override fun toString() : String {
        return """Turnout{name=$name, userName=$userName, inverted=${inverted.b()}, state=$state}"""
    }
}
