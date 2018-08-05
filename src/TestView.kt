import java.awt.*
import java.awt.geom.GeneralPath
import kotlin.math.pow

class TestView(rect: Rectangle): Push2View(rect) {

    class P(val x: Int, val y: Int) {
        fun branch(direction: String, slope: Int, targetY: Int) : P {
            return when (direction) {
                "NW" -> P(x - slope * (y - targetY) / 2, targetY)
                "NE" -> P(x + slope * (y - targetY) / 2, targetY)
                "SW" -> P(x - slope * (targetY - y) / 2, targetY)
                "SE" -> P(x + slope * (targetY - y) / 2, targetY)
                else -> this
            }
        }
        fun leg(direction: String, slope: Int) : P {
            val len = 30
            val realSlope = slope.toDouble() / 2.0 // slope is same as in branch
            val shrink = 1.0 / Math.sqrt(1.0 + realSlope * realSlope)
            val deltaY = (shrink * len.toDouble()).toInt()
            val deltaX = (realSlope * shrink * len.toDouble()).toInt()

            return when (direction) {
                "E"  -> P(x + len, y)
                "W"  -> P(x - len, y)
                "NW" -> P(x - deltaX, y - deltaY)
                "NE" -> P(x + deltaX, y - deltaY)
                "SW" -> P(x - deltaX, y + deltaY)
                "SE" -> P(x + deltaX, y + deltaY)
                else -> this
            }
        }
    }

    private fun makePath(points: Array<P>) : GeneralPath {
        val path = GeneralPath(GeneralPath.WIND_EVEN_ODD, points.size)
        path.moveTo(points[0].x.toDouble(), points[0].y.toDouble())
        for (i in 1 until points.size) {
            path.lineTo(points[i].x.toDouble(), points[i].y.toDouble())
        }
        return path
    }

    private fun strokePath(g: Graphics2D, path: GeneralPath) {
        g.paint = Color.BLACK
        g.stroke = BasicStroke(6.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3.0f)
        g.draw(path)
    }

    private fun fillPath(g: Graphics2D, path: GeneralPath, color: Color?) {
        g.paint = color ?: Color.WHITE
        g.stroke = BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3.0f)
        g.draw(path)
    }

    // 1:2  branch length stretching

    private fun lx(l: Int) : Int {
        val sqr5 = 2.236
        return (l.toDouble() * 2.0 / sqr5).toInt()
    }

    private fun ly(l: Int) : Int {
        val sqr5 = 2.236
        return (l.toDouble() / sqr5).toInt()
    }

    val ww = rect.width
    val hh = rect.height

    // obendruff

    val y = arrayOf(20, 50, 95, 140)
    val x = arrayOf(80, 160, 280, 540, 780, 900)
    val d0 = 10
    val d1 = 160
    val d2 = 280

    val a = P(x[0], y[0])
    val b = a.branch("SW", 3, y[0] + 3 * d0)
    val c = P(2 * d0, b.y)
    val d = P(d0, c.y - d0)
    val e = P(d0, a.y + d0)
    val f = P(2 * d0, a.y)
    val g = P(ww - d0, y[0])

    val h = P(x[1], y[0])
    val i = h.branch("SE", 3, y[1])
    val j = P(ww - d0, y[1])

    val k = P(x[2], y[1])
    val l = k.branch("SE", 3, y[2])
    val m = P(x[3], y[2])
    val n = P(m.x + d1, m.y)

    val o = m.branch("NE", 6, y[1])
    val p = m.branch("SW", 6, y[3])
    val q = P(p.x - d2, p.y)
    val r = P(p.x + d1, p.y)

    val y01mid = (y[0] + y[1]) / 2
    val s = P(x[4], y01mid).branch("NW", 3, y[0])
    val t = P(x[4], y01mid).branch("SE", 3, y[1])

    val u = P(x[5], y01mid).branch("SW", 3, y[1])
    val v = P(x[5], y01mid).branch("NE", 3, y[0])

    val lines = arrayOf(
            arrayOf(a, b, c, d, e, f, g),
            arrayOf(h, i, j),
            arrayOf(k, l, n),
            arrayOf(o, p),
            arrayOf(q, r),
            arrayOf(s, t),
            arrayOf(u, v))

    @Suppress("UNUSED_PARAMETER")
    override fun draw(g2: Graphics2D, frame: Int, display: Push2Display) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.paint = display.push2Colors[20]
        g2.fillRect(0, 0, rect.width, rect.height)

        for (l in lines) {
            strokePath(g2, makePath(l))
        }

        // val route = makePath(arrayOf(a.leg("SW", 3), a, h))
        val route1 = makePath(arrayOf(g, v, u, t, s, h, a, b, c, d, e, f, a.leg("W", 0)))
        fillPath(g2, route1, display.push2Colors[7])

        val route2 = makePath(arrayOf(t.leg("W", 0), o, m, l, k.leg("SE", 3)))
        fillPath(g2, route2, display.push2Colors[10])

        val route3 = makePath(arrayOf(o.leg("W", 6), i, h.leg("SE", 3)))
        fillPath(g2, route3, display.push2Colors[4])

        val route4 = makePath(arrayOf(r, q))
        fillPath(g2, route4, display.push2Colors[23])

        val route5 = makePath(arrayOf(m.leg("SW", 6), p.leg("NE", 6)))
        fillPath(g2, route5, display.push2Colors[16])

        val route6 = makePath(arrayOf(m.leg("E", 6), n))
        fillPath(g2, route6, display.push2Colors[125])

        val route7 = makePath(arrayOf(v.leg("W", 3), s.leg("E", 3)))
        fillPath(g2, route7, display.push2Colors[126])

        val route8 = makePath(arrayOf(u.leg("E", 3), j))
        fillPath(g2, route8, display.push2Colors[127])
    }
}
