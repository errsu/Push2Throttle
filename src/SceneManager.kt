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

abstract class ScenePager (
        private val elements: Push2Elements,
        private val pageCount: Int) : MidiController {

    var page = 0
    private val selectedIndex = Array(pageCount) {-1}
    private val pageLeft = elements.getElement("Page Left")
    private val pageRight = elements.getElement("Page Right")

    abstract fun build()
    abstract fun destroy()

    fun saveSelectedIndex(index: Int) {
        selectedIndex[page] = index
    }

    fun restoreSelectedIndex() : Int {
        return selectedIndex[page]
    }

    fun connectPager() {
        if (page > 0) {
            elements.connect(pageLeft!!, this,
                    mapOf("onColor" to 124, "offColor" to 6, "type" to "momentary"),
                    false)
        }
        if (page < pageCount - 1) {
            elements.connect(pageRight!!, this,
                    mapOf("onColor" to 124, "offColor" to 6, "type" to "momentary"),
                    false)
        }
    }

    fun disconnectPager() {
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
                if (page < pageCount - 1) {
                    destroy()
                    page++
                    build()
                }
            }
        }
    }
}

interface SelectionManager {
    fun requestSelection(controller: Push2ThrottleController?)
}

class ThrottleScene(private val display: Push2Display,
                    private val elements: Push2Elements,
                    private val throttleManager: ThrottleManager) :
        Scene, ScenePager(elements, 3), SelectionManager {

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
                elements.getElement("Disp B T$track") as ButtonRgb,
                elements.getElement("Pad S1 T$track") as Pad,
                this)
        // TODO: the controller could establish this connection:
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
            val slot = page * 8 + it
            controllers[slot].connectToElements()
            throttleViews[it].throttle = throttleManager.throttleAtSlot(slot)
            display.addView(throttleViews[it])
            controllers[slot].selected = (restoreSelectedIndex() == it)
        }
        connectPager()
    }

    override fun destroy() {
        saveSelectedIndex(-1)
        repeat(8) {
            val slot = page * 8 + it
            if (controllers[slot].selected) {
                controllers[slot].selected = false
                saveSelectedIndex(it)
            }
            controllers[slot].disconnectFromElements()
            throttleViews[it].throttle = null
            display.removeView(throttleViews[it])
        }
        disconnectPager()
    }

    override fun requestSelection(controller: Push2ThrottleController?) {
        repeat(8) { track ->
            val slot = page * 8 + track
            controllers[slot].run {
                this.selected = (this == controller)
            }
        }
    }
}

class PanelScene(private val display: Push2Display,
                 private val elements: Push2Elements,
                 private val panelManager: PanelManager) : Scene, ScenePager(elements, 4) {

    private val panelViews = listOf(
            PanelView0(Rectangle(0, 0, display.width, display.height)),
            PanelView1(Rectangle(0, 0, display.width, display.height)),
            PanelView2(Rectangle(0, 0, display.width, display.height)),
            PanelView3(Rectangle(0, 0, display.width, display.height)))

    private val buttons = Array(16) { elements.getElement("Disp ${if (it < 8) "A" else "B"} T${(it % 8) + 1}") as ButtonRgb}
    private val controller = Push2TurnoutController(elements, panelManager.turnoutTable, buttons)

    override fun build() {
        val panelView = panelViews[page]
        panelManager.turnoutTable.controller = controller
        panelView.turnoutViews.forEach { turnoutView ->
            turnoutView.turnout = panelManager.turnoutTable.turnoutWithUserName(turnoutView.name)
            controller.turnouts[turnoutView.elementIndex] = turnoutView.turnout
        }
        controller.connectToElements()
        panelManager.turnoutsMoved = panelView::update
        panelView.update()
        display.addView(panelView)
        connectPager()
    }

    override fun destroy() {
        val panelView = panelViews[page]
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
        disconnectPager()
    }
}
