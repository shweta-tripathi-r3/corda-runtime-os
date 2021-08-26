package net.corda.messaging.kafka.subscription

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import net.corda.messaging.api.exception.CordaMessageAPIIntermittentException
import net.corda.messaging.api.subscription.listener.StateAndEventListener
import net.corda.messaging.kafka.producer.wrapper.CordaKafkaProducer
import net.corda.messaging.kafka.properties.KafkaProperties
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.PATTERN_STATEANDEVENT
import net.corda.messaging.kafka.subscription.consumer.builder.StateAndEventBuilder
import net.corda.messaging.kafka.subscription.consumer.wrapper.ConsumerRecordAndMeta
import net.corda.messaging.kafka.subscription.consumer.wrapper.CordaKafkaConsumer
import net.corda.messaging.kafka.subscription.factory.SubscriptionMapFactory
import net.corda.messaging.kafka.subscription.net.corda.messaging.kafka.TOPIC_PREFIX
import net.corda.messaging.kafka.subscription.net.corda.messaging.kafka.createStandardTestConfig
import net.corda.messaging.kafka.subscription.net.corda.messaging.kafka.stubs.StubStateAndEventProcessor
import net.corda.schema.registry.AvroSchemaRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

class KafkaStateAndEventSubscriptionImplTest {

    companion object {
        private const val TEST_TIMEOUT_SECONDS = 20L
        private const val TOPIC = "topic"
        private const val DLQ = ".DLQ"
    }

    private val config: Config = createStandardTestConfig().getConfig(PATTERN_STATEANDEVENT)

    val map = ConcurrentHashMap<String, Pair<Long, String>>()
    private val subscriptionMapFactory = object : SubscriptionMapFactory<String, Pair<Long, String>> {
        override fun createMap(): MutableMap<String, Pair<Long, String>> = map
        override fun destroyMap(map: MutableMap<String, Pair<Long, String>>) {}
    }

    data class Mocks(
        val builder: StateAndEventBuilder<String, String, ByteBuffer>,
        val producer: CordaKafkaProducer,
        val eventConsumer: CordaKafkaConsumer<String, ByteBuffer>,
        val stateConsumer: CordaKafkaConsumer<String, String>,
        val avroSchemaRegistry: AvroSchemaRegistry,
        val listener: StateAndEventListener<String, String>,
    )

    private fun setupMocks(iterations: Long, latch: CountDownLatch): Mocks {
        val eventConsumer: CordaKafkaConsumer<String, ByteBuffer> = mock()
        val stateConsumer: CordaKafkaConsumer<String, String> = mock()
        val listener: StateAndEventListener<String, String> = mock()
        val producer: CordaKafkaProducer = mock()
        val avroSchemaRegistry: AvroSchemaRegistry = mock()
        val builder: StateAndEventBuilder<String, String, ByteBuffer> = mock()

        val topicPartition = TopicPartition(TOPIC, 0)
        val state = ConsumerRecordAndMeta<String, String>(
            TOPIC_PREFIX,
            ConsumerRecord(TOPIC, 0, 0, "key", "state5")
        )

        doAnswer { eventConsumer }.whenever(builder).createEventConsumer(any(), any(), any(), any())
        doAnswer { stateConsumer }.whenever(builder).createStateConsumer(any(), any(), any())
        doAnswer { producer }.whenever(builder).createProducer(any())
        doAnswer { setOf(topicPartition) }.whenever(stateConsumer).assignment()
        doAnswer { listOf(state) }.whenever(stateConsumer).poll()
        doAnswer { mapOf("$TOPIC$DLQ" to emptyList<TopicPartition>()) }.whenever(eventConsumer).listTopics()

        val mockConsumerRecords = generateMockConsumerRecordAndMetaList(iterations, TOPIC, 0)
        var eventsPaused = false


        doAnswer { eventsPaused = true }.whenever(eventConsumer).pause(any())
        doAnswer { eventsPaused = false }.whenever(eventConsumer).resume(any())
        doAnswer {
            if (eventsPaused) {
                emptyList()
            } else {
                listOf(mockConsumerRecords[latch.count.toInt() - 1])
            }
        }.whenever(eventConsumer).poll()

        return Mocks(builder, producer, eventConsumer, stateConsumer, avroSchemaRegistry, listener)
    }

