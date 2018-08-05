import java.awt.*
import java.awt.geom.GeneralPath

class TestView(rect: Rectangle): Push2View(rect) {

    class P(val x: Int, val y: Int)

    private fun makePath(points: Array<P>) : GeneralPath {
        val path = GeneralPath(GeneralPath.WIND_EVEN_ODD, points.size)
        path.moveTo(points[0].x.toDouble(), points[0].y.toDouble())
        for (i in 1 until points.size) {
            path.lineTo(points[i].x.toDouble(), points[i].y.toDouble())
        }
        return path
    }

    private fun strokePath(g: Graphics2D, path: GeneralPath, filled: Boolean) {
        g.paint = Color.BLACK
        g.stroke = BasicStroke(6.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3.0f)
        g.draw(path)
        if (filled) {
            g.paint = Color.YELLOW
            g.stroke = BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3.0f)
            g.draw(path)
        }
    }

    // 1:2  slope length stretching

    private fun lx(l: Int) : Int {
        val sqr5 = 2.236
        return (l.toDouble() * 2.0 / sqr5).toInt()
    }

    private fun ly(l: Int) : Int {
        val sqr5 = 2.236
        return (l.toDouble() / sqr5).toInt()
    }

    @Suppress("UNUSED_PARAMETER")
    override fun draw(g: Graphics2D, frame: Int, display: Push2Display) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.paint = display.push2Colors[20]
        g.fillRect(0, 0, rect.width, rect.height)
        g.paint = Color.BLACK
        g.stroke = BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f)
        g.drawLine(0, 80, rect.width, 80)
        g.drawLine(240, 80, 240 + 60, 80 + 30)

        val pointsBlank = arrayOf(P(40, 10), P(80, 10), P(100, 20))
        strokePath(g, makePath(pointsBlank), false)

        val pointsFilled = arrayOf(P(140, 10), P(180, 10), P(200, 20))
        strokePath(g, makePath(pointsFilled), true)

        val pointsBlank2 = arrayOf(P(240, 10), P(280, 10), P(300, 20))
        strokePath(g, makePath(pointsBlank2), false)

        val sqr5 = 2.236
        val offs = 7
        val pointsFilled2 = arrayOf(P(240 + offs, 10), P(280, 10), P(300 - lx(offs), 20 - ly(offs)))
        strokePath(g, makePath(pointsFilled2), true)

    }
}
