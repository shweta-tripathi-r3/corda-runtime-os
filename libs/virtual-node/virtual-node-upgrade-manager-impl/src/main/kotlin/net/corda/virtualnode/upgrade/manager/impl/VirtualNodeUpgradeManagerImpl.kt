package net.corda.virtualnode.upgrade.manager.impl

import net.corda.messaging.api.publisher.Publisher
import net.corda.virtualnode.upgrade.manager.VirtualNodeUpgradeManager

class VirtualNodeUpgradeManagerImpl(
    private val publisher: Publisher,
    private val vNodeUpgradeSubscription:
) : VirtualNodeUpgradeManager {
    override fun upgradeVirtualNode() {
        TODO("Not yet implemented")
    }
}