import org.usb4java.*
import java.nio.ByteBuffer

class Push2Display {
    var isOpen = false
    private val outEndpoint:   Byte  = 0x01.toByte()
    private val vendorAbleton: Short = 0x2982
    private val productPush2:  Short = 0x1967

    private val context = Context()
    private var push2DeviceHandle: DeviceHandle? = null

    private val frameHeaders = Array<ByteBuffer>(2) {
        BufferUtils.allocateByteBuffer(16)
    }
    private val headerTransfers = Array<Transfer>(2) { LibUsb.allocTransfer() }

    private val frameBuffers = Array<ByteBuffer>(2) {
        BufferUtils.allocateByteBuffer(960*160*2)
    }
    private val bufferTransfers = Array<Transfer>(2) { LibUsb.allocTransfer() }

    private var abortHandlerThread: Boolean = false
    private val usbHandlerThread = object : Thread() {
        override fun run() {
            while (!abortHandlerThread) {
                val result = LibUsb.handleEventsTimeout(null, 500000)
                if (result != LibUsb.SUCCESS)
                    throw LibUsbException("Unable to handle events", result)
            }
        }
    }

    init {
        if (LibUsb.init(context) != LibUsb.SUCCESS) {
            println("fatal error: can't initialize libusb")
            System.exit(1)
        }
        println("LibUsb initialized")
        repeat(frameHeaders.size) {
            frameHeaders[it].asIntBuffer().put(intArrayOf(
                    0xFFCCAA88.toInt(),
                    0x00000000,
                    0x00000000,
                    0x00000000))
        }
    }

    @Volatile
    private var frameCount = 0

    private val headerFinished = TransferCallback { transfer ->
        LibUsb.submitTransfer(transfer)
    }

    private val bufferFinished = TransferCallback { transfer ->
        val buffer = transfer.buffer().asShortBuffer()
        repeat(buffer.remaining()) {
            buffer.put((frameCount.and(0x0000FFFF).toShort()))
        }
        LibUsb.submitTransfer(transfer)
        frameCount += 1
        if (frameCount == 600) {
            frameCount = 0
            println("fps")
        }
    }

    private var updateCount = 0

    fun update() {
        updateCount++
    }

    fun listDevices() {
        println("USB device listing")
        val list = DeviceList()
        val context = Context()
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

    private fun openPush2Display(): Boolean {

        val list = DeviceList()
        if (LibUsb.getDeviceList(context, list) > 0) {
            for (device in list) {
                val descriptor = DeviceDescriptor()
                if (LibUsb.getDeviceDescriptor(device, descriptor) == LibUsb.SUCCESS) {
                    if (descriptor.idVendor() == vendorAbleton && descriptor.idProduct() == productPush2) {
                        val deviceHandle = DeviceHandle()
                        if (LibUsb.open(device, deviceHandle) == LibUsb.SUCCESS) {
                            LibUsb.setConfiguration(deviceHandle, 1)
                            LibUsb.claimInterface(deviceHandle, 0)
                            LibUsb.setInterfaceAltSetting(deviceHandle, 0, 0)
                            repeat (headerTransfers.size) {
                                LibUsb.fillBulkTransfer(headerTransfers[it], deviceHandle, outEndpoint,
                                        frameHeaders[it], headerFinished, null, 5000)
                            }
                            repeat (bufferTransfers.size) {
                                LibUsb.fillBulkTransfer(bufferTransfers[it], deviceHandle, outEndpoint,
                                                        frameBuffers[it], bufferFinished, null, 5000)
                            }
                            push2DeviceHandle = deviceHandle
                            usbHandlerThread.start()
                            break
                        }
                    }
                }
            }
            LibUsb.freeDeviceList(list, true)
        }
        return push2DeviceHandle != null
    }

    fun open() {
        if (openPush2Display()) {
            isOpen = true
            println("Push2 display opened")

            frameCount = 0
            repeat(bufferTransfers.size) {
                val buffer = bufferTransfers[it].buffer().asShortBuffer()
                repeat(buffer.remaining()) {
                    buffer.put((frameCount.and(0x0000FFFF).toShort()))
                }
                val success1 = LibUsb.submitTransfer(headerTransfers[it])
                val success2 = LibUsb.submitTransfer(bufferTransfers[it])
                println("submit: $success1 $success2")
                frameCount += 1
            }
        }
    }

    fun close() {
        if (push2DeviceHandle != null)
        {
            abortHandlerThread = true
            // TODO: wait until aborted
            LibUsb.close(push2DeviceHandle)
        }
        isOpen = false
    }
}
