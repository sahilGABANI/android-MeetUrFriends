package com.meetfriend.app.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*

class ProgressRequestBody(
    private val requestBody: RequestBody,
    private val callbacks: UploadCallbacks
) : RequestBody() {

    private var countingSink: CountingSink? = null

    interface UploadCallbacks {
        fun onProgressUpdate(percentage: Double)
    }

    override fun contentType(): MediaType? = requestBody.contentType()

    override fun contentLength(): Long = requestBody.contentLength()

    override fun writeTo(sink: BufferedSink) {
        countingSink = CountingSink(sink)
        val bufferedSink = countingSink!!.buffer()

        requestBody.writeTo(bufferedSink)

        bufferedSink.flush()
    }

    fun clearCallback() {
        countingSink?.let {
            countingSink = null
            it.clearCallback()
        }
    }

    inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten = 0L

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            callbacks.onProgressUpdate((bytesWritten.toDouble() / contentLength()).coerceIn(0.0, 1.0) * 100)
        }

        fun clearCallback() {
        }
    }
}
