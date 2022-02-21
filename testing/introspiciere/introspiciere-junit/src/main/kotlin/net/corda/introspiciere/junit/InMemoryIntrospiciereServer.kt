package net.corda.introspiciere.junit

import net.corda.introspiciere.server.IntrospiciereServer
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class InMemoryIntrospiciereServer(
    port: Int = 0,
    kafkaBrokers: List<String>? = null,
) : BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    override fun beforeEach(context: ExtensionContext?) = startServer()
    override fun beforeAll(context: ExtensionContext?) = startServer()
    override fun afterEach(context: ExtensionContext?) = stopServer()
    override fun afterAll(context: ExtensionContext?) = stopServer()

    private val server = IntrospiciereServer(port, kafkaBrokers)

    private fun startServer() {
        server.start()
    }

    private fun stopServer() {
        server.close()
    }

    /**
     * Endpoint where Introspiciere is listening.
     */
    val endpoint: String
        get() = "http://localhost:${server.portUsed}"

    /**
     * Introspiciere client.
     */
    val client: IntrospiciereClient
        get() = IntrospiciereClient(endpoint)
}

