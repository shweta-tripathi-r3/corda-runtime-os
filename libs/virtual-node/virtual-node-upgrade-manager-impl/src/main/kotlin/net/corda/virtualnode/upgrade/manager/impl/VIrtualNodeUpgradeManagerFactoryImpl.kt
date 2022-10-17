package net.corda.virtualnode.upgrade.manager.impl

import net.corda.libs.configuration.SmartConfig
import net.corda.messaging.api.publisher.config.PublisherConfig
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.subscription.config.RPCConfig
import net.corda.messaging.api.subscription.config.SubscriptionConfig
import net.corda.messaging.api.subscription.factory.SubscriptionFactory
import net.corda.schema.Schemas.Membership.Companion.RE_REGISTRATION_COMMAND_TOPIC
import net.corda.schema.Schemas.VirtualNode.Companion.VIRTUAL_NODE_UPGRADE_REQUEST_TOPIC
import net.corda.virtualnode.upgrade.manager.VirtualNodeUpgradeManager
import net.corda.virtualnode.upgrade.manager.VirtualNodeUpgradeManagerFactory
import org.osgi.service.component.annotations.Component

@Component(service = [VirtualNodeUpgradeManagerFactory::class])
class VIrtualNodeUpgradeManagerFactoryImpl : VirtualNodeUpgradeManagerFactory {

    private companion object {
        const val VIRTUAL_NODE_UPGRADE_REQUEST_GROUP_NAME = "virtual.node.upgrade"
        const val VIRTUAL_NODE_UPGRADE_MEMBERSHIP_REREGISTRATION_REQUEST_CLIENT = "virtual.node-upgrade.membership.reregistration.client"
    }

    override fun create(
        messagingConfig: SmartConfig,
        publisherFactory: PublisherFactory,
        subscriptionFactory: SubscriptionFactory
    ): VirtualNodeUpgradeManager {
        val statusTopicPublisher = publisherFactory.createPublisher(
            PublisherConfig("${VIRTUAL_NODE_UPGRADE_REQUEST_GROUP_NAME}.client"),
            messagingConfig
        )
        val membershipReRegistrationRpcSender = publisherFactory.createRPCSender(
            RPCConfig(
                VIRTUAL_NODE_UPGRADE_REQUEST_GROUP_NAME,
                VIRTUAL_NODE_UPGRADE_MEMBERSHIP_REREGISTRATION_REQUEST_CLIENT,
                RE_REGISTRATION_COMMAND_TOPIC,
                String::class.java,// todo
                String::class.java// todo
            ),
            messagingConfig
        )
        val upgradeRequestSubscription = subscriptionFactory.createDurableSubscription(
            SubscriptionConfig(VIRTUAL_NODE_UPGRADE_REQUEST_GROUP_NAME, VIRTUAL_NODE_UPGRADE_REQUEST_TOPIC),
            VirtualNodeUpgradeProcessor(
                statusTopicPublisher,
                membershipReRegistrationRpcSender,

            ),
            messagingConfig,
            null
        )
    }
}