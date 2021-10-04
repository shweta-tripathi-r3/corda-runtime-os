package net.corda.p2p.gateway.domino

import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.LifecycleEventHandler
import net.corda.lifecycle.LifecycleStatus
import net.corda.p2p.gateway.domino.DominoLifecycle.*
import net.corda.v5.base.util.contextLogger
import java.util.UUID

abstract class LeafDominoLifecycle(
    private val coordinatorFactory: LifecycleCoordinatorFactory,
    instanceId: String = UUID.randomUUID().toString()
): DominoLifecycle {

    companion object {
        private val logger = contextLogger()
    }

    abstract fun startSequence()

    abstract fun stopSequence()

    /**
     * This is supposed to be invoked when start has been completed asynchronously (i.e. proper configuration has arrived).
     */
    fun hasStarted() {
        // this might also be called during a restart (e.g. due to new configuration), in which case nothing needs to be done.
        if (state != State.Started) {
            state = State.Started
            coordinator.updateStatus(LifecycleStatus.UP)
        }
    }

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
        }
        when (state) {
            State.Created, State.StoppedByParent, State.StoppedDueToError -> {
                try {
                    startSequence()
                } catch (e: Exception) {
                    logger.warn("Error while trying to start component", e)
                    state = State.StoppedDueToError
                    coordinator.updateStatus(LifecycleStatus.ERROR, "${e.javaClass.simpleName}: ${e.message ?: "-"}")
                }
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
                stopSequence()
                state = State.StoppedByParent
                coordinator.updateStatus(LifecycleStatus.DOWN)
            }
            State.StoppedByParent, State.StoppedDueToError -> {
                // Nothing to do
            }
        }
    }

    fun gotError(error: Throwable) {
        logger.warn("Got error in $name", error)
        when (state) {
            State.Created -> {
                // Cannot fail before even being started.
            }
            State.Started -> {
                stopSequence()
                state = State.StoppedDueToError
                coordinator.updateStatus(LifecycleStatus.ERROR, "${error.javaClass.simpleName}: ${error.message ?: "-"}")
            }
            State.StoppedByParent -> {
                // Nothing to do
            }
            State.StoppedDueToError -> {
                // Nothing to do
            }
        }
    }

    // no-op handler
    private inner class EventHandler : LifecycleEventHandler {
        override fun processEvent(event: LifecycleEvent, coordinator: LifecycleCoordinator) {}
    }

}