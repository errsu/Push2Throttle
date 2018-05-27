class DataModel {
    val throttleCount = 3

    data class ThrottleState(
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
            var speedSteps: Int = 126
    )

    var throttleStates: Array<ThrottleState> = Array(throttleCount) {ThrottleState()}

    fun updateThrottle(num: Int, data: Map<String,Any?>) {
        val state = throttleStates[num]
        val oldState = state.copy()
        for ((key, value) in data) {
            when (key) {
                "address" -> if (value is Int) state.address = value
                "speed"   -> if (value is Float) state.speed = value
                "forward" -> if (value is Boolean) state.forward = value
                "F0"      -> if (value is Boolean) state.f0 = value
                "F1"      -> if (value is Boolean) state.f1 = value
                "F2"      -> if (value is Boolean) state.f2 = value
                "F3"      -> if (value is Boolean) state.f3 = value
                "F4"      -> if (value is Boolean) state.f4 = value
                "F5"      -> if (value is Boolean) state.f5 = value
                "F6"      -> if (value is Boolean) state.f6 = value
                "F7"      -> if (value is Boolean) state.f7 = value
                "speedSteps" -> if(value is Int) state.speedSteps = value
            }
        }
        if (state != oldState) {
            println("T$num: $state")
        }
    }
}
