package com.r3.refapp.client

import com.fasterxml.jackson.databind.module.SimpleModule
import com.r3.refapp.client.serializer.AccountDataSerializer
import com.r3.refapp.client.serializer.AccountSerializer
import com.r3.refapp.states.Account
import com.r3.refapp.states.AccountData
import net.corda.client.jackson.JacksonSupport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.web.socket.config.annotation.EnableWebSocket

/**
 * Spring Boot application that provides rest endpoints for refapp BankInABox application.
 * [EnableAuthorizationServer] annotation enables OAuth2 server features like access token generation, verification,
 * access token rotation based on refresh token .etc.
 * [EnableResourceServer] annotation enables a Spring Security filter that authenticates requests via an incoming
 * OAuth2 token, configuration needs to be provided via [ResourceServerConfigurerAdapter] bean.
 */
@SpringBootApplication
@EnableWebSocket
open class Server {
    /**
     * Provides default object to json converter
     * @param rpcConnection autowired connection to corda node
     * @return [MappingJackson2HttpMessageConverter] converter
     */
    @Bean
    open fun mappingJackson2HttpMessageConverter(@Autowired rpcConnection: NodeRPCConnection): MappingJackson2HttpMessageConverter {
        val mapper = JacksonSupport.createDefaultMapper(rpcConnection.proxy)
        val module = SimpleModule()
        module.addSerializer(AccountData::class.java, AccountDataSerializer())
        module.addSerializer(Account::class.java, AccountSerializer())
        mapper.registerModule(module)
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }
}

/**
 * Start Spring Boot application for refapp webserver.
 */
fun main(args: Array<String>) {
    val app = SpringApplication(Server::class.java)
    app.setBannerMode(Banner.Mode.OFF)
    app.webApplicationType = SERVLET
    app.run(*args)
}