    @Test
    @Timeout(TEST_TIMEOUT_SECONDS)
    fun `state and event subscription retries`() {
        val iterations = 5
        val latch = CountDownLatch(iterations)
        val (builder, producer, eventConsumer, stateConsumer, avroSchemaRegistry, listener) = setupMocks(iterations.toLong(), latch)
        val processor = StubStateAndEventProcessor(latch, CordaMessageAPIIntermittentException("Test exception"))
        val subscription = KafkaStateAndEventSubscriptionImpl(
            config,
            subscriptionMapFactory,
            builder,
            processor,
            avroSchemaRegistry,
            listener
        )

        subscription.start()
        while (subscription.isRunning) { Thread.sleep(10) }
        assertThat(latch.count).isEqualTo(0)

        verify(builder, times(1)).createEventConsumer(any(), any(), any(), any())
        verify(builder, times(1)).createStateConsumer(any(), any(), any())
        verify(builder, times(1)).createProducer(any())
        verify(stateConsumer, times(6)).poll()
        verify(eventConsumer, times(7)).poll()
        verify(producer, times(5)).beginTransaction()
        verify(producer, times(5)).sendRecords(any())
        verify(producer, times(5)).sendRecordOffsetsToTransaction(any(), any())
        verify(producer, times(5)).tryCommitTransaction()
        verify(listener, times(5)).onPostCommit(any())

        assertThat(processor.inputs.size).isEqualTo(iterations)
        for (i in 0 until iterations) {
            // The list is made counting down so we need our expectations to count down too
            val countValue = iterations - i - 1
            val input = processor.inputs[i]
            assertThat(input.first).isEqualTo("state$countValue")
            assertThat(input.second.key).isEqualTo("key$countValue")
            assertThat((StandardCharsets.UTF_8.decode(input.second.value)).toString()).isEqualTo("value$countValue")
        }
    }

    @Test
    @Timeout(TEST_TIMEOUT_SECONDS)
    fun `state and event subscription processes correct state after event`() {
        val iterations = 5
        val latch = CountDownLatch(iterations)
        val (builder, producer, eventConsumer, stateConsumer, avroSchemaRegistry, listener) = setupMocks(iterations.toLong(), latch)
        val processor = StubStateAndEventProcessor(latch)
        val subscription = KafkaStateAndEventSubscriptionImpl(
            config,
            subscriptionMapFactory,
            builder,
            processor,
            avroSchemaRegistry,
            listener
        )

        subscription.start()
        while (subscription.isRunning) { Thread.sleep(10) }
        assertThat(latch.count).isEqualTo(0)

        verify(builder, times(1)).createEventConsumer(any(), any(), any(), any())
        verify(builder, times(1)).createStateConsumer(any(), any(), any())
        verify(builder, times(1)).createProducer(any())
        verify(stateConsumer, times(6)).poll()
        verify(eventConsumer, times(6)).poll()
        verify(producer, times(5)).beginTransaction()
        verify(producer, times(5)).sendRecords(any())
        verify(producer, times(5)).sendRecordOffsetsToTransaction(any(), any())
        verify(producer, times(5)).tryCommitTransaction()
        verify(listener, times(5)).onPostCommit(any())

        assertThat(processor.inputs.size).isEqualTo(iterations)
        for (i in 0 until iterations) {
            // The list is made counting down so we need our expectations to count down too
            val countValue = iterations - i - 1
            val input = processor.inputs[i]
            assertThat(input.first).isEqualTo("state$countValue")
            assertThat(input.second.key).isEqualTo("key$countValue")
            assertThat((StandardCharsets.UTF_8.decode(input.second.value)).toString()).isEqualTo("value$countValue")
        }
    }


