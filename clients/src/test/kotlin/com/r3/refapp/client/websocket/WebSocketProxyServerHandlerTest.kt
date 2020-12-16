package com.r3.refapp.client.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.client.auth.Constants.CUSTOMER_ID_PROPERTY
import com.r3.refapp.client.auth.Constants.ROLE_NAME_CUSTOMER
import com.r3.refapp.client.response.MessageType
import com.r3.refapp.client.response.WebSocketMessage
import com.r3.refapp.states.Account
import com.r3.refapp.states.AccountData
import com.r3.refapp.states.AccountStatus
import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.util.EUR
import com.r3.refapp.util.of
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.bufferUntilSubscribed
import net.corda.core.internal.uncheckedCast
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.services.Vault
import net.corda.testing.core.TestIdentity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import rx.Observable
import rx.subjects.PublishSubject
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull


@RunWith(MockitoJUnitRunner::class)
class WebSocketProxyServerHandlerTest {

    private val nodeRPCConnection = Mockito.mock(NodeRPCConnection::class.java)
    private val objectMapper = ObjectMapper()
    private val proxy = Mockito.mock(CordaRPCOps::class.java)
    private val tokenStore = Mockito.mock(TokenStore::class.java)
    private val webSocketSession = Mockito.mock(WebSocketSession::class.java)
    private val oAuth2Authentication = Mockito.mock(OAuth2Authentication::class.java)
    private val oAuth2AccessToken = Mockito.mock(OAuth2AccessToken::class.java)
    private val vaultDataFeed = Mockito.mock(DataFeed::class.java)
    private val customerId = UUID.randomUUID()

    lateinit var webSocketProxyServerHandler: WebSocketProxyServerHandler

    @Before
    fun setUp() {
        objectMapper.registerKotlinModule()
        Mockito.`when`(nodeRPCConnection.proxy).thenReturn(proxy)
        webSocketProxyServerHandler = WebSocketProxyServerHandler(tokenStore, nodeRPCConnection, objectMapper)
        Mockito.`when`(oAuth2Authentication.isAuthenticated).thenReturn(true)
        Mockito.`when`(oAuth2Authentication.authorities).thenReturn(listOf(SimpleGrantedAuthority(ROLE_NAME_CUSTOMER)))
        Mockito.`when`(tokenStore.readAuthentication(oAuth2AccessToken.toString())).thenReturn(oAuth2Authentication)
        Mockito.`when`(tokenStore.readAccessToken(oAuth2AccessToken.toString())).thenReturn(oAuth2AccessToken)
        Mockito.`when`(oAuth2AccessToken.additionalInformation).thenReturn(mapOf(CUSTOMER_ID_PROPERTY to customerId))
        Mockito.`when`(proxy.vaultTrackByCriteria(eq(Account::class.java), any())).thenReturn(vaultDataFeed
                as DataFeed<Vault.Page<Account>, Vault.Update<Account>>)
    }

    @Test
    fun `test afterConnectionEstablished empty principal session closed`() {

        webSocketProxyServerHandler.afterConnectionEstablished(webSocketSession)

        assertFalse(webSocketSession.isOpen)
    }

    @Test
    fun `test afterConnectionEstablished non OAuth2 authentication session closed`() {

        val fakeAuthentication = Mockito.mock(OAuth2Authentication::class.java)
        val fakeAccessToken = Mockito.mock(OAuth2AccessToken::class.java)
        Mockito.`when`(tokenStore.readAuthentication(fakeAccessToken.toString())).thenReturn(fakeAuthentication)

        webSocketProxyServerHandler.handleMessage(webSocketSession, TextMessage(fakeAccessToken.toString()))

        assertFalse(webSocketSession.isOpen)
    }

    @Test
    fun `test afterConnectionEstablished no messages happy path`() {

        val updates = Mockito.mock(Observable::class.java)
        Mockito.`when`(vaultDataFeed.updates).thenReturn(updates as Observable<Vault.Update<Account>>)

        webSocketProxyServerHandler.handleMessage(webSocketSession, TextMessage(oAuth2AccessToken.toString()))

        verify(webSocketSession, never()).sendMessage(any())
    }

