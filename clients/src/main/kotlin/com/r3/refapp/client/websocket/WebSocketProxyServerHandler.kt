package com.r3.refapp.client.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.client.auth.Constants.ACCOUNT_ID_PROPERTY
import com.r3.refapp.client.auth.Constants.CUSTOMER_ID_PROPERTY
import com.r3.refapp.client.auth.Constants.PLACEHOLDER_PREFIX
import com.r3.refapp.client.auth.Constants.PLACEHOLDER_SUFFIX
import com.r3.refapp.client.auth.Constants.PROPERTY_NAME
import com.r3.refapp.client.auth.Constants.PROPERTY_VALUE
import com.r3.refapp.client.auth.Constants.ROLE_NAME_CUSTOMER
import com.r3.refapp.client.response.MessageType
import com.r3.refapp.client.response.WebSocketMessage
import com.r3.refapp.schemas.AccountStateSchemaV1
import com.r3.refapp.states.Account
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import org.apache.commons.lang3.StringUtils
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.stereotype.Component
import org.springframework.util.PropertyPlaceholderHelper
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.AbstractWebSocketHandler
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Spring managed bean, implementation of [AbstractWebSocketHandler]. Overrides [afterConnectionEstablished] function
 * in order to provide custom websocket notification behaviour.
 * @param tokenStore Spring's [TokenStore] object
 * @param rpc Corda's node RPC connection object
 * @param objectMapper Jackson [ObjectMapper]
 */
@Component
class WebSocketProxyServerHandler(private val tokenStore: TokenStore,
                                  private val rpc: NodeRPCConnection,
                                  private val objectMapper: ObjectMapper): AbstractWebSocketHandler() {

    private val proxy = rpc.proxy

    /**
     * Spring's [PropertyPlaceholderHelper] object which is used to render template message
     */
    private val placeholderHelper = PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, null, false)


    /**
     * Overrides [handleMessage] function of the [AbstractWebSocketHandler] in order to provide 'Bank in a box'
     * notifications to the websocket consumer. Websocket connections goes through standard Authorisation and
     * Authentication scheme, websocket connection is allowed only to users with CUSTOMER role. After websocket
     * connection is established [Observable] object via standard Corda API is created in order to get notifications
     * for all Account state changes that are owned by the Customer from Corda. On state update event reception
     * handler will propagate appropriate message [WebSocketMessage] to websocket channel.
     * @param session Spring's [WebSocketSession] object
     */
    @Throws(Exception::class)
    override fun handleMessage(session: WebSocketSession, message: org.springframework.web.socket.WebSocketMessage<*>) {

        val authentication = tokenStore.readAuthentication(message.payload.toString())
        val accessToken = tokenStore.readAccessToken(message.payload.toString())
        SecurityContextHolder.getContext().authentication = authentication.userAuthentication

        if(authentication.isAuthenticated && authentication.authorities.contains(SimpleGrantedAuthority(ROLE_NAME_CUSTOMER))) {

            val customerId = accessToken.additionalInformation[CUSTOMER_ID_PROPERTY]
            val customerUUID = UUID.fromString(customerId.toString())

            val expression = builder { AccountStateSchemaV1.PersistentBalance::customerId.equal(customerUUID) }
            val customerIdCriteria = QueryCriteria.VaultCustomQueryCriteria(expression)

            proxy.vaultTrackByCriteria(Account::class.java, customerIdCriteria).updates.subscribe { update ->
                generateWebSocketMessages(update, customerUUID).forEach {
                    it.let { session.sendMessage(TextMessage(objectMapper.writeValueAsString(it))) }
                }
            }
        } else {
            session.close(CloseStatus.NOT_ACCEPTABLE)
        }

    }

    /**
     * Generates appropriate websocket messages, for Customer with [customerId], based on [Vault.Update] received from
     * Corda.
     * @param vaultUpdate Corda's [Vault.Update] object
     * @param customerId Id of the Customer
     * @return Optional [WebSocketMessage] message to be sent to websocket channel
     */
    private fun generateWebSocketMessages(vaultUpdate: Vault.Update<Account>, customerId: UUID) : List<WebSocketMessage?> {
        val producedList = vaultUpdate.produced.filter { it.state.data.accountData.customerId == customerId }

        return producedList.map { produced ->
            val consumed = vaultUpdate.consumed.singleOrNull { it.state.data.accountData.accountId == produced.state.data.accountData.accountId }
                    ?: return@map constructWebSocketMessage(MessageType.ACCOUNT_CREATED, produced.state.data.accountData
                            .accountId, null, null)

            val accountDataProperties = (produced.state.data.accountData::class).memberProperties.filter { it != produced.state
                    .data.accountData.txDate::class }
            val accountProperties = (produced.state.data::class).memberProperties.filter { it != produced.state.data.accountData::class }

            evaluateProducedAndConsumed(accountDataProperties, produced.state.data.accountData, consumed.state.data.accountData,
                    produced.state.data.accountData.accountId)?.let { return@map  it }
            evaluateProducedAndConsumed(accountProperties, produced, consumed.state.data,
                    produced.state.data.accountData.accountId)?.let { return@map  it }
        }.toList()
    }

    /**
     * Helper function used to evaluate field changes between [produced] and [consumed] states.
     * @param properties Account's properties to be evaluated
     * @param produced Produced Account's state
     * @param consumed Consumed Account's state
     * @param accountId ID of the Account
     * @return Optional [WebSocketMessage] message to be sent to websocket channel
     */
    private inline fun <reified T> evaluateProducedAndConsumed(properties: Collection<KProperty1<out T, Any?>>,
                                                               produced: T, consumed: T, accountId: UUID): WebSocketMessage? {
        properties.forEach {
            val valueProduced = it.getter.call(produced)
            val valueConsumed = it.getter.call(consumed)
            if (valueConsumed != valueProduced) {
                return constructWebSocketMessage(MessageType.PROPERTY_CHANGED, accountId, it.name.capitalize(), valueProduced)
            }
        }
        return null
    }

    /**
     * Constructs [WebSocketMessage] object based on the [messageType] and properties passed. Each [messageType]
     * object contains message template string which is rendered by applying provided properties.
     * @param messageType [MessageType] which enumerates websocket message types
     * @param accountId Id of the account
     * @param propertyName (Optional) name of the property of account objects which has been changed
     * @param propertyValue (Optional) new value of the changed property
     * @return Returns constructed [WebSocketMessage]
     */
    private fun constructWebSocketMessage(messageType: MessageType, accountId: UUID, propertyName: String?, propertyValue: Any?) :
            WebSocketMessage {

        val templateMessage = messageType.messageTemplate
        val properties = Properties()
        properties.putAll(mapOf(
                ACCOUNT_ID_PROPERTY to accountId.toString(),
                PROPERTY_NAME to (propertyName?.let { it } ?: StringUtils.EMPTY),
                PROPERTY_VALUE to (propertyValue?.let { it.toString() } ?: StringUtils.EMPTY)))

        val renderedMessage = placeholderHelper.replacePlaceholders(messageType.messageTemplate, properties)

        return WebSocketMessage(Locale.ENGLISH.isO3Language, messageType, accountId, propertyName, propertyValue?.toString(),
                templateMessage, renderedMessage)
    }
}