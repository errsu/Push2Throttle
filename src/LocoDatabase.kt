import java.io.File

enum class FuncType{ L, S, O, LS, LO, SO, LSO }
enum class FuncBehavior{ M, T, I }
data class LocoFunc(
        val function: String,
        val type: FuncType,
        val name: String,
        val stdName: String,
        val behavior: FuncBehavior,
        val rank: Int?)
data class LocoInfo(val model: String, val speed: Int, val functions: Map<String, LocoFunc>)

class LocoDatabase {

    private val folderPath = "data/loco/"
    private val fileExtension = ".loco.txt"
    private val speedRegex = Regex("""\s*SPEED\s+([0-9]+)\s*""")
    private val funcRegex = Regex(
            """\s*(F\d+)\s+(L|S|O|LS|LO|SO|LSO)\s+([1-8-])\s+([MTI])\s+"(.*)"\s*([\p{IsAlphabetic}\p{Digit}]*)\s*""")

    private val data = HashMap<String, LocoInfo>()

    private val lengthThenNatural = compareBy<String>{ it.length }.then(naturalOrder())

    init {
        scanFolder()
        data.keys.forEach { key ->
            println(key)
            println("    model: ${data[key]?.model}")
            println("    speed: ${data[key]?.speed}")
            println("    functions:")
            data[key]?.functions?.toSortedMap(lengthThenNatural)?.forEach { fname, f ->
                println("        $fname: '${f.function}' ${f.type} '${f.name}' '${f.stdName}' ${f.behavior} ${f.rank}")
            }
        }
    }

    private fun scanSpeedLine(line: String) : Int? {
        val matchResult = speedRegex.matchEntire(line)
        if (matchResult == null) {
            println("Bad loco.txt format: $line")
            return null
        }
        return matchResult.groupValues[1].toInt()
    }

    private fun scanFuncLine(line: String) : LocoFunc? {
        // F12 $@ - I "Kurve / Weichensensor" Kurvenquietschen
        val matchResult = funcRegex.matchEntire(line)
        if (matchResult == null) {
            println("Bad loco.txt format: $line")
            return null
        }
        val values = matchResult.groupValues
        val function = values[1]
        val funcType = FuncType.valueOf(values[2])
        val rank = values[3].toIntOrNull()
        val funcBehavior = FuncBehavior.valueOf(values[4])
        val name = values[5]
        val stdName = values[6]
        return LocoFunc(function, funcType, name, stdName, funcBehavior, rank)
    }

    private fun parseFile(file: File) {
        println(file.name)
        val model =  file.name.dropLast(fileExtension.length)
        var speed = 100
        val functions = HashMap<String, LocoFunc>()
        file.forEachLine { line ->
            if (line.startsWith("SPEED")) {
                val locoSpeed = scanSpeedLine(line)
                if (locoSpeed == null) {
                    println("error in ${file.name}")
                } else {
                    speed = locoSpeed
                }
            }
            if (line.startsWith("F")) {
                val locoFunction = scanFuncLine(line)
                if (locoFunction == null) {
                    println("error in ${file.name}")
                } else {
                    functions[locoFunction.function] = locoFunction
                }
            }
        }
        val info = LocoInfo(model, speed, functions)
        data[model] = info
    }

    fun scanFolder() {
        val folder = File(folderPath)
        if (folder.exists()) {
            folder.listFiles().forEach { file ->
                if (file.name.endsWith(fileExtension)) {
                    parseFile(file)
                }
            }
        }
    }
}
