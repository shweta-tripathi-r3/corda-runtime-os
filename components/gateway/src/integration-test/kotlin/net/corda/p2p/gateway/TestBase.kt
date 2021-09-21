package net.corda.p2p.gateway

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import net.corda.configuration.read.ConfigurationReadService
import net.corda.configuration.read.impl.ConfigurationReadServiceImpl
import net.corda.libs.configuration.read.kafka.factory.ConfigReadServiceFactoryImpl
import net.corda.libs.configuration.write.CordaConfigurationKey
import net.corda.libs.configuration.write.CordaConfigurationVersion
import net.corda.libs.configuration.write.kafka.ConfigWriteServiceImpl
import net.corda.lifecycle.impl.LifecycleCoordinatorFactoryImpl
import net.corda.messaging.api.publisher.config.PublisherConfig
import net.corda.messaging.emulation.publisher.factory.CordaPublisherFactory
import net.corda.messaging.emulation.subscription.factory.InMemSubscriptionFactory
import net.corda.messaging.emulation.topic.service.impl.TopicServiceImpl
import net.corda.p2p.NetworkType
import net.corda.p2p.gateway.domino.LifecycleWithCoordinator
import net.corda.p2p.gateway.domino.LifecycleWithCoordinatorAndResources
import net.corda.p2p.gateway.messaging.GatewayConfiguration
import net.corda.p2p.gateway.messaging.RevocationConfig
import net.corda.p2p.gateway.messaging.RevocationConfigMode
import net.corda.p2p.gateway.messaging.SslConfiguration
import net.corda.p2p.gateway.messaging.http.SniCalculator
import org.bouncycastle.asn1.x500.X500Name
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.security.KeyStore
import java.util.Base64
import java.util.UUID

open class TestBase {
    private fun readKeyStore(fileName: String, password: String): KeyStore {
        return KeyStore.getInstance("JKS").also {
            it.load(FileInputStream(javaClass.classLoader.getResource("$fileName.jks")!!.file), password.toCharArray())
        }
    }
    private fun saveKeyStore(keyStore: KeyStore, password: String): String {
        return ByteArrayOutputStream().use {
            keyStore.store(it, password.toCharArray())
            Base64.getEncoder().encodeToString(it.toByteArray())
        }
    }

    protected val clientMessageContent = "PING"
    protected val serverResponseContent = "PONG"
    private val keystorePass = "password"
    private val truststorePass = "password"
    private val keystorePass_c4 = "cordacadevpass"
    private val truststorePass_c4 = "trustpass"
    protected val aliceSNI = listOf("alice.net", "www.alice.net")
    protected val bobSNI = listOf("bob.net", "www.bob.net")
    protected val partyAx500Name = X500Name("O=PartyA, L=London, C=GB")
    protected val partyASNI = SniCalculator.calculateSni("O=PartyA, L=London, C=GB", NetworkType.CORDA_4, "")
    protected val aliceSslConfig = SslConfiguration(
        keyStorePassword = keystorePass,
        keyStore = readKeyStore("sslkeystore_alice", keystorePass),
        trustStorePassword = truststorePass,
        trustStore = readKeyStore("truststore", truststorePass),
        revocationCheck = RevocationConfig(RevocationConfigMode.OFF)

    )
    protected val bobSslConfig = SslConfiguration(
        keyStorePassword = keystorePass,
        keyStore = readKeyStore("sslkeystore_bob", keystorePass),
        trustStorePassword = truststorePass,
        trustStore = readKeyStore("truststore", truststorePass),
        revocationCheck = RevocationConfig(RevocationConfigMode.HARD_FAIL)

    )
    protected val chipSslConfig = SslConfiguration(
        keyStorePassword = keystorePass,
        keyStore = readKeyStore("sslkeystore_chip", keystorePass),
        trustStorePassword = truststorePass,
        trustStore = readKeyStore("truststore", truststorePass),
        revocationCheck = RevocationConfig(RevocationConfigMode.HARD_FAIL)

    )
    protected val daleSslConfig = SslConfiguration(
        keyStorePassword = keystorePass,
        keyStore = readKeyStore("sslkeystore_dale", keystorePass),
        trustStorePassword = truststorePass,
        trustStore = readKeyStore("truststore", truststorePass),
        revocationCheck = RevocationConfig(RevocationConfigMode.SOFT_FAIL)

    )
    protected val c4sslConfig = SslConfiguration(
        keyStorePassword = keystorePass_c4,
        keyStore = readKeyStore("sslkeystore_c4", keystorePass_c4),
        trustStorePassword = truststorePass_c4,
        trustStore = readKeyStore("truststore_c4", truststorePass_c4),
        revocationCheck = RevocationConfig(RevocationConfigMode.OFF)
    )

