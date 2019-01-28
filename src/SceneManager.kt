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
    fun getSelectedColumn() : Int?
    fun getThrottleAtColumn(column: Int) : JmriThrottle
    fun getThrottleColumn(throttle: JmriThrottle) : Int?
}

class ThrottleScene(private val display: Push2Display,
                    private val elements: Push2Elements,
                    private val throttleManager: ThrottleManager) :
        Scene, ScenePager(elements, 3), SelectionManager {

    // The throttle controllers have a fixed connection to the throttles,
    // which continously record the loco data (if a loco is assigned).
    // There is only one loco function controller, which is known by all
    // throttles. It controls them depending on page and selection.
    // The controllers know their elements, but are connected to them only
    // when the scene is established or the selection changes.

    private val throttleControllers = Array(throttleManager.slotCount) { slot ->
        val track = (slot % 8) + 1
        Push2ThrottleController(
                elements,
                throttleManager.throttleAtSlot(slot),
                elements.getElement("Pot T$track")    as Erp,
                elements.getElement("Disp A T$track") as ButtonRgb,
                elements.getElement("Disp B T$track") as ButtonRgb,
                this)
    }

    private val locoFunctionController = Push2LocoFunctionController(
            elements,
            throttleManager,
            Array(8) { row ->
                Array(8) { col ->
                    elements.getElement("Pad S${row + 1} T${col + 1}") as Pad
                }
            },
            this)

    // The views have fixed positions and are connected to the
    // locos on scene establishment.

    private val throttleViews = Array(8) {
        ThrottleView(Rectangle(it * (display.width / 8), 0, (display.width / 8), display.height), this)
    }

    override fun build() {
        repeat(8) {
            val slot = page * 8 + it
            throttleControllers[slot].connectToElements()
            throttleViews[it].throttle = throttleManager.throttleAtSlot(slot)
            display.addView(throttleViews[it])
            throttleControllers[slot].selected = (restoreSelectedIndex() == it)
        }
        locoFunctionController.connectToElements()
        connectPager()
    }

    override fun destroy() {
        saveSelectedIndex(-1)
        repeat(8) {
            val slot = page * 8 + it
            if (throttleControllers[slot].selected) {
                throttleControllers[slot].selected = false
                saveSelectedIndex(it)
            }
            throttleControllers[slot].disconnectFromElements()
            throttleViews[it].throttle = null
            display.removeView(throttleViews[it])
        }
        locoFunctionController.disconnectFromElements()
        disconnectPager()
    }

    override fun requestSelection(controller: Push2ThrottleController?) {
        locoFunctionController.disconnectFromElements()
        repeat(8) { track ->
            val slot = page * 8 + track
            throttleControllers[slot].selected = (throttleControllers[slot] == controller)
        }
        locoFunctionController.connectToElements()
    }

    override fun getSelectedColumn() : Int? {
        repeat(8) {
            val slot = page * 8 + it
            if (throttleControllers[slot].selected) {
                return it
            }
        }
        return null
    }

    override fun getThrottleAtColumn(column: Int) : JmriThrottle {
        return throttleManager.throttleAtSlot(page * 8 + column)
    }

    override fun getThrottleColumn(throttle: JmriThrottle) : Int? {
        val col = throttle.slot - page * 8
        return if (col < 0 || col > 7) null else col
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
