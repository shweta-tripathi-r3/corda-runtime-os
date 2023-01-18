package net.corda.libs.platform.impl

import net.corda.configuration.read.ConfigChangedEvent
import net.corda.configuration.read.ConfigurationReadService
import net.corda.data.cluster.WorkerVersion
import net.corda.libs.configuration.helper.getConfig
import net.corda.libs.platform.ClusterWorkerVersion
import net.corda.libs.platform.PlatformInfoProvider
import net.corda.lifecycle.DependentComponents
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.lifecycle.createCoordinator
import net.corda.messaging.api.processor.PubSubProcessor
import net.corda.messaging.api.records.Record
import net.corda.messaging.api.subscription.config.SubscriptionConfig
import net.corda.messaging.api.subscription.factory.SubscriptionFactory
import net.corda.schema.Schemas.Cluster.Companion.CLUSTER_WORKER_VERSION_TOPIC
import net.corda.schema.configuration.ConfigKeys.MESSAGING_CONFIG
import org.osgi.framework.BundleContext
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future


@Component(service = [PlatformInfoProvider::class], immediate = true)
class PlatformInfoProviderImpl @Activate constructor(
    @Reference(service = ConfigurationReadService::class)
    private val configurationReadService: ConfigurationReadService,
    @Reference(service = LifecycleCoordinatorFactory::class)
    private val coordinatorFactory: LifecycleCoordinatorFactory,
    @Reference(service = SubscriptionFactory::class)
    private val subscriptionFactory: SubscriptionFactory,
    bundleContext: BundleContext,
) : PlatformInfoProvider {

    internal companion object {
        const val STUB_PLATFORM_VERSION = 5000

        private const val DEFAULT_PLATFORM_VERSION = 5000
        private const val PLATFORM_VERSION_KEY = "net.corda.platform.version"
        private const val VERSION_SUBSCRIPTION = "VersionSubscription"
    }

    private val dependentComponents = DependentComponents.of(
        ::configurationReadService
    )
    private val lifecycleCoordinator = coordinatorFactory.createCoordinator<PlatformInfoProvider>(dependentComponents, ::eventHandler)

    private val _workerVersions = ConcurrentHashMap<String, ClusterWorkerVersion>()

    /** Temporary stub values **/
    override val activePlatformVersion = STUB_PLATFORM_VERSION

    override val localWorkerPlatformVersion = bundleContext.getProperty(PLATFORM_VERSION_KEY)?.toInt() ?: DEFAULT_PLATFORM_VERSION

    override val localWorkerSoftwareVersion = bundleContext.bundle.version.toString()

    override val workerVersions: Map<String, ClusterWorkerVersion>
        get() = _workerVersions

    override fun start() = lifecycleCoordinator.start()

    override fun stop() = lifecycleCoordinator.stop()

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
                lifecycleCoordinator.createManagedResource(VERSION_SUBSCRIPTION) {
                    subscriptionFactory.createPubSubSubscription(
                        SubscriptionConfig(VERSION_SUBSCRIPTION, CLUSTER_WORKER_VERSION_TOPIC),
                        subscriptionHandler,
                        event.config.getConfig(MESSAGING_CONFIG)
                    ).also {
                        it.start()
                    }
                }
            }
        }
    }

    private val subscriptionHandler = object : PubSubProcessor<String, WorkerVersion> {
        override fun onNext(event: Record<String, WorkerVersion>): Future<Unit> {
            val version = event.value!!
            _workerVersions[event.key] = ClusterWorkerVersion(version.workerPlatformVersion, version.workerSoftwareVersion)
            return CompletableFuture.completedFuture(Unit)
        }

        override val keyClass = String::class.java
        override val valueClass = WorkerVersion::class.java
    }
}