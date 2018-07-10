import java.awt.Rectangle

class SceneManager(private val display: Push2Display,
                   private val elements: Push2Elements,
                   private val throttleManager: ThrottleManager) : MidiController {

    private val throttleScene = ThrottleScene(display, elements, throttleManager)
    private val panelScene = PanelScene(display)
    private var currentScene: Scene? = null
    private val select = elements.getElement("Select")

    init {
        elements.connect(select!!, this,
                mapOf("onColor" to 124, "offColor" to 6, "type" to "toggle"),
                false)
    }

    fun gotoScene(scene: String) {
        currentScene?.destroy()
        currentScene = when (scene) {
            "throttles" -> throttleScene
            "panel"     -> panelScene
            else        -> null
        }
        currentScene?.build()
        elements.updateElementStateByJmri(select!!, scene == "panel")
    }

    fun throttlesReassigned() {
        currentScene?.destroy()
        currentScene?.build()
    }

    override fun <T : Any> elementStateChanged(element: MidiElement, newValue: T) {
        when (element) {
            select -> gotoScene(if (newValue == true) "panel" else "throttles")
        }
    }
}

interface Scene {
    fun build()
    fun destroy()
}

class ThrottleScene(private val display: Push2Display,
                    private val elements: Push2Elements,
                    private val throttleManager: ThrottleManager) : Scene {

    // The controllers have a fixed connection to the throttles,
    // which continously record the loco data (if a loco is assigned).
    // The controllers also know their elements, but are not
    // connected to them until the scene is established.

    private val controllers = Array(24) {
        val throttle = throttleManager.throttleAtSlot(it)
        val track = (it % 8) + 1
        val controller = Push2ThrottleController(
                elements,
                throttle,
                elements.getElement("Pot T$track")    as Erp,
                elements.getElement("Disp A T$track") as ButtonRgb,
                elements.getElement("Disp B T$track") as ButtonRgb)
        throttle.controller = controller
        controller
    }

    // The views have fixed positions and are connected to the
    // locos on scene establishment.

    private val throttleViews = Array(8) {
        ThrottleView(Rectangle(it * (display.width / 8), 0, (display.width / 8), display.height))
    }

    override fun build() {
        repeat(8) {
            controllers[it].connectToElements()
            throttleViews[it].throttle = throttleManager.throttleAtSlot(it)
            display.addView(throttleViews[it])
        }
    }

    override fun destroy() {
        repeat(8) {
            controllers[it].disconnectFromElements()
            throttleViews[it].throttle = null
            display.removeView(throttleViews[it])
        }
    }
}

class PanelScene(private val display: Push2Display) : Scene {
    private val panelView = PanelView(Rectangle(0, 0, display.width, display.height))

    override fun build() {
        display.addView(panelView)
    }

    override fun destroy() {
        display.removeView(panelView)
    }
}
