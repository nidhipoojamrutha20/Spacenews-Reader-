
package com.example.spacenews.ui.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingSpeechRequestQueueTest {

    @Test
    fun storesPendingSpeechRequest() {
        val queue = PendingSpeechRequestQueue()

        queue.store("Space news article", 1.0f)

        assertEquals(
            PendingSpeechRequest("Space news article", 1.0f),
            queue.take()
        )
    }

    @Test
    fun newerRequestReplacesPreviousPendingRequest() {
        val queue = PendingSpeechRequestQueue()

        queue.store("First article", 0.8f)
        queue.store("Second article", 1.2f)

        assertEquals(
            PendingSpeechRequest("Second article", 1.2f),
            queue.take()
        )
    }

    @Test
    fun takingRequestConsumesItOnlyOnce() {
        val queue = PendingSpeechRequestQueue()

        queue.store("Space news", 1.0f)

        assertEquals(
            PendingSpeechRequest("Space news", 1.0f),
            queue.take()
        )

        assertNull(queue.take())
    }

    @Test
    fun clearingQueueRemovesPendingRequest() {
        val queue = PendingSpeechRequestQueue()

        queue.store("Space news", 1.0f)
        queue.clear()

        assertNull(queue.take())
    }
}
