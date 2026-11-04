package dev.oxide.overlay

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/** Receives one JSON object per line on loopback only. */
class FrameSocketServer(
    private val port: Int = DEFAULT_PORT,
    private val onClientConnected: (() -> Unit)? = null
) {
    private val running = AtomicBoolean(false)
    @Volatile private var server: ServerSocket? = null
    @Volatile private var activeClient: Socket? = null
    @Volatile private var worker: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        worker = Thread({ serve() }, "oxide-overlay-frame-server").apply {
            isDaemon = true
            start()
        }
    }

    private fun serve() {
        try {
            val listener = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 4)
                soTimeout = 1000
            }
            server = listener
            while (running.get()) {
                try {
                    listener.accept().use { client ->
                        activeClient = client
                        onClientConnected?.invoke()
                        try { readFrames(client) } finally { activeClient = null }
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    // Periodically observe stop requests.
                } catch (_: java.io.IOException) {
                    if (running.get()) continue
                }
            }
        } catch (_: Exception) {
            // The overlay demo remains usable with its built-in test frame if the port is unavailable.
        } finally {
            runCatching { server?.close() }
            server = null
            running.set(false)
        }
    }

    private fun readFrames(socket: Socket) {
        val input = BufferedInputStream(socket.getInputStream())
        val line = ByteArrayOutputStream()
        while (running.get()) {
            val value = input.read()
            if (value < 0) break
            if (value == 10) {
                if (line.size() > 0) parseAndPublish(line.toByteArray())
                line.reset()
            } else if (value != 13) {
                if (line.size() >= MAX_LINE_BYTES) break
                line.write(value)
            }
        }
    }

    private fun parseAndPublish(bytes: ByteArray) {
        runCatching {
            val root = JSONObject(String(bytes, StandardCharsets.UTF_8))
            val array: JSONArray = root.optJSONArray("boxes") ?: return
            val boxes = ArrayList<OverlayBox>(minOf(array.length(), MAX_BOXES))
            for (index in 0 until minOf(array.length(), MAX_BOXES)) {
                val item = array.optJSONObject(index) ?: continue
                val distance = if (item.has("distance") && !item.isNull("distance"))
                    item.optDouble("distance", Double.NaN).toFloat() else null
                val box = OverlayBox(
                    left = item.optDouble("l", Double.NaN).toFloat(),
                    top = item.optDouble("t", Double.NaN).toFloat(),
                    right = item.optDouble("r", Double.NaN).toFloat(),
                    bottom = item.optDouble("b", Double.NaN).toFloat(),
                    label = item.optString("label", "").take(64),
                    distanceMeters = distance
                )
                if (box.isValid()) boxes.add(box)
            }
            OverlayFrameStore.publish(boxes)
        }
    }

    fun stop() {
        running.set(false)
        runCatching { activeClient?.close() }
        activeClient = null
        runCatching { server?.close() }
        server = null
        worker?.interrupt()
        worker = null
    }

    companion object {
        const val DEFAULT_PORT = 39472
        private const val MAX_LINE_BYTES = 64 * 1024
        private const val MAX_BOXES = 128
    }
}
