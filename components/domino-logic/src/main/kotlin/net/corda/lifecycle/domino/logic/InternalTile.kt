package net.corda.lifecycle.domino.logic

import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.LifecycleEventHandler
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.RegistrationHandle
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.lifecycle.domino.logic.events.StartComponentEvent
import net.corda.lifecycle.domino.logic.events.StopComponentEvent
import net.corda.v5.base.util.contextLogger
import kotlin.concurrent.write

abstract class InternalTile(coordinatorFactory: LifecycleCoordinatorFactory) : DominoTile(coordinatorFactory) {
    companion object {
        private val logger = contextLogger()
    }

    override fun eventHandler(): LifecycleEventHandler = EventHandler()

    abstract val children: Collection<DominoTile>

    private var registrations: List<RegistrationHandle>? = null

    override fun close() {
        registrations?.forEach {
            it.close()
        }
        super.close()
        children.reversed().forEach {
            @Suppress("TooGenericExceptionCaught")
            try {
                it.close()
            } catch (e: Throwable) {
                logger.warn("Could not close $it", e)
            }
        }
    }
    override fun toString(): String {
        return "$name: $state: $children"
    }

    private inner class EventHandler: LifecycleEventHandler {
        override fun processEvent(event: LifecycleEvent, coordinator: LifecycleCoordinator) {
            stateLock.write { handleEvent(event) }
        }

        private fun handleEvent(event: LifecycleEvent) {
            when(event) {
                is StartComponentEvent -> {
                    when (state) {
                        State.Created -> {
                            if (registrations == null) {
                                registrations = children.map {
                                    coordinator.followStatusChangesByName(setOf(it.name))
                                }
                            }
                            children.forEach { it.start() }
                        }
                        State.StoppedByParent -> {
                            children.forEach { it.start() }
                        }
                        State.Started, State.StoppedDueToError -> { }
                    }
                }
                is StopComponentEvent -> {
                    when (state) {
                        State.Created, State.Started -> {
                            children.filter { it.state in setOf(State.Created, State.Started)  }.forEach { it.stop() }
                        }
                        State.StoppedDueToError, State.StoppedByParent -> { }
                    }

                }
                is RegistrationStatusChangeEvent -> {
                    when (event.status) {
                        LifecycleStatus.UP -> {
                            if (children.all { it.state == State.Started }) {
                                state = State.Started
                            } else if (children.any { it.state == State.StoppedDueToError }) {
                                children.filter {
                                    it.state != State.StoppedDueToError
                                }.forEach {
                                    it.stop()
                                }
                            }
                        }
                        LifecycleStatus.DOWN, LifecycleStatus.ERROR -> {
                            children.filter { it.state in setOf(State.Created, State.Started) }.forEach { it.stop() }

                            state = if (children.any { it.state == State.StoppedDueToError }) {
                                State.StoppedDueToError
                            } else {
                                State.StoppedByParent
                            }
                        }
                    }
                }
                else -> logger.warn("Unexpected event: $event")
            }
        }

    }
}
