package net.corda.p2p.gateway.domino

import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.LifecycleEventHandler
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.p2p.gateway.domino.DominoLifecycle.*
import net.corda.v5.base.util.contextLogger

abstract class NonLeafDominoLifecycle(
    private val coordinatorFactory: LifecycleCoordinatorFactory,
    instanceId: String
): DominoLifecycle {

    companion object {
        private val logger = contextLogger()
    }

    abstract fun children(): List<DominoLifecycle>

    val name: LifecycleCoordinatorName = LifecycleCoordinatorName(
        javaClass.simpleName,
        instanceId
    )

    private val coordinator = coordinatorFactory.createCoordinator(name, EventHandler())

    override val isRunning: Boolean
        get() = state == State.Started

    var state: State = State.Created

    override fun coordinator(): LifecycleCoordinator = coordinator
    override fun state(): State = state

    override fun start() {
        logger.info("Starting $name")
        if (!coordinator.isRunning) {
            coordinator.start()
            children().forEach { coordinator.followStatusChanges(setOf(it.coordinator())) }
        }
        when (state) {
            State.Created, State.StoppedByParent, State.StoppedDueToError -> {
                // trigger start for children and wait for UP signal indicating they have started.
                children().forEach { it.start() }
            }
            State.Started -> {
                // Do nothing
            }
        }
    }

    override fun stop() {
        logger.info("Stopping $name")
        when (state) {
            State.Created -> {
                state = State.StoppedByParent
                coordinator.updateStatus(LifecycleStatus.DOWN)
            }
            State.Started -> {
                children().forEach { it.stop() }
                state = State.StoppedByParent
                coordinator.updateStatus(LifecycleStatus.DOWN)
            }
            State.StoppedByParent, State.StoppedDueToError -> {
                // Nothing to do
            }
        }
    }

    private inner class EventHandler : LifecycleEventHandler {
        override fun processEvent(event: LifecycleEvent, coordinator: LifecycleCoordinator) {
            when (event) {
                is RegistrationStatusChangeEvent -> {
                    if (event.status == LifecycleStatus.UP) {
                        if (children().all { it.state() == State.Started }) {
                            state = State.Started
                            coordinator.updateStatus(LifecycleStatus.UP)
                        } else if (children().none { it.state() == State.StoppedDueToError }) {
                            children().forEach { it.start() }
                        } else {
                            children().forEach { it.stop() }
                        }
                    } else {
                        // one of our children went down.
                        children().forEach { it.stop() }
                        if (state == State.Started) {
                            state = State.StoppedDueToError
                            coordinator.updateStatus(LifecycleStatus.DOWN)
                        }
                    }
                }
                else -> {
                    logger.warn("Unexpected event $event")
                }
            }
        }
    }

}