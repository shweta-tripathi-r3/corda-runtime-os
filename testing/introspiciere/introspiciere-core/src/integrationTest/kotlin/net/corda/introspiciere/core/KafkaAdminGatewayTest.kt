package net.corda.introspiciere.core

import net.corda.introspiciere.domain.TopicDefinition
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.common.config.TopicConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

internal class KafkaAdminGatewayTest {
    @Test
    fun `i can create a topic`() {
        val gateway: KafkaAdminGateway = KafkaAdminGatewayImpl(object : KafkaConfig {
            override val brokers: String = "20.62.51.171:9094"
        })

        val definition = TopicDefinition(
            name = "topic".random8,
            partitions = 3,
            replicationFactor = 2,
            config = mapOf(
                TopicConfig.CLEANUP_POLICY_CONFIG to "compact",
                TopicConfig.SEGMENT_MS_CONFIG to 300_000.toString(),
            )
        )
        gateway.createTopic(definition)

        val future = ThreadLocal<Admin>().get().describeTopics(listOf(definition.name))

        val description = future.all().get()[definition.name]
        assertNotNull(description, "Topic exists")
        assertEquals(definition.name, description!!.name())
        assertEquals(definition.partitions, description.partitions().size)
        assertEquals(definition.replicationFactor, description.partitions().first().replicas().size)
    }
}

private val charPool: List<Char> = ('a'..'z') + ('0'..'9')

internal val String.random8: String
    get() = "$this-" + ThreadLocalRandom.current().ints(8L, 0, charPool.size)
        .asSequence().map(charPool::get).joinToString("")