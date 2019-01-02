object TurnoutState {
    // see ./java/src/jmri/server/json/JSON.java
    const val UNKNOWN = 0x00 // differs from NamedBean.UNKNOWN == 0x01
    const val CLOSED = 0x02
    const val THROWN = 0x04
    const val INCONSISTENT = 0x08
}

class JmriTurnout(val name: String) {

    var attrs = HashMap<String, Attribute<*>>()

    val userName   = addAttr(Attribute("userName", ""))
    val state      = addAttr(Attribute("state", 0))
    val inverted   = addAttr(Attribute("inverted", false))

    var controller: Push2TurnoutController? = null

    private fun <T : Any> addAttr(attr: Attribute<T>) : Attribute<T>{
        attrs[attr.name] = attr
        return attr
    }

    fun assignAttr(attrName: String, value: Any?) : Boolean {
        val success = attrs[attrName]?.assign(value) ?: false
        if (success) {
            controller?.turnoutAttrChanged(attrName, value)
        }
        return success
    }

    override fun toString() : String {
        return """JmriTurnout{name=$name, userName=$userName, inverted=${inverted.b()}, state=$state}"""
    }

    // TODO: check if this makes sense
    fun messageToJmri(turnouts: JmriTurnouts, propertyName: String, value: Any?) {
        turnouts.jmri.sendTree(mapOf("type" to "turnout",
                                     "data" to mapOf("name" to name,
                                     propertyName to value)))
    }
}
