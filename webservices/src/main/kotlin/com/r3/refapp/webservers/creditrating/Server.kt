package com.r3.refapp.webservers.creditrating

import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Credit Rating Simulating Spring Boot application.
 */
@SpringBootApplication
open class Server {
    // Spring boot annotated class needed to startup a spring application
}

/**
 * Start Spring Boot application.
 */
fun main(args: Array<String>) {
    val app = SpringApplication(Server::class.java)
    app.setBannerMode(Banner.Mode.OFF)
    app.webApplicationType = SERVLET
    app.run(*args)
}