    private val lifecycleCoordinatorFactory = LifecycleCoordinatorFactoryImpl()

    protected fun createConfigurationServiceFor(configuration: GatewayConfiguration): ConfigurationReadService {
        val configurationTopicService = TopicServiceImpl()
        val topicName = "config.${UUID.randomUUID().toString().replace("-", "")}"
        val publishConfig = ConfigFactory.empty()
            .withValue("hostAddress", ConfigValueFactory.fromAnyRef(configuration.hostAddress))
            .withValue("hostPort", ConfigValueFactory.fromAnyRef(configuration.hostPort))
            .withValue("traceLogging", ConfigValueFactory.fromAnyRef(configuration.traceLogging))
            .withValue("sslConfig.keyStorePassword", ConfigValueFactory.fromAnyRef(configuration.sslConfig.keyStorePassword))
            .withValue("sslConfig.keyStore", ConfigValueFactory.fromAnyRef(saveKeyStore(configuration.sslConfig.keyStore, configuration.sslConfig.keyStorePassword)))
            .withValue("sslConfig.trustStorePassword", ConfigValueFactory.fromAnyRef(configuration.sslConfig.trustStorePassword))
            .withValue("sslConfig.trustStore", ConfigValueFactory.fromAnyRef(saveKeyStore(configuration.sslConfig.trustStore, configuration.sslConfig.trustStorePassword)))
            .withValue("sslConfig.revocationCheck.mode", ConfigValueFactory.fromAnyRef(configuration.sslConfig.revocationCheck.mode.toString()))
            .withValue("connectionConfig.connectionIdleTimeout", ConfigValueFactory.fromAnyRef(configuration.connectionConfig.connectionIdleTimeout))
            .withValue("connectionConfig.maxClientConnections", ConfigValueFactory.fromAnyRef(configuration.connectionConfig.maxClientConnections))
            .withValue("connectionConfig.acquireTimeout", ConfigValueFactory.fromAnyRef(configuration.connectionConfig.acquireTimeout))
            .withValue("connectionConfig.responseTimeout", ConfigValueFactory.fromAnyRef(configuration.connectionConfig.responseTimeout))
            .withValue("connectionConfig.retryDelay", ConfigValueFactory.fromAnyRef(configuration.connectionConfig.retryDelay))

        val publisher = CordaPublisherFactory(configurationTopicService).createPublisher(PublisherConfig((topicName)))
        val configurationPublisher = ConfigWriteServiceImpl(topicName, publisher)
        configurationPublisher.updateConfiguration(
            CordaConfigurationKey(
                "myKey",
                CordaConfigurationVersion("p2p", 0, 1),
                CordaConfigurationVersion("gateway", 0, 1)
            ),
            publishConfig
        )
        val bootstrapper = ConfigFactory.empty()
            .withValue("config.topic.name", ConfigValueFactory.fromAnyRef(topicName))
        return ConfigurationReadServiceImpl(
            lifecycleCoordinatorFactory,
            ConfigReadServiceFactoryImpl(InMemSubscriptionFactory(configurationTopicService)),
        ).also {
            it.start()
            it.bootstrapConfig(bootstrapper)
        }
    }

    fun createParentCoordinator(): LifecycleWithCoordinator {
        return object : LifecycleWithCoordinatorAndResources(lifecycleCoordinatorFactory, UUID.randomUUID().toString()) {
            override fun onStart() {
            }
        }
    }

    fun createGatewayConfigService(configuration: GatewayConfiguration): GatewayConfigurationService {
        return GatewayConfigurationService(createParentCoordinator(), createConfigurationServiceFor(configuration))
    }
}
