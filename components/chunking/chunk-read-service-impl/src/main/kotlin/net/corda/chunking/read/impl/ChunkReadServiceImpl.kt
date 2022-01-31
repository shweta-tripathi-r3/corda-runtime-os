package net.corda.chunking.read.impl

import net.corda.chunking.db.ChunkDbWriterFactory
import net.corda.chunking.read.events.StartProcessingEvent
import net.corda.chunking.read.ChunkReadService
import net.corda.libs.configuration.SmartConfig
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.createCoordinator
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import javax.persistence.EntityManagerFactory

@Suppress("UNUSED")
@Component(service = [ChunkReadService::class])
class ChunkReadServiceImpl @Activate constructor(
    @Reference(service = LifecycleCoordinatorFactory::class)
    coordinatorFactory: LifecycleCoordinatorFactory,
    @Reference(service = ChunkDbWriterFactory::class)
    chunkDbWriterFactory: ChunkDbWriterFactory
) : ChunkReadService {

    private val coordinator = let {
        // Reads chunks from Kafka, and writes to the db.
        val eventHandler = ChunkReadServiceEventHandler(chunkDbWriterFactory)
        coordinatorFactory.createCoordinator<ChunkReadService>(eventHandler)
    }

    override fun startProcessing(config: SmartConfig, instanceId: Int, entityManagerFactory: EntityManagerFactory) {
        val startProcessingEvent = StartProcessingEvent(config, instanceId, entityManagerFactory)
        coordinator.postEvent(startProcessingEvent)
    }

    override val isRunning get() = coordinator.isRunning

    override fun start() = coordinator.start()

    override fun stop() = coordinator.stop()
}
