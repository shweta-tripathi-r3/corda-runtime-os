package net.corda.virtualnode.upgrade.manager

import net.corda.libs.configuration.SmartConfig
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.subscription.factory.SubscriptionFactory

interface VirtualNodeUpgradeManagerFactory {
    fun create(
        messagingConfig: SmartConfig,
        publisherFactory: PublisherFactory,
        subscriptionFactory: SubscriptionFactory
    ): VirtualNodeUpgradeManager
}