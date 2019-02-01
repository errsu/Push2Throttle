package push2throttle

import org.usb4java.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Push2DisplayDriver(private val libUsbHelper: LibUsbHelper, private val display: Push2Display) {

    private var startTime: Long = 0
    private var framesCompleted = 0

    private fun initializeFpsMeasurement() {
        startTime = System.currentTimeMillis()
    }

    private fun onAfterFrameCompleted() {
        if (++framesCompleted % 600 == 0) {
            val now = System.currentTimeMillis()
            println("fps: ${600.0 * 1000.0 / (now - startTime).toFloat()}")
            startTime = now
        }
    }

    private val outEndpoint:   Byte  = 0x01.toByte()
    private val vendorAbleton: Short = 0x2982
    private val productPush2:  Short = 0x1967

    private var push2DeviceHandle: DeviceHandle? = null

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

    private fun closePush2Display()
    {
        if (push2DeviceHandle != null)
        {
            LibUsb.releaseInterface(push2DeviceHandle, 0)
            LibUsb.close(push2DeviceHandle)
        }
    }

    private val numBuffers           = 3
    private val linesPerBuffer       = 8
    private val bufferSizeInBytes    = 16 * 1024 // buffer length in bytes
    private val buffersPerFrame      = 20

    private fun b(x: Int) = x.toByte()
    private fun i(x: Long) = x.toInt()

    private val headerTransfer = LibUsb.allocTransfer()
    private val frameHeader = BufferUtils.allocateByteBuffer(16).put(byteArrayOf(
            b(0xFF), b(0xCC), b(0xAA), b(0x88),
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00))


    private val bufferTransfers = Array<Transfer>(numBuffers) { LibUsb.allocTransfer() }
    private val frameBuffers = Array<ByteBuffer>(numBuffers) {
        BufferUtils.allocateByteBuffer(bufferSizeInBytes).order(ByteOrder.LITTLE_ENDIAN)
    }

    private var frame = 0
    private var indexOfBufferInFrame = 0

    private fun prepareAndSubmitNextSendRequest(transfer: Transfer) {
        val firstLine = indexOfBufferInFrame * linesPerBuffer
        display.fillBufferWithLines(transfer.buffer(), frame, firstLine, linesPerBuffer)

        val intBuffer = transfer.buffer().asIntBuffer()
        repeat(intBuffer.limit()) {
            intBuffer.put(it, intBuffer[it].xor(0xFFE7F3E7.toInt()))
        }

        if (indexOfBufferInFrame == 0) {
            LibUsb.submitTransfer(headerTransfer)
        }

        LibUsb.submitTransfer(transfer)

        indexOfBufferInFrame++

        if (indexOfBufferInFrame == buffersPerFrame) {
            frame++
            indexOfBufferInFrame = 0
        }
    }

    private val transferFinished = TransferCallback { transfer ->
        // TODO: check if we want to terminate on failure
        when {
            transfer.status() != LibUsb.TRANSFER_COMPLETED ->
                println(when(transfer.status()) {
                    LibUsb.TRANSFER_ERROR     -> "error: transfer failed"
                    LibUsb.TRANSFER_TIMED_OUT -> "error: transfer timed out"
                    LibUsb.TRANSFER_CANCELLED -> "error: transfer was cancelled"
                    LibUsb.TRANSFER_STALL     -> "error: endpoint stalled/control request not supported"
                    LibUsb.TRANSFER_NO_DEVICE -> "error: device was disconnected"
                    LibUsb.TRANSFER_OVERFLOW  -> "error: device sent more data than requested"
                    else -> "error: snd transfer failed with status ${transfer.status()}"
                })
            transfer.length() != transfer.actualLength() ->
                println("error: only transferred ${transfer.actualLength()} of ${transfer.length()} bytes\n")
            transfer == headerTransfer ->
                onAfterFrameCompleted()
            else ->
                prepareAndSubmitNextSendRequest(transfer)
        }
    }

    private fun submitInitialSendRequests() {
        LibUsb.fillBulkTransfer(headerTransfer, push2DeviceHandle, outEndpoint,
                frameHeader, transferFinished, null, 1000)

        repeat(numBuffers) {
            // the loop is endless, so requests are never released using libusb_free_transfer
            LibUsb.fillBulkTransfer(bufferTransfers[it], push2DeviceHandle, outEndpoint,
                    frameBuffers[it], transferFinished, null, 1000)
            prepareAndSubmitNextSendRequest(bufferTransfers[it])
        }
    }

    var isOpen = false

    fun open() {
        if (openPush2Display()) {
            isOpen = true
            println("Push2 display opened")
            initializeFpsMeasurement()
            submitInitialSendRequests()
        }
    }

    fun close() {
        if (isOpen) {
            closePush2Display()
            isOpen = false
        }
    }
}
