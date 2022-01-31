package net.corda.chunking.read

import net.corda.libs.configuration.SmartConfig
import net.corda.lifecycle.Lifecycle
import javax.persistence.EntityManagerFactory

/**
 * Service that reads chunks from Kafka and puts them into the database.
 */
interface ChunkReadService : Lifecycle {
    /**
     *  Start processing incoming chunks.  The "entry-point" into this component.
     *  Configures its access to the database via the [EntityManagerFactory]
     */
    fun startProcessing(config: SmartConfig, instanceId: Int, entityManagerFactory: EntityManagerFactory)
}
