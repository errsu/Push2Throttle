import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

data class TurnoutSymbol(val name: String, val x: Int, val y: Int, val hand: String, val face: String)

class PanelViewOld(rect: Rectangle): Push2View(rect) {

    private val panelImage = ImageIO.read(File("Panel.png"))

    private val turnoutImage = mapOf(
            "RightHand" to mapOf (
                    "LtoR" to mapOf(
                            "Straight" to ImageIO.read(File("WR-gerade.png")),
                            "Diverge"  to ImageIO.read(File("WR-abzweig.png"))),
                    "RtoL" to mapOf(
                            "Straight" to ImageIO.read(File("WR-gerade180.png")),
                            "Diverge"  to ImageIO.read(File("WR-abzweig180.png")))
            ),
            "LeftHand" to mapOf (
                    "LtoR" to mapOf(
                            "Straight" to ImageIO.read(File("WL-gerade180.png")),
                            "Diverge"  to ImageIO.read(File("WL-abzweig180.png"))),
                    "RtoL" to mapOf(
                            "Straight" to ImageIO.read(File("WL-gerade.png")),
                            "Diverge"  to ImageIO.read(File("WL-abzweig.png")))
            )
    )

    fun getTurnoutImage(hand: String, face: String, state: Boolean) : BufferedImage? {
        val handImages = turnoutImage[hand]
        val faceImages = handImages?.get(face)
        return faceImages?.get(if (state) "Diverge" else "Straight")
    }

    private val turnoutSymbol = mapOf(
             "W1" to TurnoutSymbol( "W1",  63,   2, "RightHand", "LtoR"),
             "W2" to TurnoutSymbol( "W2", 171,  16, "RightHand", "LtoR"),
             "W3" to TurnoutSymbol( "W3", 291,  29, "RightHand", "LtoR"),
             "W4" to TurnoutSymbol( "W4", 410,  42, "RightHand", "LtoR"),
             "W5" to TurnoutSymbol( "W5", 532,  56, "RightHand", "LtoR"),
             "W6" to TurnoutSymbol( "W6", 774,  16, "LeftHand",  "RtoL"),
             "W7" to TurnoutSymbol( "W7", 878,   2, "LeftHand",  "RtoL"),
             "W8" to TurnoutSymbol( "W8", 878, 107, "RightHand", "RtoL"),
             "W9" to TurnoutSymbol( "W9", 775, 109, "LeftHand",  "RtoL"),
            "W10" to TurnoutSymbol("W10", 291, 123, "RightHand", "LtoR"),
            "W11" to TurnoutSymbol("W11", 170, 109, "RightHand", "LtoR"),
            "W12" to TurnoutSymbol("W12",  64, 107, "LeftHand",  "LtoR")
    )

    private val turnoutState = mapOf(
             "W1" to true,
             "W2" to false,
             "W3" to false,
             "W4" to true,
             "W5" to false,
             "W6" to false,
             "W7" to false,
             "W8" to true,
             "W9" to false,
            "W10" to true,
            "W11" to true,
            "W12" to false)

    private val displayWidth  = 960
    private val displayHeight = 160

    @Suppress("UNUSED_PARAMETER")
    override fun draw(g: Graphics2D, frame: Int, display: Push2Display) {
//        g.paint = display.push2Colors[(frame / 60) % 27]
//        g.fillRect(0, 0, displayWidth, displayHeight)
        g.drawImage(panelImage, 0, 0, null)
        for (name in turnoutSymbol.keys) {
//            val state = turnoutState[name] ?: false
            val state = (((frame / 60) % 2) == 1)
            val symbol = turnoutSymbol[name]
            val image = getTurnoutImage(symbol!!.hand, symbol.face, state)
            g.drawImage(image, symbol.x, symbol.y, null)
        }
    }
}
