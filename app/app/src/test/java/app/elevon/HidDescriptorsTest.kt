package app.elevon

import app.elevon.hid.HidDescriptors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the composite HID report descriptor by walking its items. If a
 * descriptor edit introduces a malformed item (wrong size, unbalanced
 * collections), this fails before any computer ever sees it.
 */
class HidDescriptorsTest {

    private fun walk(descriptor: ByteArray): ParseResult {
        var i = 0
        var collections = 0
        var endCollections = 0
        val reportIds = mutableSetOf<Int>()
        while (i < descriptor.size) {
            val b0 = descriptor[i].toInt() and 0xFF
            val size = when ((b0 and 0x03)) {
                0 -> 0
                1 -> 1
                2 -> 2
                else -> 4
            }
            val tag = b0 and 0xFC
            val dataStart = i + 1
            if (dataStart + size > descriptor.size) error("truncated item at $i")
            when (tag) {
                0x84 -> reportIds.add(signed(descriptor, dataStart, size)) // REPORT_ID (tag 0x8, global, size 1 => b0 0x85; masked tag 0x84)
                0xA0 -> collections++
                0xC0 -> endCollections++
            }
            i += 1 + size
        }
        return ParseResult(collections, endCollections, reportIds)
    }

    private fun signed(d: ByteArray, start: Int, size: Int): Int {
        var v = 0L
        for (k in 0 until size) v = v or ((d[start + k].toLong() and 0xFF) shl (8 * k))
        // Report IDs are 0..255; treat as unsigned single byte (sign-extension only if size 1 & >=0x80 — IDs are small)
        return if (size == 1) (v and 0xFF).toInt() else v.toInt()
    }

    private data class ParseResult(
        val collections: Int,
        val endCollections: Int,
        val reportIds: Set<Int>,
    )

    @Test
    fun `composite descriptor parses cleanly`() {
        val result = walk(HidDescriptors.composite())
        assertEquals(result.collections, result.endCollections)
        assertEquals(setOf(1, 2, 3, 4, 5), result.reportIds)
    }

    @Test
    fun `basic descriptor has only keyboard and mouse`() {
        val result = walk(HidDescriptors.basic())
        assertEquals(result.collections, result.endCollections)
        assertEquals(setOf(1, 2), result.reportIds)
    }

    @Test
    fun `payload sizes match documented report layouts`() {
        assertEquals(8, HidDescriptors.keyboardPayloadSize) // modifiers + reserved + six key slots
        assertEquals(5, HidDescriptors.mousePayloadSize)
        assertEquals(2, HidDescriptors.consumerPayloadSize)
        assertEquals(1, HidDescriptors.systemPayloadSize)
        assertEquals(9, HidDescriptors.gamepadPayloadSize)
        assertTrue(HidDescriptors.composite().size > HidDescriptors.basic().size)
    }
}
