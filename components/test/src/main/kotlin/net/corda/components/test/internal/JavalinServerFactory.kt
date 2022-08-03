package net.corda.components.test.internal

import io.javalin.Javalin
import io.javalin.core.security.RouteRole
import io.javalin.websocket.WsConfig

interface JavalinServerFactory {
    fun createJavalinServer(handler: (ws: WsConfig) -> RouteRole): Javalin
}
