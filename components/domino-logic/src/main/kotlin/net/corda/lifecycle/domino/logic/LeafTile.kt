package net.corda.lifecycle.domino.logic

import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.LifecycleEventHandler
import net.corda.lifecycle.domino.logic.events.StartComponentEvent
import net.corda.lifecycle.domino.logic.events.StopComponentEvent
import net.corda.lifecycle.domino.logic.util.ResourcesHolder
import net.corda.v5.base.util.contextLogger
import kotlin.concurrent.write

abstract class LeafTile(
    coordinatorFactory: LifecycleCoordinatorFactory
) :DominoTile(coordinatorFactory) {

    companion object {
        private val logger = contextLogger()
    }

    protected val resources = ResourcesHolder()

    abstract fun createResources()

    fun started() {
        stateLock.write { state = State.Started }
    }

    override fun eventHandler(): LifecycleEventHandler {
        return EventHandler()
    }

    private inner class EventHandler: LifecycleEventHandler {
        override fun processEvent(event: LifecycleEvent, coordinator: LifecycleCoordinator) {
            stateLock.write { handleEvent(event) }
        }

        private fun handleEvent(event: LifecycleEvent) {
            when(event) {
                StartComponentEvent -> {
                    when (state) {
                        State.Created, State.StoppedByParent -> {
                            @Suppress("TooGenericExceptionCaught")
                            try {
                                createResources()
                            } catch (e: Throwable) {
                                state = State.StoppedDueToError
                            }
                        }
                        State.Started, State.StoppedDueToError -> {}
                    }

                }
                StopComponentEvent -> {
                    when (state) {
                        State.Created, State.Started -> {
                            resources.close()
                            state = State.StoppedByParent
                        }
                        State.StoppedByParent, State.StoppedDueToError -> {}
                    }
                }
                else -> logger.warn("Unexpected event: $event")
            }
        }

    }

}
