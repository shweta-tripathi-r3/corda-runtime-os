package net.corda.p2p.gateway.messaging

import com.typesafe.config.Config
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import net.corda.configuration.read.ConfigurationHandler
import net.corda.configuration.read.ConfigurationReadService
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.p2p.gateway.GatewayConfigurationService
import net.corda.p2p.gateway.domino.DominoLifecycle
import net.corda.p2p.gateway.domino.DominoLifecycle.*
import net.corda.p2p.gateway.domino.LeafDominoLifecycle
import net.corda.p2p.gateway.domino.LifecycleWithCoordinator
import net.corda.p2p.gateway.messaging.http.DestinationInfo
import net.corda.p2p.gateway.messaging.http.HttpClient
import net.corda.p2p.gateway.messaging.http.HttpEventListener
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import java.net.URI
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * The [ConnectionManager] is responsible for creating an HTTP connection and caching it. If a connection to the requested
 * target already exists, it's reused. There will be a maximum 100 connections allowed at any given time. Any new requests
 * will block until resources become available.
 *
 * To ensure we don't block indefinitely, several timeouts will be used to determine when to close an inactive connection
 * or to drop a request for one.
 *
 */
class ConnectionManager(
    private val configurationReaderService: ConfigurationReadService,
    private val listener: HttpEventListener,
    private val lifecycleCoordinatorFactory: LifecycleCoordinatorFactory
) : LeafDominoLifecycle(lifecycleCoordinatorFactory),
    ConfigurationHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(ConnectionManager::class.java)

        private const val NUM_CLIENT_WRITE_THREADS = 2
        private const val NUM_CLIENT_NETTY_THREADS = 2
    }

    private var gatewayConfiguration: GatewayConfiguration? = null
    private var configRegistration: AutoCloseable? = null

    private val clientPool = ConcurrentHashMap<URI, HttpClient>()
    private var writeGroup: EventLoopGroup? = null
    private var nettyGroup: EventLoopGroup? = null

    override fun startSequence() {
        if (state != State.Started && gatewayConfiguration != null) {
            startResources()
        }

        if (configRegistration == null) {
            configRegistration = configurationReaderService.registerForUpdates(this)
        }
    }

    override fun stopSequence() {
        cleanUpConnectionPool()
    }

    private fun restart() {
        cleanUpConnectionPool()
        startResources()
    }

    private fun cleanUpConnectionPool() {
        val oldClients = clientPool.toMap()
        clientPool.clear()
        oldClients.values.forEach {
            it.close()
        }
    }

    private fun startResources() {
        writeGroup = NioEventLoopGroup(NUM_CLIENT_WRITE_THREADS)
        nettyGroup = NioEventLoopGroup(NUM_CLIENT_NETTY_THREADS)
        hasStarted()
    }

    /**
     * Return an existing or new [HttpClient].
     * @param destinationInfo the [DestinationInfo] object containing the destination's URI, SNI, and legal name
     */
    fun acquire(destinationInfo: DestinationInfo): HttpClient {
        val gatewayConfiguration = gatewayConfiguration ?: throw RuntimeException("Component is not configured yet.")

        return clientPool.computeIfAbsent(destinationInfo.uri) {
            val client = HttpClient(
                destinationInfo,
                gatewayConfiguration.sslConfig,
                writeGroup!!,
                nettyGroup!!,
                listener
            )
            client.start()
            client
        }
    }

    override fun onNewConfiguration(changedKeys: Set<String>, config: Map<String, Config>) {
        // TODO: check for changes needs to be more granular (i.e. changes on host/port are not relevant here).
        if (config.containsKey("p2p.gateway") && changedKeys.contains("p2p.gateway")) {
            val configPath = config["p2p.gateway"]!!
            val keyStore = Base64.getDecoder().decode(configPath.getString("sslConfig.keyStore"))
            val keyStorePassword = configPath.getString("sslConfig.keyStorePassword")
            val trustStore = Base64.getDecoder().decode(configPath.getString("sslConfig.trustStore"))
            val truststorePassword = configPath.getString("sslConfig.trustStorePassword")
            val revocationConfigMode = configPath.getEnum(RevocationConfigMode::class.java, "sslConfig.revocationCheck.mode")
            val revocationConfig = RevocationConfig(revocationConfigMode)
            val sslConfiguration = SslConfiguration(keyStore, keyStorePassword, trustStore, truststorePassword, revocationConfig)
            gatewayConfiguration = GatewayConfiguration(
                configPath.getString("hostAddress"),
                configPath.getInt("hostPort"),
                sslConfiguration
            )

            if (state != State.StoppedByParent) {
                restart()
            }
        }
    }

}
