package net.corda.p2p.gateway

import io.netty.handler.codec.http.HttpResponseStatus
import net.corda.p2p.gateway.messaging.ConnectionManager
import net.corda.p2p.gateway.messaging.GatewayConfiguration
import net.corda.p2p.gateway.messaging.http.DestinationInfo
import net.corda.p2p.gateway.messaging.http.HttpConnectionEvent
import net.corda.p2p.gateway.messaging.http.HttpEventListener
import net.corda.p2p.gateway.messaging.http.HttpMessage
import net.corda.p2p.gateway.messaging.http.HttpServer
import net.corda.p2p.gateway.messaging.http.ListenerWithServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.CountDownLatch

class ConnectionManagerTest : TestBase() {

    private val serverAddress = URI.create("http://localhost:10000")
    private val destination = DestinationInfo(serverAddress, aliceSNI[0], null)
    private val configuration = GatewayConfiguration(
        hostAddress = serverAddress.host,
        hostPort = serverAddress.port,
        sslConfig = aliceSslConfig
    )
    private val parent = createParentCoordinator()
    private val configService = createConfigurationServiceFor(configuration)
    private val configurationPair = getConfigPair()
    private val configReadService = configurationPair.first
    private val configWriter = configurationPair.second

    @Test
    @Timeout(30)
    fun `acquire connection`() {
        val responseReceived = CountDownLatch(1)
        val manager = ConnectionManager(
            configService,
            object : HttpEventListener {
                override fun onMessage(message: HttpMessage) {
                    assertEquals(serverResponseContent, String(message.payload))
                    responseReceived.countDown()
                }
            },
            lifecycleCoordinatorFactory
        )
        val listener = object : ListenerWithServer() {
            override fun onMessage(message: HttpMessage) {
                assertEquals(clientMessageContent, String(message.payload))
                server?.write(HttpResponseStatus.OK, serverResponseContent.toByteArray(), message.source)
            }
        }
        manager.startAndWaitForStarted()

        configWriter.publishConfig(configuration)
        HttpServer(listener, configReadService, lifecycleCoordinatorFactory).use { server ->
            listener.server = server
            server.startAndWaitForStarted()
            manager.acquire(destination).use { client ->
                client.write(clientMessageContent.toByteArray())
                responseReceived.await()
            }
        }
    }

    @Test
    @Timeout(30)
    fun `reuse connection`() {
        val manager = ConnectionManager(configService, object : HttpEventListener {}, lifecycleCoordinatorFactory)
        val remotePeers = mutableListOf<SocketAddress>()
        manager.startAndWaitForStarted()
        val requestReceived = CountDownLatch(2)
        val listener = object : HttpEventListener {
            override fun onOpen(event: HttpConnectionEvent) {
                remotePeers.add(event.channel.remoteAddress())
            }
            override fun onMessage(message: HttpMessage) {
                requestReceived.countDown()
            }
        }
        configWriter.publishConfig(configuration)
        HttpServer(listener, configReadService, lifecycleCoordinatorFactory).use { server ->
            server.startAndWaitForStarted()
            manager.acquire(destination).write(clientMessageContent.toByteArray())
            manager.acquire(destination).write(clientMessageContent.toByteArray())
            requestReceived.await()
            assertEquals(1, remotePeers.size)
            manager.acquire(destination).stop()
        }
    }

}
