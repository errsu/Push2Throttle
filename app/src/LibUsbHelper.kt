package push2throttle

import org.usb4java.*

class LibUsbHelper {

    val context = Context()
    var abortHandlerThread = false

    init {
        if (LibUsb.init(context) != LibUsb.SUCCESS) {
            println("LibUsb: fatal error - can't initialize")
            System.exit(1)
        }
        println("LibUsb initialized")
        val usbHandlerThread = object : Thread() {
            override fun run() {
                while (!abortHandlerThread) {
                    val result = LibUsb.handleEventsTimeout(null, 500)
                    if (result != LibUsb.SUCCESS) {
                        println("LibUsb: unable to handle events: $result")
                    }
                }
            }
        }
        usbHandlerThread.start()
    }

    fun listDevices() {
        println("USB device listing")
        val list = DeviceList()
        val result = LibUsb.getDeviceList(context, list)

        if (result < 0) {
            println("Unable to get device list: code $result")
            return
        }

        for (device in list) {
            val descriptor = DeviceDescriptor()
            if (LibUsb.getDeviceDescriptor(device, descriptor) != LibUsb.SUCCESS) {
                continue
            }
            println("vendor: 0x%04x  product: 0x%04x".format(descriptor.idVendor(), descriptor.idProduct()))
        }

        LibUsb.freeDeviceList(list, true)
    }

    fun close() {
        LibUsb.exit(context)
    }
}
