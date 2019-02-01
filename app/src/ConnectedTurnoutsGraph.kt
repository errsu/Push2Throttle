package push2throttle

//------------------------------------------------------------------------------------
// graph representation of selected path

// We have an undirected graph where no vertex
// has more than two edges connected.
// Also, there are no cycles by construction.
// The main goal is to find the connected components,
// which are printed in one go with a single color.

class ConnectedTurnoutsGraph {
    class Vertex(val name: String, val n: Int) {
        override fun toString() : String {
            return "V$n($name){$adj1,$adj2,$visited}"
        }
        var adj1: Int = -1
        var adj2: Int = -1
        var visited = false
        fun valency() : Int {
            return if (adj1 == -1) 0 else 1 + if (adj2 == -1) 0 else 1
        }

        fun reset() {
            adj1 = -1
            adj2 = -1
            visited = false
        }
        fun addEdge(to: Int) {
            when {
                adj1 == -1 -> adj1 = to
                adj2 == -1 -> adj2 = to
                else -> println("ConnectedTurnoutsGraph Error: more than two edges for vertex $n")
            }
        }
    }

    val vertices = arrayListOf<Vertex>()

    // how to use:
    // Building time
    // - add all vertexes
    // - enumerate vertexes (?)
    // On turnout state changed
    // - reset edges
    // - add all active edges
    // - find components

    fun addVertex(name: String, n: Int) {
        vertices.add(Vertex(name, n))
    }

    fun resetEdges() {
        vertices.forEach { it.reset() }
    }

    fun addEdge(n0: Int, n1: Int) {
        vertices[n0].addEdge(n1)
        vertices[n1].addEdge(n0)
    }

    fun tracePathes(startIndex: Int, list: MutableList<Int>) {
        list.add(startIndex)
        vertices[startIndex].visited = true
        val next1 = vertices[startIndex].adj1
        if (next1 != -1 && !vertices[next1].visited) {
            tracePathes(next1, list)
        }
        val next2 = vertices[startIndex].adj2
        if (next2 != -1 && !vertices[next2].visited) {
            tracePathes(next2, list)
        }
    }

    fun findComponents() : List<List<Int>> {
        val components = mutableListOf<List<Int>>()

        while (true) {
            val list = mutableListOf<Int>()

            // find corner vertex (cycles are ignored)
            for (startIndex in vertices.indices) {
                if (!vertices[startIndex].visited && vertices[startIndex].valency() == 1) {
                    // recursively add next vertices starting with corner
                    tracePathes(startIndex, list)
                    break
                }
            }

            if (list.isNotEmpty()) {
                components.add(list)
            }
            else {
                return components
            }
        }
    }
}