enum class JsmnType {
    UNDEFINED,
    OBJECT,
    ARRAY,
    STRING,
    PRIMITIVE // number, boolean (true/false) or null
}

data class JsmnTok(var type: JsmnType, var start: Int, var end: Int, var size: Int)

const val JSMN_ERROR_NOMEM = -1 // Not enough tokens were provided
const val JSMN_ERROR_INVAL = -2 // Invalid character inside JSON string
const val JSMN_ERROR_PART  = -3 // The string is not a full JSON packet, more bytes expected

class JsmnParser(private val num_tokens: Int) {
    // JSON parser. Contains an array of token blocks available. Also stores
    // the string being parsed now and current position in that string
    private var pos = 0   // offset in the JSON string
    private var toknext = 0   // next token to allocate
    private var toksuper = -1  // superior token node, e.g parent object or array

    val tokens = Array(num_tokens) { JsmnTok(JsmnType.UNDEFINED, -1, -1, -1) }

	private fun allocToken() : JsmnTok? {
		if (toknext >= num_tokens) {
			return null
		}
		val token = tokens[toknext++]
		token.start = -1
		token.end = -1
		token.size = 0
		return token
	}

    private fun JsmnTok.fill(type: JsmnType, start: Int, end: Int) {
        this.type = type
        this.start = start
        this.end = end
        this.size = 0
    }

	private fun parsePrimitive(js: String) : Int {
        // returns error or 0
        val start = pos
        while (pos < js.length) {
            when (js[pos]) {
                '\t', '\r', '\n', ' ', ',', ']', '}' -> {
                    if (tokens.isEmpty()) {
                        pos--
                        return 0
                    }
                    val token = allocToken()
                    return if (token == null) {
                        pos = start
                        JSMN_ERROR_NOMEM
                    } else {
                        token.fill(JsmnType.PRIMITIVE, start, pos)
                        pos--
                        0
                    }
                }
            }
            if (js[pos] < 32.toChar() || js[pos] >= 127.toChar()) {
                pos = start
                return JSMN_ERROR_INVAL
            }
            pos++
        }
        // In strict mode primitive must be followed by a comma/object/array
        pos = start
        return JSMN_ERROR_PART
	}

    private fun parseString(js: String) : Int {
        // returns error or 0
        val start = pos++

        // Skip starting quote
        while (pos < js.length) {
            val c = js[pos]
            if (c == '\"') { // Quote: end of string
                if (tokens.isEmpty()) {
                    return 0
                }
                val token = allocToken()
                return if (token == null) {
                    pos = start
                    JSMN_ERROR_NOMEM
                } else {
                    token.fill(JsmnType.STRING, start + 1, pos)
                    0
                }
            }
            if (c == '\\' && pos + 1 < js.length) { // Backslash: Quoted symbol expected
                pos++
                when (js[pos]) {
                    '\"', '/', '\\', 'b', 'f', 'r', 'n', 't' -> {
                        // Allowed escaped symbols
                    }
                    'u' -> {
                        // Allows escaped symbol \uXXXX
                        pos++
                        for (i in 1..minOf(4, js.length - pos)) {
                            val n = js[pos].toInt()
                            // If it isn't a hex character (0-9, A-F, a-f) we have an error
                            if(n !in 48..57 && n !in 65..70 && n !in 97..102) {
                                pos = start
                                return JSMN_ERROR_INVAL
                            }
                            pos++
                        }
                        pos--
                    }
                    else -> { // Unexpected symbol
                        pos = start
                        return JSMN_ERROR_INVAL
                    }
                }
            }
            pos++
        }
        pos = start
        return JSMN_ERROR_PART;
    }

    // Run JSON parser. It parses a JSON data string into an array of tokens,
    // each describing a single JSON object.
    fun parse(js: String): Int {
        pos = 0
        toknext = 0
        toksuper = -1

        var count = toknext // result to be returned

        while (pos < js.length) {
            when (js[pos]) {
                '{', '[' -> {
					count++
                    if (tokens.isNotEmpty()) {
                        val token = allocToken() ?: return JSMN_ERROR_NOMEM
                        if (toksuper != -1) {
                            tokens[toksuper].size++
                        }
                        token.type = if (js[pos] == '{') JsmnType.OBJECT else JsmnType.ARRAY
                        token.start = pos
                        toksuper = toknext - 1
                    }
				}
				'}', ']' -> {
                    if (tokens.isNotEmpty()) {
                        val type = if (js[pos] == '}') JsmnType.OBJECT else JsmnType.ARRAY
                        for (i in toknext - 1 downTo -1) {
                            if (i == -1) return JSMN_ERROR_INVAL // no match found
                            val token = tokens[i]
                            if (token.start != -1 && token.end == -1) {
                                if (token.type != type) {
                                    return JSMN_ERROR_INVAL
                                }
                                toksuper = -1
                                token.end = pos + 1
                                for (j in i downTo 0) {
                                    val token2 = tokens[j]
                                    if (token2.start != -1 && token2.end == -1) {
                                        toksuper = j
                                        break
                                    }
                                }
                                break
                            }
                        }
                    }
				}
                '\"' -> {
                    val err = parseString(js)
                    if (err < 0) {
						return err
					}
                    count++
                    if (toksuper != -1 && tokens.isNotEmpty()) {
						tokens[toksuper].size++
					}
                }
                '\t', '\r', '\n', ' ' -> {
                }
                ':' -> {
                    toksuper = toknext - 1
                }
                ',' -> {
                    if (tokens.isNotEmpty() && toksuper != -1 &&
                        tokens[toksuper].type != JsmnType.ARRAY &&
                        tokens[toksuper].type != JsmnType.OBJECT) {
						for (i in toknext - 1 downTo 0) {
                            if (tokens[i].type == JsmnType.ARRAY ||
								tokens[i].type == JsmnType.OBJECT) {
                                if (tokens[i].start != -1 && tokens[i].end == -1) {
                                    toksuper = i
                                    break
                                }
                            }
                        }
                    }
                }
                '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 't', 'f', 'n' -> {
                    // In strict mode primitives are: numbers and booleans
                    // And they must not be keys of the object
                    if (tokens.isNotEmpty() && toksuper != -1) {
                        val token = tokens[toksuper]
                        if (token.type == JsmnType.OBJECT ||
                           (token.type == JsmnType.STRING && token.size != 0)) {
                            return JSMN_ERROR_INVAL
                        }
                    }
                    val err = parsePrimitive(js)
                    if (err < 0) {
						return err
					}
                    count++
                    if (toksuper != -1 && tokens.isNotEmpty()) {
						tokens[toksuper].size++
					}
                }
                else -> {
                    return JSMN_ERROR_INVAL // Unexpected char in strict mode
                }
            }
            pos++
        }

