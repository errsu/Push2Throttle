package push2throttle

import java.util.*
import kotlin.math.min

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

class JsmnParser(private val numTokens: Int) {
    // JSON parser. Contains an array of token blocks available. Also stores
    // the string being parsed now and current position in that string
    private var pos = 0        // offset in the JSON string
    private var toknext = 0    // next token to allocate
    private var toksuper = -1  // superior token node, e.g parent object or array

    val tokens = Array(numTokens) { JsmnTok(JsmnType.UNDEFINED, -1, -1, -1) }

	private fun allocToken() : JsmnTok? {
		if (toknext >= numTokens) {
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

    private fun tokenToPrimitive(js: String, tok: Int) : Pair<Any?, Int> {
        val primitiveToken = tokens[tok]
        if (primitiveToken.type == JsmnType.PRIMITIVE)
        {
            val primitive = js.substring(primitiveToken.start, primitiveToken.end)
            return Pair(when {
                            primitive == "true"  -> true
                            primitive == "false" -> false
                            primitive == "null"  -> null
                            '.' in primitive     -> primitive.toFloat()
                            else                 -> primitive.toInt()
                        }, tok + 1)
        } else {
            throw Error("primitive expected at ${primitiveToken.start}")
        }
    }

    private fun tokenToString(js : String, tok: Int) : Pair<String, Int> {
        val stringToken = tokens[tok]
        if (stringToken.type == JsmnType.STRING) {
            return Pair(js.substring(stringToken.start, stringToken.end), tok + 1)
        } else {
            throw Error("string expected at ${stringToken.start}")
        }
    }

    private fun tokenToMap(js : String, tok: Int) : Pair<Map<String, Any?>, Int> {
        val objectToken = tokens[tok]
        if (objectToken.type == JsmnType.OBJECT) {
            val result : MutableMap<String, Any?> = HashMap()
            var nextTok = tok + 1
            repeat(objectToken.size) {
                val keyToken = tokens[nextTok]
                val (key, tokAfterKey) = tokenToString(js, nextTok)
                nextTok = tokAfterKey
                if (keyToken.size == 1) {
                    val (value, tokAfterValue) = tokenToAny(js, nextTok)
                    nextTok = tokAfterValue
                    result[key] = value
                } else {
                    result[key] = null
                }
            }
            return Pair(result, nextTok)
        } else {
            throw Error("object expected at ${objectToken.start}")
        }
    }

    private fun tokenToList(js : String, tok: Int) : Pair<List<Any?>, Int> {
        val arrayToken = tokens[tok]
        if (arrayToken.type == JsmnType.ARRAY) {
            val result : Array<Any?> = Array(arrayToken.size) { null }
            var nextTok = tok + 1
            repeat(arrayToken.size) {
                val (value, tokAfterValue) = tokenToAny(js, nextTok)
                nextTok = tokAfterValue
                result[it] = value
            }
            return Pair(result.toList(), nextTok)
        } else {
            throw Error("array expected at ${arrayToken.start}")
        }
    }

    private fun tokenToAny(js : String, tok: Int) : Pair<Any?, Int> {
        val token = tokens[tok]
        return when (token.type) {
            JsmnType.STRING    -> tokenToString(js, tok)
            JsmnType.PRIMITIVE -> tokenToPrimitive(js, tok)
            JsmnType.OBJECT    -> tokenToMap(js, tok)
            JsmnType.ARRAY     -> tokenToList(js, tok)
            else -> throw Error("unexpected token type")
        }
    }

    fun parseToTree(js: String) : Any? {
        if (tokens.isEmpty()) return null
        val tokCount = parse(js)
        if (tokCount <= 0) return null
        return try {
            val (result, tokAfterAny) = tokenToAny(js, 0)
            if (tokAfterAny != tokCount) {
                throw Error("not all tokens used")
            } else {
                result
            }
        } catch (e: Error) {
            println("Jsmn::parseToTree Error: ${e.message}")
            null
        }
    }

    fun printError(js: String, n: Int) {
        println(when (n) {
            0 -> "no content found"
            JSMN_ERROR_NOMEM -> "Not enough tokens were provided"
            JSMN_ERROR_INVAL -> "Invalid character inside JSON string: ${js.substring(pos, min(20, js.length - pos))}"
            JSMN_ERROR_PART -> "The string is not a full JSON packet, more bytes expected"
            else -> "unexpected JSON error: $n"
        })
    }
}

class JsmnFormatter {
    private fun formatMap(map: Map<*,*>) : String {
        val sb = StringBuilder()
        sb.append("{")
        var first = true
        for ((key, value) in map) {
            if (first) {
                first = false
            } else {
                sb.append(", ")
            }
            if (value == null) {
                sb.append(""""$key"""")
            } else {
                sb.append(""""$key" : ${formatTree(value)}""")
            }
        }
        sb.append("}")
        return sb.toString()
    }

    private fun formatList(list: List<*>) : String {
        val sb = StringBuilder()
        sb.append("[")
        var first = true
        for (value in list) {
            if (first) {
                first = false
            } else {
                sb.append(", ")
            }
            sb.append(formatTree(value))
        }
        sb.append("]")
        return sb.toString()
    }

    fun formatTree(tree: Any?) : String {
        return when(tree) {
            null -> "null"
            is Boolean -> if (tree) "true" else "false"
            is Float, is Int -> tree.toString()
            is String -> """"$tree""""
            is Map<*,*> -> formatMap(tree)
            is List<*> -> formatList(tree)
            else -> """""""
        }
    }
}

fun testJsmn() {
    testCase("""{"type":"throttle","data":{"throttle":"L0","speed":-1.0}}""")
    testCase("""{"a":"b","c":[ 1, 2, 3 ],"d": null}""")
    testCase("""{"a"}""")
    testCase("""{"a", "b", "c" : false}""")
    testCase("""[1, 2, 3]""")
    testCase("""{1, [1, 2]}""")
    testCase("""{1 : "bad key"}""")
    testCase("""{[] : "another bad key"}""")
    testCase("""{{} : "another bad key"}""")
    testCase("""{true: "final bad key"}""")
    testCase("""{"bad value" : 2 + 3}""")
}

fun testCase(js: String) {
    println(js)
    val parserZ = JsmnParser(0)
    val n = parserZ.parse(js)
    println("tokens detected: $n")
    if (n > 0) {
        val parser = JsmnParser(n)
        val n1 = parser.parse(js)
        if (n1 != n) {
            println("got $n1 tokens when reparsing")
        }
        if (n1 > 0) {
            for (i in 0 until n1) {
                val token = parser.tokens[i]
                println("[$i] ${token} '${js.substring(token.start, token.end)}'")
            }
            val tree = parser.parseToTree(js)
            println("Tree: $tree")
            val formatter = JsmnFormatter()
            println(formatter.formatTree(tree))
        }
    }
    println("-----------------------------------------------------------------------------")
}

/******************************************************************************************
Test results:

{"type":"throttle","data":{"throttle":"L0","speed":-1.0}}
tokens detected: 9
[0] JsmnTok(type=OBJECT, start=0, end=57, size=2) '{"type":"throttle","data":{"throttle":"L0","speed":-1.0}}'
[1] JsmnTok(type=STRING, start=2, end=6, size=1) 'type'
[2] JsmnTok(type=STRING, start=9, end=17, size=0) 'throttle'
[3] JsmnTok(type=STRING, start=20, end=24, size=1) 'data'
[4] JsmnTok(type=OBJECT, start=26, end=56, size=2) '{"throttle":"L0","speed":-1.0}'
[5] JsmnTok(type=STRING, start=28, end=36, size=1) 'throttle'
[6] JsmnTok(type=STRING, start=39, end=41, size=0) 'L0'
[7] JsmnTok(type=STRING, start=44, end=49, size=1) 'speed'
[8] JsmnTok(type=PRIMITIVE, start=51, end=55, size=0) '-1.0'
Tree: {data={throttle=L0, speed=-1.0}, type=throttle}
-----------------------------------------------------------------------------
{"a":"b","c":[ 1, 2, 3 ],"d": null}
tokens detected: 10
[0] JsmnTok(type=OBJECT, start=0, end=35, size=3) '{"a":"b","c":[ 1, 2, 3 ],"d": null}'
[1] JsmnTok(type=STRING, start=2, end=3, size=1) 'a'
[2] JsmnTok(type=STRING, start=6, end=7, size=0) 'b'
[3] JsmnTok(type=STRING, start=10, end=11, size=1) 'c'
[4] JsmnTok(type=ARRAY, start=13, end=24, size=3) '[ 1, 2, 3 ]'
[5] JsmnTok(type=PRIMITIVE, start=15, end=16, size=0) '1'
[6] JsmnTok(type=PRIMITIVE, start=18, end=19, size=0) '2'
[7] JsmnTok(type=PRIMITIVE, start=21, end=22, size=0) '3'
[8] JsmnTok(type=STRING, start=26, end=27, size=1) 'd'
[9] JsmnTok(type=PRIMITIVE, start=30, end=34, size=0) 'null'
Tree: {a=b, c=[1, 2, 3], d=null}
-----------------------------------------------------------------------------
{"a"}
tokens detected: 2
[0] JsmnTok(type=OBJECT, start=0, end=5, size=1) '{"a"}'
[1] JsmnTok(type=STRING, start=2, end=3, size=0) 'a'
Tree: {a=null}
-----------------------------------------------------------------------------
{"a", "b", "c" : false}
tokens detected: 5
[0] JsmnTok(type=OBJECT, start=0, end=23, size=3) '{"a", "b", "c" : false}'
[1] JsmnTok(type=STRING, start=2, end=3, size=0) 'a'
[2] JsmnTok(type=STRING, start=7, end=8, size=0) 'b'
[3] JsmnTok(type=STRING, start=12, end=13, size=1) 'c'
[4] JsmnTok(type=PRIMITIVE, start=17, end=22, size=0) 'false'
Tree: {a=null, b=null, c=false}
-----------------------------------------------------------------------------
[1, 2, 3]
tokens detected: 4
[0] JsmnTok(type=ARRAY, start=0, end=9, size=3) '[1, 2, 3]'
[1] JsmnTok(type=PRIMITIVE, start=1, end=2, size=0) '1'
[2] JsmnTok(type=PRIMITIVE, start=4, end=5, size=0) '2'
[3] JsmnTok(type=PRIMITIVE, start=7, end=8, size=0) '3'
Tree: [1, 2, 3]
-----------------------------------------------------------------------------
{1, [1, 2]}
tokens detected: 5
got -2 tokens when reparsing
-----------------------------------------------------------------------------
{1 : "bad key"}
tokens detected: 3
got -2 tokens when reparsing
-----------------------------------------------------------------------------
{[] : "another bad key"}
tokens detected: 3
[0] JsmnTok(type=OBJECT, start=0, end=24, size=1) '{[] : "another bad key"}'
[1] JsmnTok(type=ARRAY, start=1, end=3, size=1) '[]'
[2] JsmnTok(type=STRING, start=7, end=22, size=0) 'another bad key'
Jsmn::parseToTree Error: string expected at 1
Tree: null
-----------------------------------------------------------------------------
{{} : "another bad key"}
tokens detected: 3
[0] JsmnTok(type=OBJECT, start=0, end=24, size=1) '{{} : "another bad key"}'
[1] JsmnTok(type=OBJECT, start=1, end=3, size=1) '{}'
[2] JsmnTok(type=STRING, start=7, end=22, size=0) 'another bad key'
Jsmn::parseToTree Error: string expected at 1
Tree: null
-----------------------------------------------------------------------------
{true: "final bad key"}
tokens detected: 3
got -2 tokens when reparsing
-----------------------------------------------------------------------------
{"bad value" : 2 + 3}
tokens detected: -2
-----------------------------------------------------------------------------
*/
