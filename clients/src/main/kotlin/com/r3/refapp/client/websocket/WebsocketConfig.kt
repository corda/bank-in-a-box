package com.r3.refapp.client.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.client.auth.ResourceServerConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.stereotype.Component
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * Spring managed bean which configures websocket handlers. Websocket endpoint will be available on pre-configured
 * [webSocketPaths], CORS allowed origins will be configured via [corsAllowedOrigins] same as for the rest of the
 * application. Configured handler is responsible for accepting incoming websocket sessions.
 */
@Component
open class WebsocketConfig(private val tokenStore: TokenStore, private val rpc: NodeRPCConnection, private val objectMapper: ObjectMapper):
        WebSocketConfigurer {

    /**
     * Configurable list of websocket paths
     */
    @Value("#{'\${auth.server.websocket-paths}'.split(',')}")
    lateinit var webSocketPaths: List<String>

    /**
     * Configurable CORS allowed origins
     */
    @Value("\${auth.server.cors.allowed-origins}")
    lateinit var corsAllowedOrigins: List<String>

    /**
     * Overrides Spring's [WebSocketConfigurer] interface function in order to configure handler responsible for
     * handling incoming websocket sessions, standard security features configured in [ResourceServerConfiguration] will
     * also be applied to websocket endpoint.
     * @param registry Spring's [WebSocketHandlerRegistry]
     */
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(WebSocketProxyServerHandler(tokenStore, rpc, objectMapper), *webSocketPaths.toTypedArray())
                .setAllowedOrigins(*corsAllowedOrigins.toTypedArray())
    }
}