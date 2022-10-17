package net.corda.virtualnode.upgrade.service.internal

import net.corda.configuration.read.ConfigChangedEvent
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.LifecycleEventHandler
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.lifecycle.StartEvent
import net.corda.v5.base.util.contextLogger

class VirtualNodeUpgradeServiceEventHandler : LifecycleEventHandler {

    private companion object {
        val log = contextLogger()
    }

    override fun processEvent(event: LifecycleEvent, coordinator: LifecycleCoordinator) {
        when (event) {
            is StartEvent -> {
                log.info("Received start event, following dependencies.")
            }
            is RegistrationStatusChangeEvent -> {
                log.info("Registration status change received: ${event.status.name}.")
                when (event.status) {
                    LifecycleStatus.UP -> {

                    }
                    LifecycleStatus.DOWN -> {

                    }

                    LifecycleStatus.ERROR -> {

                    }
                }
            }
            is ConfigChangedEvent -> {

            }
        }

    }
}