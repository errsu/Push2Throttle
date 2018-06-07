import org.usb4java.*
import java.nio.ByteBuffer

class Push2Display(val libUsbHelper: LibUsbHelper) {

    var isOpen = false
    private val outEndpoint:   Byte  = 0x01.toByte()
    private val vendorAbleton: Short = 0x2982
    private val productPush2:  Short = 0x1967

    private var push2DeviceHandle: DeviceHandle? = null

    private val BUFFERCOUNT = 4
    private val frameHeaders = Array<ByteBuffer>(BUFFERCOUNT) {
        BufferUtils.allocateByteBuffer(16)
    }
    private val headerTransfers = Array<Transfer>(BUFFERCOUNT) { LibUsb.allocTransfer() }

    private val frameBuffers = Array<ByteBuffer>(BUFFERCOUNT) {
        BufferUtils.allocateByteBuffer(1024*160*2)
    }
    private val bufferTransfers = Array<Transfer>(BUFFERCOUNT) { LibUsb.allocTransfer() }

    init {
        repeat(frameHeaders.size) {
            frameHeaders[it].asIntBuffer().put(intArrayOf(
                    0xFFCCAA88.toInt(),
                    0x00000000,
                    0x00000000,
                    0x00000000))
        }
        repeat(headerTransfers.size) {
            headerTransfers[it] = LibUsb.allocTransfer()
        }

        repeat(bufferTransfers.size) {
            bufferTransfers[it] = LibUsb.allocTransfer()
        }
    }

    @Volatile
    private var frameCount = 0
    private var startTime = System.currentTimeMillis()

    private val headerFinished = TransferCallback { transfer ->
        LibUsb.submitTransfer(transfer)
    }

    private val frameFinished = TransferCallback { transfer ->
        if (transfer.status() == LibUsb.TRANSFER_COMPLETED) {
            when (transfer.userData()) {
                "header" -> LibUsb.submitTransfer(transfer)
                "data" -> {
                    val buffer = transfer.buffer().asIntBuffer()
                    val color : Int = mapOf(
                            0 to 0x00F800F8,
                            1 to 0xE007E007.toInt(),
                            2 to 0x1F001F00,
                            3 to 0xFFFFFFFF.toInt())
                            // .getOrDefault(2 + frameCount % 2, 0)
                            .getOrDefault((frameCount / 60) % 4, 0)
                            .xor(0xE7F3E7FF.toInt())
                    repeat(buffer.remaining()) {
                        buffer.put(color)
                    }
                    val bufferX = transfer.buffer().asShortBuffer()
                    repeat(160) {
                        bufferX.put(it * 1024 + frameCount % 960, if (frameCount % 2 == 0) 0xE7F3.toShort() else 0xE7FF.toShort())
                    }
                    LibUsb.submitTransfer(transfer)
                    frameCount += 1
                    if (frameCount % 600 == 0) {
                        val now = System.currentTimeMillis()
                        println("fps: ${600.0 * 1000.0 / (now - startTime).toFloat()}")
                        startTime = now
                    }
                }
            }
        }
        else {
            println("transfer failed: ${transfer.status()}")
        }
    }

    private var updateCount = 0

    fun update() {
        updateCount++
    }

    private fun openPush2Display(): Boolean {

        val list = DeviceList()
        if (LibUsb.getDeviceList(libUsbHelper.context, list) > 0) {
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
                                        frameHeaders[it], frameFinished, "header", 1000)
                            }
                            repeat (bufferTransfers.size) {
                                LibUsb.fillBulkTransfer(bufferTransfers[it], deviceHandle, outEndpoint,
                                                        frameBuffers[it], frameFinished, "data", 1000)
                            }
                            push2DeviceHandle = deviceHandle
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
                    buffer.put(0.toShort())
                }
                LibUsb.submitTransfer(headerTransfers[it])
                LibUsb.submitTransfer(bufferTransfers[it])
                frameCount += 1
            }
            startTime = System.currentTimeMillis()
        }
    }

    fun close() {
        if (push2DeviceHandle != null)
        {
            // abortHandlerThread = true
            // TODO: wait until aborted
            LibUsb.close(push2DeviceHandle)
        }
        isOpen = false
    }
}
