package net.corda.p2p.gateway

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigValueFactory
import java.io.FileInputStream
import net.corda.lifecycle.Lifecycle
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.subscription.Subscription
import net.corda.messaging.api.subscription.factory.SubscriptionFactory
import net.corda.messaging.api.subscription.factory.config.SubscriptionConfig
import net.corda.p2p.LinkOutMessage
import net.corda.p2p.gateway.messaging.ConnectionManager
import net.corda.p2p.gateway.messaging.http.HttpServer
import net.corda.p2p.gateway.messaging.internal.InboundMessageHandler
import net.corda.p2p.gateway.messaging.internal.OutboundMessageHandler
import net.corda.p2p.gateway.messaging.internal.PartitionAssignmentListenerImpl
import net.corda.p2p.gateway.messaging.session.SessionPartitionMapperImpl
import net.corda.p2p.schema.Schema.Companion.LINK_OUT_TOPIC
import org.osgi.service.component.annotations.Reference
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.security.KeyStore
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import net.corda.components.examples.config.reader.ConfigReader
import net.corda.libs.configuration.read.ConfigListener
import net.corda.libs.configuration.read.ConfigReadService
import net.corda.libs.configuration.read.factory.ConfigReadServiceFactory
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.StartEvent
import net.corda.lifecycle.StopEvent
import net.corda.p2p.gateway.messaging.GatewayConfiguration
import net.corda.p2p.gateway.messaging.RevocationConfig
import net.corda.p2p.gateway.messaging.RevocationConfigImpl
import net.corda.p2p.gateway.messaging.SslConfiguration
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component

/**
 * The Gateway is a light component which facilitates the sending and receiving of P2P messages.
 * Upon connecting to the internal messaging system, the Gateway will subscribe to the different topics for outgoing messages.
 * Each such message will trigger the creation or retrieval of a persistent HTTP connection to the target (specified in the
 * message header).
 *
 * The messaging relies on shallow POST requests, meaning the serving Gateway will send a response back immediately after
 * receipt of the request. Once e response arrives, it is inspected for any server side errors and, if needed, published
 * to the internal messaging system.
 *
 */

