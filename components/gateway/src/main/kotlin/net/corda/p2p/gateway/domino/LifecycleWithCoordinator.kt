package net.corda.p2p.gateway.domino

import net.corda.lifecycle.Lifecycle
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.LifecycleEventHandler
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.v5.base.util.contextLogger
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

abstract class LifecycleWithCoordinator(
    private val coordinatorFactory: LifecycleCoordinatorFactory,
    instanceId: String
) :
    Lifecycle {

    constructor(parent: LifecycleWithCoordinator) : this(
        parent.coordinatorFactory,
        parent.name.toString()
    )

    companion object {
        private val logger = contextLogger()
    }

    enum class State {
        Created,
        Started,
        StoppedDueToError,
        StoppedByParent
    }

    val name: LifecycleCoordinatorName = LifecycleCoordinatorName(
        javaClass.simpleName,
        instanceId
    )

    private val coordinator = coordinatorFactory.createCoordinator(name, EventHandler())

    override val isRunning: Boolean
        get() = state == State.Started

    var state: State = State.Created

    abstract val children: Collection<LifecycleWithCoordinator>

    open fun startSequence() {}

    open fun stopSequence() {}

    override fun start() {
        logger.info("Starting $name")
        if (!coordinator.isRunning) {
            coordinator.start()
        }
        when (state) {
            State.Created, State.StoppedByParent, State.StoppedDueToError -> {

                /**
                 * If there are no children, we initiate the start sequence of ourselves.
                 * If there are children, we trigger start() on them and wait until they are all ready before we start ourselves.
                 */
                if (children.isEmpty()) {
                    try {
                        startSequence()
                        state = State.Started
                        coordinator.updateStatus(LifecycleStatus.UP)
                    } catch (e: Exception) {
                        state = State.StoppedDueToError
                        coordinator.updateStatus(LifecycleStatus.ERROR, "${e.javaClass.simpleName}: ${e.message ?: "-"}")
                    }
                } else {
                    children.map {
                        it.name
                    }.map {
                        coordinator.followStatusChangesByName(setOf(it))
                    }.forEach {
                        executeBeforeClose(it::close)
                    }
                    children.forEach { it.start() }
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
                if (children.isEmpty()) {
                    stopSequence()
                    state = State.StoppedByParent
                    coordinator.updateStatus(LifecycleStatus.DOWN)
                } else {
                    children.forEach { it.stop() }
                    state = State.StoppedByParent
                    coordinator.updateStatus(LifecycleStatus.DOWN)
                }
            }
            State.StoppedByParent, State.StoppedDueToError -> {
                // Nothing to do
            }
        }
    }

    fun gotError(error: Throwable) {
        logger.info("Got error in $name", error)
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

    private inner class EventHandler : LifecycleEventHandler {
        override fun processEvent(event: LifecycleEvent, coordinator: LifecycleCoordinator) {
            when (event) {
                is RegistrationStatusChangeEvent -> {
                    if (event.status == LifecycleStatus.UP) {
                        if (children.all { it.state == State.Started }) {
                            state = State.Started
                            coordinator.updateStatus(LifecycleStatus.UP)
                        } else if (children.none { it.state == State.StoppedDueToError }) {
                            children.forEach { it.start() }
                        }
                    } else {
                        children.forEach { it.stop() }
                        // one of our children went down.
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

    override fun close() {
        stopActionsSequence()

        closeActions.onEach {
            @Suppress("TooGenericExceptionCaught")
            try {
                it.invoke()
            } catch (e: Throwable) {
                logger.warn("Fail to close", e)
            }
        }
        closeActions.clear()

        children.reversed().forEach {
            it.close()
        }
        coordinator.close()
    }

    override fun toString(): String {
        return "$name: $state: $children"
    }





    /**
     *  Not needed with new implementation
     */

    private val closeActions = ConcurrentLinkedDeque<()->Unit>()
    fun executeBeforeClose(action: () -> Unit) {
        closeActions.addFirst(action)
    }
    private val stopActions = ConcurrentLinkedDeque<()->Unit>()
    fun executeBeforeStop(action: () -> Unit) {
        stopActions.addFirst(action)
    }

    private fun stopActionsSequence() {
        stopActions.onEach {
            @Suppress("TooGenericExceptionCaught")
            try {
                it.invoke()
            } catch (e: Throwable) {
                logger.warn("Fail to stop", e)
            }
        }
        stopActions.clear()
    }

}
