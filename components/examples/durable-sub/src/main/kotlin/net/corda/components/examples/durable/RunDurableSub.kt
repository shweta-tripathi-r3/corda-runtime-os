package net.corda.components.examples.durable

import net.corda.components.examples.durable.processor.DemoPubSubProcessor
import net.corda.messaging.api.subscription.factory.SubscriptionFactory
import net.corda.messaging.api.subscription.factory.config.SubscriptionConfig
import net.corda.osgi.api.Application
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

@Component
class RunDurableSub @Activate constructor(
    @Reference(service = SubscriptionFactory::class)
    private val subscriptionFactory: SubscriptionFactory
) : Application {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun startup(args: Array<String>) {
        if (args.size != 5) {
            println("Required command line arguments: inputTopicName groupName instanceId outputEventTopic outputPubSubTopic")
            exitProcess(1)
        }

        val topic = args[0]
        val groupName = args[1]
        val instanceId = args[2]
        val outputEventTopic = args[3]
        val outputPubSubTopic = args[4]

        val processor = DemoPubSubProcessor(outputEventTopic, outputPubSubTopic)
        val subscription = subscriptionFactory.createDurableSubscription(
            SubscriptionConfig(groupName, topic, instanceId.toInt()),
            processor,
            mapOf(),
            null)

        subscription.start()
    }

    override fun shutdown() {
        log.info("Stopping durable sub")
    }
}