package net.corda.lifecycle.domino.logic

import com.typesafe.config.Config
import net.corda.configuration.read.ConfigurationHandler
import net.corda.configuration.read.ConfigurationReadService
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.LifecycleEventHandler
import net.corda.lifecycle.domino.logic.events.NewConfigurationEvent
import net.corda.lifecycle.domino.logic.events.StartComponentEvent
import net.corda.lifecycle.domino.logic.events.StopComponentEvent
import net.corda.lifecycle.domino.logic.util.ResourcesHolder
import net.corda.v5.base.util.contextLogger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.write

abstract class ConfigurationAwareLeafTile<C>(
    coordinatorFactory: LifecycleCoordinatorFactory,
    private val configurationReaderService: ConfigurationReadService,
    private val key: String,
    private val configFactory: (Config) -> C
): DominoTile(coordinatorFactory) {

    companion object {
        private val logger = contextLogger()
    }

    override fun eventHandler(): LifecycleEventHandler = EventHandler()

    private var configurationHolder: C? = null
    protected val resources = ResourcesHolder()

    private inner class Handler : ConfigurationHandler {
        override fun onNewConfiguration(changedKeys: Set<String>, config: Map<String, Config>) {
            coordinator.postEvent(NewConfigurationEvent(changedKeys, config))
        }
    }

    private val registration = AtomicReference<AutoCloseable>(null)

    override fun close() {
        registration.getAndSet(null)?.close()
        super.close()
    }

    private fun applyNewConfiguration(newConfiguration: C) {
        @Suppress("TooGenericExceptionCaught")
        try {
            logger.info("Got configuration $name")
            val oldConfiguration = configurationHolder
            configurationHolder = newConfiguration
            if (oldConfiguration == newConfiguration) {
                logger.info("Configuration had not changed $name")
                return
            } else if ((state == State.StoppedDueToError)) {
                logger.info("Reconfiguring $name")
                applyNewConfiguration(newConfiguration, oldConfiguration)
                state = State.Started
                logger.info("Reconfigured $name")
            }
        } catch (e: Throwable) {
            state = State.StoppedDueToError
        }
    }

    abstract fun applyNewConfiguration(newConfiguration: C, oldConfiguration: C?)

    private inner class EventHandler: LifecycleEventHandler {
        override fun processEvent(event: LifecycleEvent, coordinator: LifecycleCoordinator) {
            stateLock.write { handleEvent(event) }
        }

        private fun handleEvent(event: LifecycleEvent) {
            when(event) {
                is StartComponentEvent -> {
                    when (state) {
                        State.Created -> {
                            if(registration.get() == null) {
                                registration.getAndSet(
                                    configurationReaderService.registerForUpdates(Handler()))
                                    ?.close()
                            }
                        }
                        State.StoppedByParent -> {
                            if (configurationHolder != null) {
                                applyNewConfiguration(configurationHolder!!)
                            }
                        }
                        State.Started, State.StoppedDueToError -> { }
                    }

                }
                is StopComponentEvent -> {
                    when (state) {
                        State.Created, State.Started -> {
                            resources.close()
                            state = State.StoppedByParent
                        }
                        State.StoppedByParent, State.StoppedDueToError -> { }
                    }

                }
                is NewConfigurationEvent -> {
                    when (state) {
                        State.Created, State.Started, State.StoppedDueToError -> {
                            if (event.changedKeys.contains(key)) {
                                val newConfiguration = event.config[key]
                                if (newConfiguration != null) {
                                    applyNewConfiguration(configFactory(newConfiguration))
                                }
                            }
                        }
                        State.StoppedByParent -> { }
                    }

                }
                else -> logger.warn("Unexpected event: $event")
            }
        }

    }
}
