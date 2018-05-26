class ThrottleState {
    // Typical JMRI Responses
    // connect():
    // -> {"type":"hello","data":{"JMRI":"4.10+R419243e",
    //                            "json":"4.0",
    //                            "heartbeat":13500,
    //                            "railroad":"Meine JMRI Bahngesellschaft",
    // ping {"type":"ping"}
    // -> {"type":"pong"}
    //
    // register {"type":"throttle","data":{"throttle":"L0","address":0}}
    // -> {"type":"throttle","data":{"address":0,"speed":0.0,"forward":true,
    //             "F0":false,"F1":false,"F2":false,"F3":false,"F4":false,"F5":false,
    //             "F6":false,"F7":false,"F8":false,"F9":false,"F10":false,"F11":false,
    //             "F12":false,"F13":false,"F14":false,"F15":false,"F16":false,"F17":false,
    //             "F18":false,"F19":false,"F20":false,"F21":false,"F22":false,"F23":false,
    //             "F24":false,"F25":false,"F26":false,"F27":false,"F28":false,
    //             "speedSteps":126,"clients":1,"throttle":"L0"}}
    //
    // on throttle changes:
    // -> {"type":"throttle","data":{"speed":0.5714286,"throttle":"L0"}}
    // -> {"type":"throttle","data":{"speed":0.54761904,"throttle":"L0"}}
    // -> {"type":"throttle","data":{"F0":true,"throttle":"L0"}}   // light
    // -> {"type":"throttle","data":{"F0":false,"throttle":"L0"}}  // light again
    // -> {"type":"throttle","data":{"F1":true,"throttle":"L0"}}
    // -> {"type":"throttle","data":{"F7":true,"throttle":"L0"}}
    // -> {"type":"throttle","data":{"speed":0.6825397,"throttle":"L0"}}
    // -> {"type":"throttle","data":{"speed":0.0,"throttle":"L0"}} // idle
    // -> {"type":"throttle","data":{"speed":-1.0,"throttle":"L0"}} // Stop!
    // -> {"type":"throttle","data":{"forward":false,"throttle":"L0"}} // reverse
}
