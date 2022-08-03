package net.corda.components.test.internal.impl

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.ws
import io.javalin.core.security.RouteRole
import io.javalin.websocket.WsConfig
import net.corda.components.test.internal.JavalinServerFactory
import org.osgi.service.component.annotations.Component

@Component(service = [JavalinServerFactory::class])
class JavalinServerFactoryImpl : JavalinServerFactory {
    override fun createJavalinServer(
        handler: (ws: WsConfig) -> RouteRole
    ): Javalin = Javalin.create { config ->
        config.enableCorsForAllOrigins()
        config.asyncRequestTimeout = 10_000L
        config.enforceSsl = false
    }.routes {
        ws("/") { handler(it) }
    }
}
