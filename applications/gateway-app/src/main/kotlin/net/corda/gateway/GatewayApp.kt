package net.corda.gateway

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import java.io.File
import java.io.FileInputStream
import java.util.*
import net.corda.components.examples.config.reader.ConfigReader
import net.corda.libs.configuration.read.factory.ConfigReadServiceFactory
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.StartEvent
import net.corda.lifecycle.StopEvent
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.subscription.factory.SubscriptionFactory
import net.corda.osgi.api.Application
import net.corda.osgi.api.Shutdown
import net.corda.p2p.gateway.Gateway

import net.corda.v5.base.util.contextLogger
import org.osgi.framework.FrameworkUtil
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine

enum class LifeCycleState {
    UNINITIALIZED, STARTINGCONFIG, STARTINGMESSAGING, REINITMESSAGING
}

class ConfigReceivedEvent(val currentConfigurationSnapshot: Map<String, Config>) : LifecycleEvent
class ConfigUpdateEvent(val currentConfigurationSnapshot: Map<String, Config>) : LifecycleEvent

@Component(immediate = true)
class GatewayApp @Activate constructor(
    @Reference(service = SubscriptionFactory::class)
    private val subscriptionFactory: SubscriptionFactory,
    @Reference(service = PublisherFactory::class)
    private val publisherFactory: PublisherFactory,
    @Reference(service = Shutdown::class)
    private val shutDownService: Shutdown,
    @Reference(service = ConfigReadServiceFactory::class)
    private var configReadServiceFactory: ConfigReadServiceFactory
) : Application {

    private companion object {
        val logger = contextLogger()
        val consoleLogger: Logger = LoggerFactory.getLogger("Console")

        const val BATCH_SIZE: Int = 128
        const val TOPIC_PREFIX = "messaging.topic.prefix"
        const val CONFIG_TOPIC_NAME = "config.topic.name"
        const val BOOTSTRAP_SERVERS = "bootstrap.servers"
        const val KAFKA_COMMON_BOOTSTRAP_SERVER = "messaging.kafka.common.bootstrap.servers"
    }

    private var lifeCycleCoordinator: LifecycleCoordinator? = null

    @Suppress("SpreadOperator")
    override fun startup(args: Array<String>) {
        consoleLogger.info("Starting application")
        // Read cli args
        val parameters = AppParameters()
        CommandLine(parameters).parseArgs(*args)
        if (parameters.helpRequested) {
            CommandLine.usage(AppParameters(), System.out)
            shutDownService.shutdown(FrameworkUtil.getBundle(this::class.java))
        } else {
            var configReader: ConfigReader? = null
//            var gatewayInstance: Gateway? = null
            var state: LifeCycleState = LifeCycleState.UNINITIALIZED
            val kafkaProperties = getKafkaPropertiesFromFile(parameters.kafkaProperties)
            val bootstrapConfig = getBootstrapConfig(kafkaProperties)
            val configService = configReadServiceFactory.createReadService(bootstrapConfig)
            Gateway(configService)
            logger.info("Starting life cycle coordinator")
            lifeCycleCoordinator!!.start()
            consoleLogger.info("Application started")
        }

    }

    override fun shutdown() {
        consoleLogger.info("Stopping application")
        lifeCycleCoordinator?.stop()
        logger.info("Stopping application")
    }

    private fun getKafkaPropertiesFromFile(kafkaPropertiesFile: File?): Properties? {
        if (kafkaPropertiesFile == null) {
            return null
        }

        val kafkaConnectionProperties = Properties()
        kafkaConnectionProperties.load(FileInputStream(kafkaPropertiesFile))
        return kafkaConnectionProperties
    }

    private fun getBootstrapConfig(kafkaConnectionProperties: Properties?): Config {
        val bootstrapServer = getConfigValue(kafkaConnectionProperties, BOOTSTRAP_SERVERS)
        return ConfigFactory.empty()
            .withValue(KAFKA_COMMON_BOOTSTRAP_SERVER, ConfigValueFactory.fromAnyRef(bootstrapServer))
            .withValue(CONFIG_TOPIC_NAME, ConfigValueFactory.fromAnyRef(getConfigValue(kafkaConnectionProperties, CONFIG_TOPIC_NAME)))
            .withValue(TOPIC_PREFIX, ConfigValueFactory.fromAnyRef(getConfigValue(kafkaConnectionProperties, TOPIC_PREFIX, "")))
    }

    private fun getConfigValue(kafkaConnectionProperties: Properties?, path: String, default: String? = null): String {
        var configValue = System.getProperty(path)
        if (configValue == null && kafkaConnectionProperties != null) {
            configValue = kafkaConnectionProperties[path].toString()
        }

        if (configValue == null) {
            if (default != null) {
                return default
            }
            logger.error(
                "No $path property found! " +
                        "Pass property in via --kafka properties file or via -D$path"
            )
            shutdown()
        }
        return configValue
    }

//    private fun Config.parse(): GatewayConfiguration {
//        val aliceSslConfig = object : SslConfiguration {
//            override val keyStore: KeyStore = KeyStore.getInstance("JKS").also {
//                it.load(FileInputStream(javaClass.classLoader.getResource("sslkeystore_alice.jks")!!.file), "password".toCharArray())
//            }
//            override val keyStorePassword: String = "password"
//            override val trustStore: KeyStore = KeyStore.getInstance("JKS").also {
//                it.load(FileInputStream(javaClass.classLoader.getResource("truststore.jks")!!.file), "password".toCharArray())
//            }
//            override val trustStorePassword: String = "password"
//            override val revocationCheck = RevocationConfigImpl(RevocationConfig.Mode.OFF)
//        }
//        return GatewayConfiguration("www.alice.net", 10000, aliceSslConfig)
//    }
}