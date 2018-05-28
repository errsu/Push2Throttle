data class ThrottleState(
            val name: String,
            var address: Int = 0,
            var speed: Float = 0.0f,
            var forward: Boolean = true,
            var f0: Boolean = false,
            var f1: Boolean = false,
            var f2: Boolean = false,
            var f3: Boolean = false,
            var f4: Boolean = false,
            var f5: Boolean = false,
            var f6: Boolean = false,
            var f7: Boolean = false,
            var speedSteps: Int = 126) {

    fun update(data: Map<String,Any?>) {
        val oldState = copy()
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
        if (this != oldState) {
            println("$name: $this")
        }
    }
}
