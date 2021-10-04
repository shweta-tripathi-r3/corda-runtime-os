package net.corda.p2p.gateway.domino.util

import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.messaging.api.publisher.Publisher
import net.corda.messaging.api.publisher.config.PublisherConfig
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.records.Record
import net.corda.p2p.gateway.domino.LeafDominoLifecycle
import java.lang.IllegalStateException
import java.util.concurrent.CompletableFuture

// TODO: Ideally we should make [Publisher] a lifecycle, so that we are also able to stop it.
class PublisherWithDominoLogic(private val publisher: Publisher,
                               coordinatorFactory: LifecycleCoordinatorFactory): LeafDominoLifecycle(coordinatorFactory) {

    override fun startSequence() {
        publisher.start()
        hasStarted()
    }

    override fun stopSequence() {}

    fun publishToPartition(records: List<Pair<Int, Record<*, *>>>): List<CompletableFuture<Unit>> {
        return publisher.publishToPartition(records)
    }

    fun publish(records: List<Record<*, *>>): List<CompletableFuture<Unit>> {
        return publisher.publish(records)
    }

}