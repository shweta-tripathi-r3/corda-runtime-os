package net.corda.components.examples.pubsub

import net.corda.components.examples.pubsub.processor.DemoPubSubProcessor
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
class RunPubSub @Activate constructor(
    @Reference(service = SubscriptionFactory::class)
    private val subscriptionFactory: SubscriptionFactory
) : Application {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun startup(args: Array<String>) {
        if (args.size != 2) {
            println("Required command line arguments: topicName groupName")
            exitProcess(1)
        }

        val topic = args[0]
        val groupName = args[1]

        val processor = DemoPubSubProcessor()
        val subscription = subscriptionFactory.createPubSubSubscription(
            SubscriptionConfig(groupName,topic),
            processor,
            null,
            mapOf())

        subscription.start()
    }

    override fun shutdown() {
        log.info("Stopping pub sub sub.")
    }
}