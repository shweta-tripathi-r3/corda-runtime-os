package net.corda.lifecycle.domino.logic

import net.corda.lifecycle.Lifecycle
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleEventHandler
import net.corda.lifecycle.LifecycleException
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.domino.logic.events.StartComponentEvent
import net.corda.lifecycle.domino.logic.events.StopComponentEvent
import net.corda.v5.base.util.contextLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

abstract class DominoTile(
    coordinatorFactory: LifecycleCoordinatorFactory,
) : Lifecycle {
    companion object {
        private val logger = contextLogger()
        private val instancesIndex = ConcurrentHashMap<String, Int>()
    }

    enum class State {
        Created,
        Started,
        StoppedDueToError,
        StoppedByParent
    }

    val name = LifecycleCoordinatorName(
        javaClass.simpleName,
        instancesIndex.compute(javaClass.simpleName) { _, last ->
            if (last == null) {
                1
            } else {
                last + 1
            }
        }.toString()
    )

    override fun start() {
        coordinator.start()
        coordinator.postEvent(StartComponentEvent)
    }

    override fun stop() {
        coordinator.postEvent(StopComponentEvent)
    }

    protected abstract fun eventHandler(): LifecycleEventHandler

    protected val coordinator = coordinatorFactory.createCoordinator(name, eventHandler())

    var state: State = State.Created
        set(newState) {
            val oldState = state
            field = newState
            if (newState != oldState) {
                val status = when (newState) {
                    State.Started -> LifecycleStatus.UP
                    State.Created, State.StoppedByParent -> LifecycleStatus.DOWN
                    State.StoppedDueToError -> LifecycleStatus.ERROR
                }
                coordinator.updateStatus(status)
                logger.info("State of $name is $newState")
            }
        }

    override val isRunning: Boolean
        get() = state == State.Started

    val stateLock = ReentrantReadWriteLock(true)

    override fun close() {
        stop()

        try {
            coordinator.close()
        } catch (e: LifecycleException) {
            // This try-catch should be removed once CORE-2786 is fixed
            logger.debug("Could not close coordinator", e)
        }
    }

    override fun toString(): String {
        return "$name: $state"
    }
}
