package net.corda.p2p.gateway.messaging.session

import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.messaging.api.processor.CompactedProcessor
import net.corda.messaging.api.records.Record
import net.corda.messaging.api.subscription.factory.SubscriptionFactory
import net.corda.messaging.api.subscription.factory.config.SubscriptionConfig
import net.corda.p2p.SessionPartitions
import net.corda.p2p.gateway.domino.LeafDominoLifecycle
import net.corda.p2p.gateway.domino.LifecycleWithCoordinator
import net.corda.p2p.schema.Schema.Companion.SESSION_OUT_PARTITIONS
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.util.concurrent.ConcurrentHashMap

class SessionPartitionMapperImpl(
    subscriptionFactory: SubscriptionFactory,
    coordinatorFactory: LifecycleCoordinatorFactory
) : SessionPartitionMapper,
    LeafDominoLifecycle(coordinatorFactory) {

    companion object {
        const val CONSUMER_GROUP_ID = "session_partitions_mapper"
    }

    private val sessionPartitionsMapping = ConcurrentHashMap<String, List<Int>>()

    private val sessionPartitionSubscription = subscriptionFactory.createCompactedSubscription(
        SubscriptionConfig(CONSUMER_GROUP_ID, SESSION_OUT_PARTITIONS),
        SessionPartitionProcessor()
    )

    override fun startSequence() {
        sessionPartitionSubscription.start()
    }

    override fun stopSequence() {
        sessionPartitionSubscription.stop()
    }

    override fun getPartitions(sessionId: String): List<Int>? {
        if (!isRunning) {
            throw RuntimeException("getPartitions invoked, while session partition mapper is not running.")
        } else {
            return sessionPartitionsMapping[sessionId]
        }
    }

    private inner class SessionPartitionProcessor :
        CompactedProcessor<String, SessionPartitions> {
        override val keyClass: Class<String>
            get() = String::class.java
        override val valueClass: Class<SessionPartitions>
            get() = SessionPartitions::class.java

        override fun onSnapshot(currentData: Map<String, SessionPartitions>) {
            sessionPartitionsMapping.putAll(currentData.map { it.key to it.value.partitions })
            hasStarted()
        }

        override fun onNext(
            newRecord: Record<String, SessionPartitions>,
            oldValue: SessionPartitions?,
            currentData: Map<String, SessionPartitions>
        ) {
            if (newRecord.value == null) {
                sessionPartitionsMapping.remove(newRecord.key)
            } else {
                sessionPartitionsMapping[newRecord.key] = newRecord.value!!.partitions
            }
        }
    }
}
