package net.corda.p2p.gateway.domino.util

import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.messaging.api.subscription.Subscription
import net.corda.p2p.gateway.domino.LeafDominoLifecycle

class EventLogSubscriptionWithDominoLogic<K, V>(private val eventLogSubscription: Subscription<K, V>,
                                                lifecycleCoordinatorFactory: LifecycleCoordinatorFactory):
    LeafDominoLifecycle(lifecycleCoordinatorFactory) {

    override fun startSequence() {
        eventLogSubscription.start()
        hasStarted()
    }

    override fun stopSequence() {
        eventLogSubscription.stop()
    }


}