package net.corda.messagebus.kafka.config

import com.typesafe.config.ConfigFactory
import net.corda.libs.configuration.SmartConfig
import net.corda.libs.configuration.SmartConfigFactory
import net.corda.messagebus.api.configuration.ConsumerConfig
import net.corda.messagebus.api.configuration.ProducerConfig
import net.corda.messaging.api.exception.CordaMessageAPIConfigException
import net.corda.schema.configuration.MessagingKeys.Bus.BUS_TYPE
import net.corda.schema.configuration.MessagingKeys.Bus.KAFKA_PROPERTIES
import net.corda.v5.base.util.contextLogger
import org.osgi.framework.Bundle
import org.osgi.framework.FrameworkUtil
import java.util.Properties

/**
 * Resolve a Kafka bus configuration against the enforced and default configurations provided by the library.
 */
internal class ConfigResolver(private val smartConfigFactory: SmartConfigFactory) {

    private companion object {
        private val logger = contextLogger()

        private const val ENFORCED_CONFIG_FILE = "messaging-enforced.conf"
        private const val DEFAULT_CONFIG_FILE = "messaging-defaults.conf"

        private const val EXPECTED_BUS_TYPE = "KAFKA"

        private const val GROUP_PATH = "group"
        private const val CLIENT_ID_PATH = "clientId"
        private const val TOPIC_PREFIX_PATH = "topicPrefix"
        private const val INSTANCE_ID_PATH = "instanceId"
    }

    private val defaults = getResourceConfig(DEFAULT_CONFIG_FILE)
    private val enforced = getResourceConfig(ENFORCED_CONFIG_FILE)

    /**
     * Resolve the provided configuration and return a valid set of Kafka properties suitable for the given role.
     *
     * @param busConfig The supplied message bus configuration. Must match the schema used in the defaults and enforced
     *               config files included with this library.
     * @param rolePath The role to be configured. This is a path representing the object type being created at the patterns
     *             layer and a description of which consumer or producer is requested.
     * @param configParams A config object containing parameters to resolve against. Should be obtained from the
     *                     required configuration provided to the builders.
     */
    private fun resolve(busConfig: SmartConfig, rolePath: String, configParams: SmartConfig): Properties {
        // TODO fix paths here. Does the returned config when you do getConfig have the path stripped?
        if (busConfig.getString(BUS_TYPE) != EXPECTED_BUS_TYPE) {
            throw CordaMessageAPIConfigException("foo")
        }

        val kafkaParams = busConfig.getConfig(KAFKA_PROPERTIES)
        val resolvedConfig = enforced
            .withFallback(kafkaParams)
            .withFallback(configParams)
            .withFallback(defaults)
            .resolve()

        logger.info("Resolved kafka configuration: ${resolvedConfig.root().render()}")

        // Trim down to just the Kafka config for the specified role.
        val roleConfig = resolvedConfig.getConfig("roles.$rolePath")
        val properties = roleConfig.toKafkaProperties()
        logger.info("Kafka properties for role $rolePath: $properties")
        return properties
    }

    fun resolve(busConfig: SmartConfig, consumerConfig: ConsumerConfig): Properties {
        return resolve(busConfig, consumerConfig.role.configPath, consumerConfig.toSmartConfig())
    }

    fun resolve(busConfig: SmartConfig, producerConfig: ProducerConfig): Properties {
        return resolve(busConfig, producerConfig.role.configPath, producerConfig.toSmartConfig())
    }

    /**
     * Retrieve a resource from this bundle and convert it to a SmartConfig object.
     *
     * If this is running outside OSGi (e.g. a unit test) then fall back to standard Java classloader mechanisms.
     */
    private fun getResourceConfig(resource: String): SmartConfig {
        val bundle: Bundle? = FrameworkUtil.getBundle(this::class.java)
        val url = bundle?.getResource(resource)
            ?: this::class.java.classLoader.getResource(resource) ?: throw CordaMessageAPIConfigException("foo") // TODO
        val config = ConfigFactory.parseURL(url)
        return smartConfigFactory.create(config)
    }

    private fun SmartConfig.toKafkaProperties(): Properties {
        val properties = Properties()
        for ((key, _) in this.entrySet()) {
            properties.setProperty(key, this.getString(key))
        }
        return properties
    }

    // All parameters in the enforced and default config files must be specified. These functions insert dummy values
    // for those parameters that don't matter when resolving the config.
    private fun ConsumerConfig.toSmartConfig(): SmartConfig {
        return smartConfigFactory.create(ConfigFactory.parseMap(mapOf(
            GROUP_PATH to group,
            CLIENT_ID_PATH to clientId,
            TOPIC_PREFIX_PATH to topicPrefix,
            INSTANCE_ID_PATH to "<undefined>"
        )))
    }

    private fun ProducerConfig.toSmartConfig(): SmartConfig {
        return smartConfigFactory.create(ConfigFactory.parseMap(mapOf(
            CLIENT_ID_PATH to clientId,
            INSTANCE_ID_PATH to instanceId,
            GROUP_PATH to "<undefined>"
        )))
    }
}