    @Test
    fun `state and event subscription processes multiples events by key, small batches`() {
        val latch = CountDownLatch(30)
        val (builder, producer, eventConsumer, _, avroSchemaRegistry, listener) = setupMocks(0, latch)
        val records = mutableListOf<ConsumerRecordAndMeta<String, String>>()
        var offset = 0
        for (i in 0 until 3) {
            for (j in 0 until 10) {
                records.add(ConsumerRecordAndMeta("", ConsumerRecord(TOPIC, 1, offset.toLong(), "key$i", "value$j")))
                offset++
            }
        }

        var eventsPaused = false
        doAnswer {
            if (eventsPaused) {
                emptyList()
            } else {
                eventsPaused = true
                records
            }
        }.whenever(eventConsumer).poll()

        val processor = StubStateAndEventProcessor(latch)
        val subscription = KafkaStateAndEventSubscriptionImpl(
            config,
            subscriptionMapFactory,
            builder,
            processor,
            avroSchemaRegistry,
            listener
        )

        subscription.start()
        assertTrue(latch.await(TEST_TIMEOUT_SECONDS, SECONDS))
        subscription.stop()

        verify(builder, times(1)).createEventConsumer(any(), any(), any(), any())
        verify(builder, times(1)).createStateConsumer(any(), any(), any())
        verify(builder, times(1)).createProducer(any())
        verify(producer, times(28)).beginTransaction()
        verify(producer, times(28)).sendRecords(any())
        verify(producer, times(28)).sendRecordOffsetsToTransaction(any(), any())
        verify(producer, times(28)).tryCommitTransaction()
        verify(listener, times(28)).onPostCommit(any())

        assertThat(processor.inputs.size).isEqualTo(30)
    }


    @Test
    fun `state and event subscription processes multiples events by key, large batches`() {
        val latch = CountDownLatch(30)
        val (builder, producer, eventConsumer, _, avroSchemaRegistry, listener) = setupMocks(0, latch)
        val records = mutableListOf<ConsumerRecordAndMeta<String, String>>()
        var offset = 0
        for (j in 0 until 3) {
            for (i in 0 until 10) {
                records.add(ConsumerRecordAndMeta("", ConsumerRecord(TOPIC, 1, offset.toLong(), "key$i", "value$j")))
                offset++
            }
        }

        var eventsPaused = false
        doAnswer {
            if (eventsPaused) {
                emptyList()
            } else {
                eventsPaused = true
                records
            }
        }.whenever(eventConsumer).poll()

        val processor = StubStateAndEventProcessor(latch)
        val subscription = KafkaStateAndEventSubscriptionImpl(
            config,
            subscriptionMapFactory,
            builder,
            processor,
            avroSchemaRegistry,
            listener
        )

        subscription.start()
        assertTrue(latch.await(TEST_TIMEOUT_SECONDS, SECONDS))
        subscription.stop()

        verify(builder, times(1)).createEventConsumer(any(), any(), any(), any())
        verify(builder, times(1)).createStateConsumer(any(), any(), any())
        verify(builder, times(1)).createProducer(any())
        verify(producer, times(3)).beginTransaction()
        verify(producer, times(3)).sendRecords(any())
        verify(producer, times(3)).sendRecordOffsetsToTransaction(any(), any())
        verify(producer, times(3)).tryCommitTransaction()
        verify(listener, times(3)).onPostCommit(any())

        assertThat(processor.inputs.size).isEqualTo(30)
    }


