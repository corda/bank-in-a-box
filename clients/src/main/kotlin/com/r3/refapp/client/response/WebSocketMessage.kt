package com.r3.refapp.client.response

import java.util.*

/**
 * MessageType enumerates different types of messages that client can receive via WebSocket channel. Each message
 * type has associated message template which is rendered when sent to client.
 * @param messageTemplate Message template specific to each [MessageType]
 */
enum class MessageType(val messageTemplate: String) {
    ACCOUNT_CREATED("New account with accountId: \${accountId} created!"),
    PROPERTY_CHANGED("\${propertyName} changed for account with id: \${accountId}, new \${propertyName} = " +
            "\${propertyValue}")
}

/**
 * WebSocketMessage which is serialised and sent to Websocket client in JSON format.
 * @param isoLanguageCode Message language in ISO format
 * @param messageType Type of the Websocket message
 * @param accountId Id of the account
 * @param propertyName Name of the changed property on the account object
 * @param newPropertyValue New value of the changed property on the account object
 * @param templateMessage Message in the template form
 * @param renderedMessage Rendered WebSocket message
 */
data class WebSocketMessage(val isoLanguageCode: String,
                            val messageType: MessageType,
                            val accountId: UUID,
                            val propertyName: String?,
                            val newPropertyValue: String?,
                            val templateMessage: String,
                            val renderedMessage: String)