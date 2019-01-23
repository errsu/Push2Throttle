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
data class LocoInfo(val mfgModel: String, val speed: Int, val functions: Map<String, LocoFunc>)

class LocoDatabase {

    private val folderPath = "data/loco/"
    private val locoFileExtension = ".loco.txt"
    private val functionMatrixFileName = "function_matrix.txt"
    private val speedRegex = Regex("""\s*SPEED\s+([0-9]+)\s*""")
    private val funcRegex = Regex(
            """(?U)\s*(F\d+)\s+(L|S|O|LS|LO|SO|LSO)\s+([1-8-])\s+([MTI])\s+"(.*)"\s*([\p{Alnum}]*)\s*""")
    private val matrixRegex = StringBuffer().run {
        append("""(?U)\s*([1-8])""")
        repeat(8) {append("""\s+([\p{Alnum}]*|-)""")}
        append("""\s*""")
        Regex(toString())
    }
    private val locoData = HashMap<String, LocoInfo>()
    private val functionMatrix = Array(8) { Array<String?>(8) {null}}
    private val functionRowCols = HashMap<String, Pair<Int, Int>>()
    private val lengthThenNatural = compareBy<String>{ it.length }.then(naturalOrder())

    init {
        scanFolder()
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

    private fun parseLocoFile(file: File) {
        val mfgModel = file.name.dropLast(locoFileExtension.length)
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
        val info = LocoInfo(mfgModel, speed, functions)
        locoData[mfgModel] = info
    }

    private fun parseFunctionMatrixFile(file: File) {
        file.forEachLine { line ->
            if (!line.startsWith("#") && line.isNotBlank()) {
                val matchResult = matrixRegex.matchEntire(line)
                if (matchResult == null) {
                    println("Bad function matrix file format: $line")
                } else {
                    val values = matchResult.groupValues
                    val row = values[1].toInt() - 1
                    repeat(8) { col ->
                        val stdName = values[2 + col]
                        if (stdName == "-") {
                            functionMatrix[row][col] = null
                        } else {
                            functionMatrix[row][col] = stdName
                            functionRowCols[stdName] = Pair(row, col)
                        }
                    }
                }
            }
        }
    }

    fun scanFolder() {
        val folder = File(folderPath)
        if (folder.exists()) {
            folder.listFiles().forEach { file ->
                if (file.name == functionMatrixFileName) {
                    parseFunctionMatrixFile(file)
                } else if (file.name.endsWith(locoFileExtension)) {
                    parseLocoFile(file)
                }
            }
        }
    }

    fun getInfoForMfgModel(mfg: String, model: String): LocoInfo? {
        val mfgModel = mfg.toLowerCase().replace(' ', '_') + "_" + model.toLowerCase().replace(' ', '_')
        return locoData[mfgModel]
    }

    fun getRowColForFunction(stdName: String): Pair<Int, Int>? {
        return functionRowCols[stdName]
    }

    fun print() {
        functionMatrix.forEach {
            println("${it.asList()}")
        }
        locoData.keys.forEach { key ->
            println(key)
            println("    mfg_model: ${locoData[key]?.mfgModel}")
            println("    speed: ${locoData[key]?.speed}")
            println("    functions:")
            locoData[key]?.functions?.toSortedMap(lengthThenNatural)?.forEach { fname, f ->
                println("        $fname: '${f.function}' ${f.type} '${f.name}' '${f.stdName}' ${f.behavior} ${f.rank}")
            }
        }
    }
}
