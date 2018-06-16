import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class ThrottleState(
        private val name: String,
        private val mapper: Push2Mapper) {
            private var address: Int = 0
            private var speed: Float by Delegates.observable(0.0f, this::changed)
            private var forward: Boolean by Delegates.observable(true, this::changed)
            private var f0: Boolean by Delegates.observable(false, this::changed)
            private var f1: Boolean by Delegates.observable(false, this::changed)
            private var f2: Boolean by Delegates.observable(false, this::changed)
            private var f3: Boolean by Delegates.observable(false, this::changed)
            private var f4: Boolean by Delegates.observable(false, this::changed)
            private var f5: Boolean by Delegates.observable(false, this::changed)
            private var f6: Boolean by Delegates.observable(false, this::changed)
            private var f7: Boolean by Delegates.observable(false, this::changed)
            private var speedSteps: Int = 126

    @Suppress("UNUSED_PARAMETER")
    private fun <T> changed(property: KProperty<*>, oldValue: T, newValue: T) {
        mapper.jmriThrottleStateChanged(name, property, newValue)
    }

    fun updateFromJmri(data: Map<String,Any?>) {
        for ((key, value) in data) {
            when (key) {
                "address" -> if (value is Int) address = value
                "speed"   -> if (value is Float) speed = value
                "forward" -> if (value is Boolean) forward = value
                "F0"      -> if (value is Boolean) f0 = value
                "F1"      -> if (value is Boolean) f1 = value
                "F2"      -> if (value is Boolean) f2 = value
                "F3"      -> if (value is Boolean) f3 = value
                "F4"      -> if (value is Boolean) f4 = value
                "F5"      -> if (value is Boolean) f5 = value
                "F6"      -> if (value is Boolean) f6 = value
                "F7"      -> if (value is Boolean) f7 = value
                "speedSteps" -> if(value is Int) speedSteps = value
            }
        }
    }

    override fun toString() : String {
        return """$name {addr: $address, speed: $speed, fwd: $forward, """ +
               """F0-F7: [$f0, $f1, $f2, $f3, $f4, $f5, $f6, $f7]}"""
    }
}
