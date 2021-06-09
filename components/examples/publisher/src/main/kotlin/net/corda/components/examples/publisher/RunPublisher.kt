package net.corda.components.examples.publisher

import net.corda.data.demo.DemoRecord
import net.corda.messaging.api.publisher.config.PublisherConfig
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.records.Record
import net.corda.osgi.api.Application
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

@Component
class RunPubSub @Activate constructor(
    @Reference(service = PublisherFactory::class)
    private val publisherFactory: PublisherFactory
) : Application {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun startup(args: Array<String>) {
        if (args.size != 4) {
            println("Required command line arguments: outputTopic clientId instanceId numberOfRecords")
            exitProcess(1)
        }

        val topic = args[0]
        val clientId = args[1]
        val instanceId = args[2].toInt()
        val numberOfRecords = args[3].toInt()

        val pubconfigAsync = PublisherConfig(clientId)
        val pubconfigTransactional = PublisherConfig("$clientId-transactional", instanceId)
        val publisherAsync = publisherFactory.createPublisher(pubconfigAsync, mutableMapOf())
        val publisherTransactional = publisherFactory.createPublisher(pubconfigTransactional, mutableMapOf())

        val recordsAsync = mutableListOf<Record<*, *>>()
        for (i in 1..numberOfRecords) {
            recordsAsync.add(Record(topic, "key1", DemoRecord(i)))
        }

        val recordsTransactional = mutableListOf<Record<*, *>>()
        for (i in 1..numberOfRecords) {
            recordsTransactional.add(Record(topic, "key2", DemoRecord(i)))
        }

        log.info("Publishing Async records..")
        publisherAsync.publish(recordsAsync)
        log.info("Publishing transactional records..")
        publisherTransactional.publish(recordsTransactional)
        log.info("Finished publishing.")
        publisherAsync.close()
        publisherTransactional.close()
    }

    override fun shutdown() {
        log.info("Shutting down publisher!")
    }
}