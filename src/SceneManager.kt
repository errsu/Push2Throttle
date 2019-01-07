import java.awt.Rectangle

class SceneManager(private val display: Push2Display,
                   private val elements: Push2Elements,
                   private val throttleManager: ThrottleManager,
                   private val panelManager: PanelManager) : MidiController {

    private val throttleScene = ThrottleScene(display, elements, throttleManager)
    private val panelScene = PanelScene(display, elements, panelManager)
    private var currentScene: Scene? = null
    private val select = elements.getElement("Select")

    init {
        throttleManager.throttlesReassigned = this::sceneContentReassigned
        panelManager.turnoutsReassigned = this::sceneContentReassigned
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

    fun sceneContentReassigned() {
        currentScene?.destroy()
        currentScene?.build()
    }

    override fun <T : Any> elementStateChanged(element: MidiElement, newValue: T) {
        when (element) {
            select    -> gotoScene(if (newValue == true) "panel" else "throttles")
        }
    }
}

interface Scene {
    fun build()
    fun destroy()
}

class ThrottleScene(private val display: Push2Display,
                    private val elements: Push2Elements,
                    private val throttleManager: ThrottleManager) : Scene, MidiController {

    // The controllers have a fixed connection to the throttles,
    // which continously record the loco data (if a loco is assigned).
    // The controllers also know their elements, but are not
    // connected to them until the scene is established.

    private var page = 0

    private val controllers = Array(24) {
        val throttle = throttleManager.throttleAtSlot(it)
        val track = (it % 8) + 1
        val controller = Push2ThrottleController(
                elements,
                throttle,
                elements.getElement("Pot T$track")    as Erp,
                elements.getElement("Disp A T$track") as ButtonRgb,
                elements.getElement("Disp B T$track") as ButtonRgb)
        // TODO: the controller could establish this connection:
        throttle.controller = controller
        controller
    }

    private val pageLeft = elements.getElement("Page Left")
    private val pageRight = elements.getElement("Page Right")

    // The views have fixed positions and are connected to the
    // locos on scene establishment.

    private val throttleViews = Array(8) {
        ThrottleView(Rectangle(it * (display.width / 8), 0, (display.width / 8), display.height))
    }

    override fun build() {
        repeat(8) {
            val slot = page * 8 + it
            controllers[slot].connectToElements()
            throttleViews[it].throttle = throttleManager.throttleAtSlot(slot)
            display.addView(throttleViews[it])
        }
        if (page > 0) {
            elements.connect(pageLeft!!, this,
                    mapOf("onColor" to 124, "offColor" to 6, "type" to "momentary"),
                    false)
        }
        if (page < 2) {
            elements.connect(pageRight!!, this,
                    mapOf("onColor" to 124, "offColor" to 6, "type" to "momentary"),
                    false)
        }
    }

    override fun destroy() {
        repeat(8) {
            val slot = page * 8 + it
            controllers[slot].disconnectFromElements()
            throttleViews[it].throttle = null
            display.removeView(throttleViews[it])
        }
        elements.disconnect(pageLeft!!)
        elements.disconnect(pageRight!!)
    }

    override fun <T : Any> elementStateChanged(element: MidiElement, newValue: T) {
        when (element) {
            pageLeft  -> if (newValue == true) {
                if (page > 0) {
                    destroy()
                    page--
                    build()
                }
            }
            pageRight -> if (newValue == true) {
                if (page < 2) {
                    destroy()
                    page++
                    build()
                }
            }
        }
    }
}

class PanelScene(private val display: Push2Display,
                 private val elements: Push2Elements,
                 private val panelManager: PanelManager) : Scene {

    private val panelView = PanelView4(Rectangle(0, 0, display.width, display.height))
    private val buttons = Array(16) { elements.getElement("Disp ${if (it < 8) "A" else "B"} T${(it % 8) + 1}") as ButtonRgb}
    private val controller = Push2TurnoutController(elements, panelManager.turnoutTable, buttons)

    override fun build() {
        panelManager.turnoutTable.controller = controller
        panelView.turnoutViews.forEach { turnoutView ->
            turnoutView.turnout = panelManager.turnoutTable.turnoutWithUserName(turnoutView.name)
            controller.turnouts[turnoutView.elementIndex] = turnoutView.turnout
        }
        controller.connectToElements()
        panelManager.turnoutsMoved = panelView::update
        panelView.update()
        display.addView(panelView)
    }

    override fun destroy() {
        panelManager.turnoutTable.controller = null
        panelView.turnoutViews.forEach { turnoutView ->
            turnoutView.turnout = null
        }
        for (index in controller.turnouts.indices) {
            controller.turnouts[index] = null
        }
        controller.disconnectFromElements()
        panelManager.turnoutsMoved = {}
        display.removeView(panelView)
    }
}
