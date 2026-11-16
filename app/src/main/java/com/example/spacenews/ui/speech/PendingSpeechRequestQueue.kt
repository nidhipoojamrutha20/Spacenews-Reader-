
package com.example.spacenews.ui.speech

internal data class PendingSpeechRequest(
    val text: String,
    val rate: Float
)

internal class PendingSpeechRequestQueue {

    private var pending: PendingSpeechRequest? = null

    fun store(text: String, rate: Float) {
        pending = PendingSpeechRequest(text, rate)
    }

    fun take(): PendingSpeechRequest? {
        val request = pending
        pending = null
        return request
    }

    fun clear() {
        pending = null
    }
}
