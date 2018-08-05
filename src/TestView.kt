import java.awt.*
import java.awt.geom.GeneralPath

class TestView(rect: Rectangle): Push2View(rect) {

    class P(val x: Double, val y: Double) {
        fun branch(direction: String, slope: Double, targetY: Double) : P {
            return when (direction) {
                "NW" -> P(x - slope * (y - targetY) / 2.0, targetY)
                "NE" -> P(x + slope * (y - targetY) / 2.0, targetY)
                "SW" -> P(x - slope * (targetY - y) / 2.0, targetY)
                "SE" -> P(x + slope * (targetY - y) / 2.0, targetY)
                else -> this
            }
        }
        fun leg(direction: String, slope: Double) : P {
            val len = 24.0
            val realSlope = slope / 2.0 // slope is same as in branch
            val shrink = 1.0 / Math.sqrt(1.0 + realSlope * realSlope)
            val deltaY = shrink * len
            val deltaX = realSlope * deltaY

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
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        return path
    }

    private fun strokePath(g: Graphics2D, path: GeneralPath) {
        g.paint = Color.BLACK
        g.stroke = BasicStroke(6.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3.0f)
        g.draw(path)
    }

    private fun fillPath(g: Graphics2D, color: Color?, path: GeneralPath) {
        g.paint = color ?: Color.WHITE
        g.stroke = BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3.0f)
        g.draw(path)
    }

    // 1:2  branch length stretching

    val ww = rect.width.toDouble()
    val hh = rect.height.toDouble()
    val s0 = 3.0
    val s1 = 6.0

    // obendruff

    val y = arrayOf(20.0, 50.0, 95.0, 140.0)
    val x = arrayOf(80.0, 160.0, 280.0, 540.0, 780.0, 900.0)
    val d0 = 10.0
    val d1 = 160.0
    val d2 = 280.0

    val a = P(x[0], y[0])
    val b = a.branch("SW", s0, y[0] + d0 * 3.0)
    val c = P(2 * d0, b.y)
    val d = P(d0, c.y - d0)
    val e = P(d0, a.y + d0)
    val f = P(2 * d0, a.y)
    val g = P(ww - d0, y[0])

    val h = P(x[1], y[0])
    val i = h.branch("SE", s0, y[1])
    val j = P(ww - d0, y[1])

    val k = P(x[2], y[1])
    val l = k.branch("SE", s0, y[2])
    val m = P(x[3], y[2])
    val n = P(m.x + d1, m.y)

    val o = m.branch("NE", s1, y[1])
    val p = m.branch("SW", s1, y[3])
    val q = P(p.x - d2, p.y)
    val r = P(p.x + d1, p.y)

    val y01mid = (y[0] + y[1]) / 2
    val s = P(x[4], y01mid).branch("NW", s0, y[0])
    val t = P(x[4], y01mid).branch("SE", s0, y[1])

    val u = P(x[5], y01mid).branch("SW", s0, y[1])
    val v = P(x[5], y01mid).branch("NE", s0, y[0])

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

        val yel = display.push2Colors[7]
        val blu = display.push2Colors[125]

        fillPath(g2, yel, makePath(arrayOf(g, v.leg("E", s0))))
        fillPath(g2, yel, makePath(arrayOf(v.leg("E",  s0), v, v.leg("SW", s0))))
        fillPath(g2, yel, makePath(arrayOf(v.leg("SW", s0),    u.leg("NE", s0))))
        fillPath(g2, yel, makePath(arrayOf(u.leg("NE", s0), u, u.leg("W", s0))))
        fillPath(g2, yel, makePath(arrayOf(u.leg("W",  s0),    t.leg("E", s0))))
        fillPath(g2, yel, makePath(arrayOf(t.leg("E",  s0), t, t.leg("NW", s0))))
        fillPath(g2, yel, makePath(arrayOf(t.leg("NW", s0),    s.leg("SE", s0))))
        fillPath(g2, yel, makePath(arrayOf(s.leg("SE", s0), s, s.leg("W", s0))))
        fillPath(g2, yel, makePath(arrayOf(s.leg("W",  s0),    h.leg("E", s0))))
        fillPath(g2, yel, makePath(arrayOf(h.leg("E",  s0), h, h.leg("W", s0))))
        fillPath(g2, yel, makePath(arrayOf(h.leg("W",  s0),    a.leg("E", s0))))
        fillPath(g2, yel, makePath(arrayOf(a.leg("E",  s0), a, a.leg("SW", s0))))
        fillPath(g2, yel, makePath(arrayOf(a.leg("SW", s0), b, c, d, e, f, a.leg("W", s0))))

        fillPath(g2, yel, makePath(arrayOf(t.leg("W",  s0), o, m, l, k.leg("SE", s0))))
        fillPath(g2, yel, makePath(arrayOf(o.leg("W",  s1), i, h.leg("SE", s0))))
        fillPath(g2, yel, makePath(arrayOf(r,                  q)))
        fillPath(g2, blu, makePath(arrayOf(m.leg("SW", s1),    p.leg("NE", s1))))
        fillPath(g2, blu, makePath(arrayOf(m.leg("E",  s1),    n)))
        fillPath(g2, blu, makePath(arrayOf(v.leg("W",  s0),    s.leg("E", s0))))
        fillPath(g2, blu, makePath(arrayOf(u.leg("E",  s0),    j)))
    }
}
