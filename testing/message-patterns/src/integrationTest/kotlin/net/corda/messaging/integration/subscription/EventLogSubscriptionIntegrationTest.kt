package net.corda.messaging.integration.subscription

import com.typesafe.config.ConfigValueFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import net.corda.data.demo.DemoRecord
import net.corda.db.messagebus.testkit.DBSetup
import net.corda.libs.messaging.topic.utils.TopicUtils
import net.corda.libs.messaging.topic.utils.factory.TopicUtilsFactory
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.messaging.api.publisher.Publisher
import net.corda.messaging.api.publisher.config.PublisherConfig
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.subscription.Subscription
import net.corda.messaging.api.subscription.config.SubscriptionConfig
import net.corda.messaging.api.subscription.factory.SubscriptionFactory
import net.corda.messaging.integration.IntegrationTestProperties.Companion.TEST_CONFIG
import net.corda.messaging.integration.TopicTemplates
import net.corda.messaging.integration.TopicTemplates.Companion.EVENT_LOG_TOPIC2
import net.corda.messaging.integration.getDemoRecords
import net.corda.messaging.integration.getKafkaProperties
import net.corda.messaging.integration.getTopicConfig
import net.corda.messaging.integration.processors.TestEventLogProcessor
import net.corda.schema.configuration.BootConfig.INSTANCE_ID
import net.corda.test.util.eventually
import net.corda.v5.base.util.millis
import net.corda.v5.base.util.seconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.osgi.test.common.annotation.InjectService
import org.osgi.test.junit5.context.BundleContextExtension
import org.osgi.test.junit5.service.ServiceExtension

@ExtendWith(ServiceExtension::class, BundleContextExtension::class, DBSetup::class)
class EventLogSubscriptionIntegrationTest {

    private lateinit var publisherConfig: PublisherConfig
    private lateinit var publisher: Publisher

    private companion object {
        const val CLIENT_ID = "eventLogTestPublisher"
    }

    @InjectService(timeout = 4000)
    lateinit var publisherFactory: PublisherFactory

    @InjectService(timeout = 4000)
    lateinit var subscriptionFactory: SubscriptionFactory

    @InjectService(timeout = 4000)
    lateinit var lifecycleCoordinatorFactory: LifecycleCoordinatorFactory

    @InjectService(timeout = 4000)
    lateinit var topicUtilFactory: TopicUtilsFactory

    private lateinit var topicUtils: TopicUtils

    @BeforeEach
    fun beforeEach() {
        topicUtils = topicUtilFactory.createTopicUtils(getKafkaProperties())
    }


    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    fun `transactional publish records, start two durable subscription, stop subs, publish again and start subs`() {
        val partitionRecordCount = 1
        val keyCount = 20
        val records = getDemoRecords(EVENT_LOG_TOPIC2, partitionRecordCount, keyCount)
        val publishRecordCount = records.size
        val totalRecordCount = publishRecordCount * 2
        assertThat(publishRecordCount).isEqualTo(keyCount*partitionRecordCount)
        topicUtils.createTopics(getTopicConfig(TopicTemplates.EVENT_LOG_TOPIC2_TEMPLATE))

        val coordinator =
            lifecycleCoordinatorFactory.createCoordinator(LifecycleCoordinatorName("eventLogTest2"))
            { event: LifecycleEvent, coordinator: LifecycleCoordinator ->
                when (event) {
                    is RegistrationStatusChangeEvent -> {
                            coordinator.updateStatus(event.status)
                    }
                }
            }
        coordinator.start()

        publisherConfig = PublisherConfig(CLIENT_ID + EVENT_LOG_TOPIC2)
        publisher = publisherFactory.createPublisher(publisherConfig, TEST_CONFIG)
        val futures = publisher.publish(records)
        assertThat(futures.size).isEqualTo(1)
        futures[0].get()

        val latch = CountDownLatch(totalRecordCount)
        val eventLogSubs = (1..keyCount).map { createSub(TestEventLogProcessor(latch, "outputEventLog2", it)) }

        eventLogSubs.forEach { it.start() }

        coordinator.followStatusChangesByName(eventLogSubs.map { it.subscriptionName  }.toSet())

        eventually(duration = 10.seconds, waitBetween = 100.millis, waitBefore = 0.millis) {
            assertThat(latch.count).isEqualTo(publishRecordCount.toLong())
        }

        eventLogSubs.forEach { it.close() }

        publisher.publish(records).forEach { it.get() }

        val eventLogSubs2 = ((keyCount+1)..keyCount*2).map { createSub(TestEventLogProcessor(latch, "outputEventLog2", it)) }

        val futures2 = publisher.publish(records)
        assertThat(futures2.size).isEqualTo(1)
        futures[0].get()


        eventLogSubs2.forEach { it.start() }

        assertTrue(latch.await(10, TimeUnit.SECONDS))

        eventLogSubs2.forEach { it.close() }

        publisher.close()

       /* val verifyLatch = CountDownLatch(totalRecordCount*2)
        val verifySub = createSub(TestEventLogProcessor(verifyLatch, null, 1), "outputEventLog2")
        verifySub.start()
        eventually(duration = 10.seconds, waitBetween = 100.millis, waitBefore = 0.millis) {
            assertThat(latch.count).isEqualTo(totalRecordCount)
        }
        verifySub.close()*/
    }

    fun createSub(processor: TestEventLogProcessor, topic: String = EVENT_LOG_TOPIC2): Subscription<String, DemoRecord> {
        val config = TEST_CONFIG.withValue(
            INSTANCE_ID,
            ConfigValueFactory.fromAnyRef(processor.id)
        )
        return subscriptionFactory.createEventLogSubscription(
            SubscriptionConfig("$topic-group", topic),
            processor,
            config,
            null
        )
    }
}