    @Test
    fun `state and event subscription verify partition sync`() {
        var latch = CountDownLatch(1)
        val (builder, _, eventConsumer, stateConsumer, avroSchemaRegistry, listener) = setupMocks(0, latch)
        val records = mutableListOf<ConsumerRecordAndMeta<String, String>>()
        records.add(ConsumerRecordAndMeta("", ConsumerRecord(TOPIC, 1, 1, "key1", "value1")))

        doAnswer {
            records
        }.whenever(eventConsumer).poll()

        val processor = StubStateAndEventProcessor(latch)
        val subscription = KafkaStateAndEventSubscriptionImpl(
            config,
            subscriptionMapFactory,
            builder,
            processor,
            avroSchemaRegistry,
            listener
        )

        val partitions = listOf(TopicPartition(TOPIC, 0))
        val beginningOffsets = mapOf(TopicPartition("$TOPIC.state", 0) to 0L)
        val endOffsets = mapOf(TopicPartition("$TOPIC.state", 0) to 1L)
        whenever(stateConsumer.beginningOffsets(any())).thenReturn(beginningOffsets)
        whenever(stateConsumer.endOffsets(any())).thenReturn(endOffsets)
        whenever(stateConsumer.position(any())).thenReturn(100)

        subscription.start()
        assertTrue(latch.await(TEST_TIMEOUT_SECONDS, SECONDS))

        subscription.onPartitionsAssigned(partitions)

        latch = CountDownLatch(1)
        processor.latch = latch
        assertTrue(latch.await(TEST_TIMEOUT_SECONDS, SECONDS))

        subscription.onPartitionsRevoked(partitions)

        subscription.stop()

        verify(stateConsumer, times(2)).assign(any())
        verify(stateConsumer, times(1)).seekToBeginning(any())
        verify(stateConsumer, times(1)).beginningOffsets(any())
        verify(stateConsumer, times(1)).endOffsets(any())
        verify(eventConsumer, times(1)).pause(any())

        verify(stateConsumer, times(1)).position(any())
        verify(stateConsumer, times(1)).position(any())
        verify(eventConsumer, times(1)).resume(any())
        verify(listener, times(1)).onPartitionSynced(any())

        verify(stateConsumer, atLeast(1)).assignment()
        verify(listener, times(1)).onPartitionLost(any())
    }

    @Test
    fun `state and event subscription verify dead letter`() {
        val latch = CountDownLatch(1)
        val (builder, producer, eventConsumer, _, avroSchemaRegistry, listener) = setupMocks(0, latch)
        val records = mutableListOf<ConsumerRecordAndMeta<String, String>>()
        records.add(ConsumerRecordAndMeta("", ConsumerRecord(TOPIC, 1, 1, "key1", "value1")))

        var eventsPaused = false
        doAnswer {
            if (eventsPaused) {
                emptyList()
            } else {
                eventsPaused = true
                records
            }
        }.whenever(eventConsumer).poll()

        val shortWaitProcessorConfig = config
            .withValue(KafkaProperties.CONSUMER_MAX_POLL_INTERVAL.replace("consumer", "eventConsumer"),
                ConfigValueFactory.fromAnyRef(10000))
            .withValue(KafkaProperties.CONSUMER_PROCESSOR_TIMEOUT.replace("consumer", "eventConsumer"),
                ConfigValueFactory.fromAnyRef(100))

        val processor = StubStateAndEventProcessor(latch, null, 3000)
        val subscription = KafkaStateAndEventSubscriptionImpl(
            shortWaitProcessorConfig,
            subscriptionMapFactory,
            builder,
            processor,
            avroSchemaRegistry,
            listener
        )

        subscription.start()
        assertTrue(latch.await(TEST_TIMEOUT_SECONDS, SECONDS))
        subscription.stop()

        verify(builder, times(1)).createEventConsumer(any(), any(), any(), any())
        verify(builder, times(1)).createStateConsumer(any(), any(), any())
        verify(builder, times(1)).createProducer(any())
        verify(producer, times(1)).beginTransaction()
        verify(producer, times(1)).sendRecords(any())
        verify(producer, times(1)).sendRecordOffsetsToTransaction(any(), any())
        verify(producer, times(1)).tryCommitTransaction()
        verify(listener, times(1)).onPostCommit(any())
    }
}
