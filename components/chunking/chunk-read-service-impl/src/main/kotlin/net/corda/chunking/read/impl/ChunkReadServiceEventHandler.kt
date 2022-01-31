package net.corda.chunking.read.impl

import net.corda.chunking.db.ChunkDbWriter
import net.corda.chunking.db.ChunkDbWriterFactory
import net.corda.chunking.read.events.StartProcessingEvent
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.LifecycleEventHandler
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.StopEvent
import net.corda.v5.base.exceptions.CordaRuntimeException

class ChunkReadServiceEventHandler(private val chunkDbWriterFactory: ChunkDbWriterFactory) : LifecycleEventHandler {
    private var chunkDbWriter: ChunkDbWriter? = null

    override fun processEvent(event: LifecycleEvent, coordinator: LifecycleCoordinator) {
        when (event) {
            is StartProcessingEvent -> onStartProcessingEvent(event, coordinator)
            is StopEvent -> onStop(coordinator)
        }
    }

    private fun onStop(coordinator: LifecycleCoordinator) {
        chunkDbWriter?.stop()
        chunkDbWriter = null
        coordinator.updateStatus(LifecycleStatus.DOWN)
    }

    private fun onStartProcessingEvent(event: StartProcessingEvent, coordinator: LifecycleCoordinator) {
        if (chunkDbWriter != null) {
            throw CordaRuntimeException("An attempt was made to start processing twice.")
        }

        try {
            chunkDbWriter = chunkDbWriterFactory
                .create(event.config, event.instanceId, event.entityManagerFactory)
                .apply { start() }
        } catch (e: Exception) {
            coordinator.updateStatus(LifecycleStatus.ERROR)
            throw CordaRuntimeException("Could not subscribe to requests.", e)
        }

        coordinator.updateStatus(LifecycleStatus.UP)
    }
}