    @Test
    fun `test afterConnectionEstablished message sent happy path`() {

        val accountId = UUID.randomUUID()
        val vaultUpdate = getVaultUpdate(accountId)
        val publisher = PublishSubject.create<Vault.Update<Account>>()
        val updates: Observable<Vault.Update<Account>> = uncheckedCast(publisher.bufferUntilSubscribed())
        publisher.onNext(vaultUpdate)

        Mockito.`when`(vaultDataFeed.updates).thenReturn(updates)

        webSocketProxyServerHandler.handleMessage(webSocketSession, TextMessage(oAuth2AccessToken.toString()))

        val argument = ArgumentCaptor.forClass(TextMessage::class.java)
        verify(webSocketSession).sendMessage(argument.capture())

        val message = objectMapper.readValue(argument.value.payload, WebSocketMessage::class.java)
        assertEquals("New account with accountId: $accountId created!", message.renderedMessage)
        assertEquals(MessageType.ACCOUNT_CREATED.messageTemplate, message.templateMessage)
        assertEquals("eng", message.isoLanguageCode)
        assertEquals(MessageType.ACCOUNT_CREATED, message.messageType)
        assertEquals(accountId, message.accountId)
        assertNull(message.propertyName)
        assertNull(message.newPropertyValue)
    }

    @Test
    fun `test afterConnectionEstablished message sent property changed happy path`() {

        val accountId = UUID.randomUUID()
        val vaultUpdate = getVaultUpdate(accountId, true)
        val publisher = PublishSubject.create<Vault.Update<Account>>()
        val updates: Observable<Vault.Update<Account>> = uncheckedCast(publisher.bufferUntilSubscribed())
        publisher.onNext(vaultUpdate)

        Mockito.`when`(vaultDataFeed.updates).thenReturn(updates)

        webSocketProxyServerHandler.handleMessage(webSocketSession, TextMessage(oAuth2AccessToken.toString()))

        val argument = ArgumentCaptor.forClass(TextMessage::class.java)
        verify(webSocketSession).sendMessage(argument.capture())

        val message = objectMapper.readValue(argument.value.payload, WebSocketMessage::class.java)
        assertEquals("Balance changed for account with id: $accountId, new Balance = 10.00 EUR", message.renderedMessage)
        assertEquals(MessageType.PROPERTY_CHANGED.messageTemplate, message.templateMessage)
        assertEquals("eng", message.isoLanguageCode)
        assertEquals(MessageType.PROPERTY_CHANGED, message.messageType)
        assertEquals(accountId, message.accountId)
        assertEquals("Balance", message.propertyName)
        assertEquals("10.00 EUR", message.newPropertyValue)
    }

    private fun getVaultUpdate(accountId: UUID, createConsumed: Boolean = false): Vault.Update<Account> {
        val bank = TestIdentity(CordaX500Name("bank", "Dublin", "IE"))
        val notary = TestIdentity(CordaX500Name("notary", "Dublin", "IE"))
        val id = UniqueIdentifier(id = accountId)
        val accountInfo = AccountInfo(accountId.toString(), bank.party, id)

        val accountData = AccountData(
                accountId = accountId,
                accountInfo = accountInfo,
                customerId = customerId,
                balance = 10 of EUR,
                txDate = Instant.now(),
                status = AccountStatus.ACTIVE)
        val currentAccountState = CurrentAccountState(accountData, null, null, linearId = UniqueIdentifier())

        val stateRef = StateRef(SecureHash.sha256("txId2"), 1)
        val transactionState = TransactionState(data = currentAccountState, notary = notary.party)
        val stateAndRef = StateAndRef(transactionState, stateRef)
        var consumedSet = mutableSetOf<StateAndRef<Account>>()

        if (createConsumed) {
            val stateRefConsumed = StateRef(SecureHash.sha256("txId1"), 1)
            val transactionStateConsumed = TransactionState(data = currentAccountState.copy(accountData = accountData.copy(balance = 5 of EUR)),
                    notary = notary.party)
            val stateAndRefConsumed = StateAndRef(transactionStateConsumed, stateRefConsumed)
            consumedSet.add(stateAndRefConsumed)
        }
        return Vault.Update(consumedSet, setOf(stateAndRef), null)
    }
}