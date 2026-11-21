package com.example.notebook.ui

import android.content.res.Resources
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A short message for a snackbar, optionally with an action such as "Отменить".
 * Text is resolved from resources at display time, so it follows the current language.
 */
class UserMessage(
    val text: (Resources) -> String,
    val actionLabel: ((Resources) -> String)? = null,
    val action: (suspend () -> Unit)? = null,
)

/**
 * App-wide queue of snackbar messages. Lets one screen trigger a message that
 * another screen shows: the editor moves a note to the trash and closes, and the
 * main screen then shows "Заметка в корзине · Отменить".
 */
class UserMessenger {
    private val channel = Channel<UserMessage>(Channel.BUFFERED)

    /** Each message is delivered to exactly one collector, once. */
    val messages: Flow<UserMessage> = channel.receiveAsFlow()

    fun send(message: UserMessage) {
        channel.trySend(message)
    }
}