@Component
class Gateway @Activate constructor(
    @Reference(service = ConfigReadService::class)
    private val configService: ConfigReadService,
    @Reference(service = SubscriptionFactory::class)
    private val subscriptionFactory: SubscriptionFactory,
    @Reference(service = PublisherFactory::class)
    private val publisherFactory: PublisherFactory
) : Lifecycle {

    companion object {
        private val logger = LoggerFactory.getLogger(Gateway::class.java)
        const val CONSUMER_GROUP_ID = "gateway"
        const val PUBLISHER_ID = "gateway"
    }

    // The idea is to use the lifecycleCoordinator in combination with the configReadServiceFactory to have the Gateway
    // start-up in a suspended state in which it waits for config to arrive through Kafka.
    // On new Config, the sub components are initialised and started
    // On Config update, the affected components should be restarted
    // Hotloading should be seamless. There's the issue of handling established connections and in-flight messages
    private var lifecycleCoordinator: LifecycleCoordinator? = null
    private val closeActions = mutableListOf<() -> Unit>()
    private var httpServer: HttpServer? = null
    private var connectionManager: ConnectionManager? = null
    private var p2pMessageSubscription: Subscription<String, LinkOutMessage>? = null
    private var sessionPartitionMapper = SessionPartitionMapperImpl(subscriptionFactory)
    private var inboundMessageProcessor: InboundMessageHandler? = null
    private var outboundMessageProcessor: OutboundMessageHandler? = null
    private var configListener: AutoCloseable? = null
    private val lock = ReentrantLock()

    @Volatile
    private var started = false

    override val isRunning: Boolean
        get() = started

    override fun start() {
        lock.withLock {
            if (started) {
                logger.info("Already started")
                return
            }
            logger.info("Starting Gateway service")
            started = true
//            // For the time being we're using the example component for a configuration reader. To be replaced with
//            // the kafka-config-read when it's finished. Ths config read component should also be created by the
//            // standalone app using the CLI args to set-up the kafka connectivity params.
//            // For now we use some dummy properties
//            val kafkaProperties = Properties()
//            val bootstrapConfig = getBootstrapConfig(kafkaProperties)
//            var configReader: ConfigReader? = null
//            // This coordinator is used for config only. There's uncertainty around how to use it for other sub components
//            // or if there should be one coordinator for each and form a dependency chain somehow
//            lifecycleCoordinator = LifecycleCoordinatorFactory.createCoordinator<Gateway>(128) { event, _ ->
//                logger.info("Lifecycle event received: $event")
//                when (event) {
//                    is StartEvent -> {
//                        logger.info("Starting Kafka config reader")
//                        configReader?.start()
//                    }
//                    is StopEvent -> {
//                        configReader?.stop()
//                    }
//                    is ConfigReceivedEvent -> {
//                        event.currentConfigurationSnapshot["corda.gateway"]?.let {
//                            initServices(it)
//                        }
//                    }
//                    is ConfigUpdateEvent -> {
//                        val config = event.currentConfigurationSnapshot["corda.gateway"]
//                        // Restart affected components
//                    }
//                    else -> logger.error("Unexpected event received: $event")
//                }
//            }
//
//            configReader = ConfigReader(lifecycleCoordinator!!, configReadServiceFactory)
            val listener = ConfigListener { changedKeys: Set<String>, currentConfigurationSnapshot: Map<String, Config> ->
                // Need to check if it's the first config or an update
                if (changedKeys.contains("corda.gateway")) {
                    val gatewayConfig = currentConfigurationSnapshot["corda.gateway"]!!.parse()
                    initServices(gatewayConfig)
                }
            }
            configListener = configService.registerCallback(listener)
            // if this is created and managed by the parent component, then do we still need to call start?
            // what if the parent is an app?
            configService.start()
            closeActions += { configService.stop() }
            logger.info("Gateway started")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun stop() {
        lock.withLock {
            logger.info("Shutting down")
            started = false
            for (closeAction in closeActions.reversed()) {
                try {
                    closeAction()
                } catch (e: InterruptedException) {
                    logger.warn("InterruptedException was thrown during shutdown, ignoring.")
                } catch (e: Exception) {
                    logger.warn("Exception thrown during shutdown.", e)
                }
            }

            logger.info("Shutdown complete")
        }
    }

//    private fun getBootstrapConfig(kafkaConnectionProperties: Properties?): Config {
//        return ConfigFactory.empty()
//    }

    private fun initServices(config: GatewayConfiguration) {
        connectionManager = ConnectionManager(config.sslConfig, config.connectionConfig)
        connectionManager!!.start()
        closeActions += { connectionManager?.close() }
        httpServer = HttpServer(config.hostAddress, config.hostPort, config.sslConfig)
        httpServer!!.start()
        closeActions += { httpServer!!.close() }
        sessionPartitionMapper.start()
        closeActions += { sessionPartitionMapper.close() }
        inboundMessageProcessor = InboundMessageHandler(httpServer!!, config.maxMessageSize, publisherFactory, sessionPartitionMapper)
        inboundMessageProcessor!!.start()
        closeActions += { inboundMessageProcessor!!.close() }
        outboundMessageProcessor = OutboundMessageHandler(connectionManager!!, publisherFactory)
        outboundMessageProcessor!!.start()
        closeActions += { outboundMessageProcessor!!.close() }
        val subscriptionConfig = SubscriptionConfig(CONSUMER_GROUP_ID, LINK_OUT_TOPIC)
        p2pMessageSubscription = subscriptionFactory.createEventLogSubscription(subscriptionConfig,
            outboundMessageProcessor!!,
            ConfigFactory.empty(),
            PartitionAssignmentListenerImpl())
        p2pMessageSubscription!!.start()
        closeActions += { p2pMessageSubscription!!.close() }
    }

    private fun Config.parse(): GatewayConfiguration {
        val aliceSslConfig = object : SslConfiguration {
            override val keyStore: KeyStore = KeyStore.getInstance("JKS").also {
                it.load(FileInputStream(javaClass.classLoader.getResource("sslkeystore_alice.jks")!!.file), "password".toCharArray())
            }
            override val keyStorePassword: String = "password"
            override val trustStore: KeyStore = KeyStore.getInstance("JKS").also {
                it.load(FileInputStream(javaClass.classLoader.getResource("truststore.jks")!!.file), "password".toCharArray())
            }
            override val trustStorePassword: String = "password"
            override val revocationCheck = RevocationConfigImpl(RevocationConfig.Mode.OFF)
        }
        return GatewayConfiguration("www.alice.net", 10000, aliceSslConfig)
    }
}