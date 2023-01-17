package net.corda.applications.workers.workercommon.internal

import net.corda.applications.workers.workercommon.VersionPublisher
import net.corda.configuration.read.ConfigChangedEvent
import net.corda.configuration.read.ConfigurationReadService
import net.corda.data.cluster.WorkerVersion
import net.corda.libs.configuration.helper.getConfig
import net.corda.libs.platform.PlatformInfoProvider
import net.corda.lifecycle.DependentComponents
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.lifecycle.StopEvent
import net.corda.lifecycle.createCoordinator
import net.corda.messaging.api.publisher.Publisher
import net.corda.messaging.api.publisher.config.PublisherConfig
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.records.Record
import net.corda.schema.Schemas.Cluster.Companion.CLUSTER_WORKER_VERSION_TOPIC
import net.corda.schema.configuration.ConfigKeys.MESSAGING_CONFIG
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import kotlin.concurrent.thread

@Component(service = [VersionPublisher::class], immediate = true)
class VersionPublisherImpl @Activate constructor(
    @Reference(service = ConfigurationReadService::class)
    private val configurationReadService: ConfigurationReadService,
    @Reference(service = LifecycleCoordinatorFactory::class)
    private val coordinatorFactory: LifecycleCoordinatorFactory,
    @Reference(service = PlatformInfoProvider::class)
    val platformInfoProvider: PlatformInfoProvider,
    @Reference(service = PublisherFactory::class)
    val publisherFactory: PublisherFactory,
) : VersionPublisher {

    private var publisher: Publisher? = null
    private var running = true
    private var serviceName = ""

    private val dependentComponents = DependentComponents.of(
        ::configurationReadService,
    )

    private val lifecycleCoordinator =
        coordinatorFactory.createCoordinator<VersionPublisher>(dependentComponents, ::eventHandler)

    @Suppress("UNUSED_PARAMETER")
    private fun eventHandler(event: LifecycleEvent, coordinator: LifecycleCoordinator) {
        when (event) {
            is RegistrationStatusChangeEvent -> {
                configurationReadService.registerComponentForUpdates(
                    lifecycleCoordinator,
                    setOf(MESSAGING_CONFIG)
                )
            }

            is ConfigChangedEvent -> {
                publisher = publisherFactory.createPublisher(
                    PublisherConfig("", false),
                    event.config.getConfig(MESSAGING_CONFIG)
                )
                doStuff()
            }

            is StopEvent -> {
                running = false
            }
        }
    }

    private fun doStuff() {
        thread {
            while (running && serviceName.isNotEmpty()) {
                publisher?.publish(
                    listOf(
                        Record(
                            CLUSTER_WORKER_VERSION_TOPIC,
                            serviceName,
                            WorkerVersion(
                                platformInfoProvider.localWorkerPlatformVersion,
                                platformInfoProvider.activePlatformVersion,
                                platformInfoProvider.localWorkerSoftwareVersion
                            )
                        )
                    )
                )
                Thread.sleep(1000)
            }
        }
    }

    override fun start(name: String) {
        serviceName = name
        lifecycleCoordinator.start()
    }

    override val isRunning: Boolean
        get() = running

    override fun start() = lifecycleCoordinator.start()

    override fun stop() = lifecycleCoordinator.stop()

}