        if (tokens.isNotEmpty()) {
            for (i in toknext - 1 downTo 0) {
                if (tokens[i].start != -1 && tokens[i].end == -1) {
                    return JSMN_ERROR_PART // Unmatched opened object or array
                }
            }
        }

        return count
    }
}

fun testJsmn() {
    val parserZ = JsmnParser(0)
    testCase(parserZ, """{"type":"throttle","data":{"throttle":"L0","speed":-1.0}}""")
    testCase(parserZ, """{"a":"b","c":[ 1, 2, 3 ],"d": null}""")
    testCase(parserZ, """{"a"}""")
    testCase(parserZ, """{"a", "b", "c" : false}""")

    val parser = JsmnParser(10)
    testCase(parser, """{"type":"throttle","data":{"throttle":"L0","speed":-1.0}}""")
    testCase(parser, """{"a":"b","c":[ 1, 2, 3 ],"d": null}""")
    testCase(parser, """{"a"}""")
    testCase(parser, """{"a", "b", "c" : false}""")
}

fun testCase(parser: JsmnParser, js: String) {
    println(js)
    val n = parser.parse(js)
    println("tokens returned: $n")
    if (parser.tokens.isNotEmpty()) {
        for (i in 0 until n) {
            val token = parser.tokens[i]
            println("${token} '${js.substring(token.start, token.end)}'")
        }
    }
    println("-----------------------------------------------------------------------------")
}

/******************************************************************************************
Test results:

{"type":"throttle","data":{"throttle":"L0","speed":-1.0}}
tokens returned: 9
-----------------------------------------------------------------------------
{"a":"b","c":[ 1, 2, 3 ],"d": null}
tokens returned: 10
-----------------------------------------------------------------------------
{"a"}
tokens returned: 2
-----------------------------------------------------------------------------
{"a", "b", "c" : false}
tokens returned: 5
-----------------------------------------------------------------------------
{"type":"throttle","data":{"throttle":"L0","speed":-1.0}}
tokens returned: 9
JsmnTok(type=OBJECT, start=0, end=57, size=2) '{"type":"throttle","data":{"throttle":"L0","speed":-1.0}}'
JsmnTok(type=STRING, start=2, end=6, size=1) 'type'
JsmnTok(type=STRING, start=9, end=17, size=0) 'throttle'
JsmnTok(type=STRING, start=20, end=24, size=1) 'data'
JsmnTok(type=OBJECT, start=26, end=56, size=2) '{"throttle":"L0","speed":-1.0}'
JsmnTok(type=STRING, start=28, end=36, size=1) 'throttle'
JsmnTok(type=STRING, start=39, end=41, size=0) 'L0'
JsmnTok(type=STRING, start=44, end=49, size=1) 'speed'
JsmnTok(type=PRIMITIVE, start=51, end=55, size=0) '-1.0'
-----------------------------------------------------------------------------
{"a":"b","c":[ 1, 2, 3 ],"d": null}
tokens returned: 10
JsmnTok(type=OBJECT, start=0, end=35, size=3) '{"a":"b","c":[ 1, 2, 3 ],"d": null}'
JsmnTok(type=STRING, start=2, end=3, size=1) 'a'
JsmnTok(type=STRING, start=6, end=7, size=0) 'b'
JsmnTok(type=STRING, start=10, end=11, size=1) 'c'
JsmnTok(type=ARRAY, start=13, end=24, size=3) '[ 1, 2, 3 ]'
JsmnTok(type=PRIMITIVE, start=15, end=16, size=0) '1'
JsmnTok(type=PRIMITIVE, start=18, end=19, size=0) '2'
JsmnTok(type=PRIMITIVE, start=21, end=22, size=0) '3'
JsmnTok(type=STRING, start=26, end=27, size=1) 'd'
JsmnTok(type=PRIMITIVE, start=30, end=34, size=0) 'null'
-----------------------------------------------------------------------------
{"a"}
tokens returned: 2
JsmnTok(type=OBJECT, start=0, end=5, size=1) '{"a"}'
JsmnTok(type=STRING, start=2, end=3, size=0) 'a'
-----------------------------------------------------------------------------
{"a", "b", "c" : false}
tokens returned: 5
JsmnTok(type=OBJECT, start=0, end=23, size=3) '{"a", "b", "c" : false}'
JsmnTok(type=STRING, start=2, end=3, size=0) 'a'
JsmnTok(type=STRING, start=7, end=8, size=0) 'b'
JsmnTok(type=STRING, start=12, end=13, size=1) 'c'
JsmnTok(type=PRIMITIVE, start=17, end=22, size=0) 'false'
-----------------------------------------------------------------------------
*/
