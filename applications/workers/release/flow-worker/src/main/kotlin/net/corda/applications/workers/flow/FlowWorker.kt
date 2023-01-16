package net.corda.applications.workers.flow

import net.corda.applications.workers.workercommon.ApplicationBanner
import net.corda.applications.workers.workercommon.DefaultWorkerParams
import net.corda.applications.workers.workercommon.JavaSerialisationFilter
import net.corda.applications.workers.workercommon.WorkerHelpers.Companion.getBootstrapConfig
import net.corda.applications.workers.workercommon.WorkerHelpers.Companion.getParams
import net.corda.applications.workers.workercommon.WorkerHelpers.Companion.loggerStartupInfo
import net.corda.applications.workers.workercommon.WorkerHelpers.Companion.printHelpOrVersion
import net.corda.applications.workers.workercommon.WorkerHelpers.Companion.setupMonitor
import net.corda.applications.workers.workercommon.WorkerMonitor
import net.corda.data.cluster.WorkerVersion
import net.corda.libs.configuration.SmartConfigFactoryFactory
import net.corda.libs.configuration.validation.ConfigurationValidatorFactory
import net.corda.libs.platform.PlatformInfoProvider
import net.corda.messaging.api.publisher.config.PublisherConfig
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.records.Record
import net.corda.osgi.api.Application
import net.corda.osgi.api.Shutdown
import net.corda.processors.flow.FlowProcessor
import net.corda.schema.Schemas.Cluster.Companion.CLUSTER_WORKER_VERSION_TOPIC
import net.corda.v5.base.util.contextLogger
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import picocli.CommandLine.Mixin
import kotlin.concurrent.thread

/** The worker for handling flows. */
@Suppress("Unused", "LongParameterList")
@Component(service = [Application::class])
class FlowWorker @Activate constructor(
    @Reference(service = FlowProcessor::class)
    private val flowProcessor: FlowProcessor,
    @Reference(service = Shutdown::class)
    private val shutDownService: Shutdown,
    @Reference(service = WorkerMonitor::class)
    private val workerMonitor: WorkerMonitor,
    @Reference(service = ConfigurationValidatorFactory::class)
    private val configurationValidatorFactory: ConfigurationValidatorFactory,
    @Reference(service = PlatformInfoProvider::class)
    val platformInfoProvider: PlatformInfoProvider,
    @Reference(service = ApplicationBanner::class)
    val applicationBanner: ApplicationBanner,
    @Reference(service = SmartConfigFactoryFactory::class)
    val smartConfigFactoryFactory: SmartConfigFactoryFactory,
    @Reference(service = PublisherFactory::class)
    val publisherFactory: PublisherFactory,
) : Application {

    private companion object {
        private val logger = contextLogger()
    }

    private var running = false

    /** Parses the arguments, then initialises and starts the [flowProcessor]. */
    override fun startup(args: Array<String>) {
        logger.info("Flow worker starting.")
        logger.loggerStartupInfo(platformInfoProvider)

        applicationBanner.show("Flow Worker", platformInfoProvider)

        if (System.getProperty("co.paralleluniverse.fibers.verifyInstrumentation") == true.toString()) {
            logger.info("Quasar's instrumentation verification is enabled")
        }

        JavaSerialisationFilter.install()

        val params = getParams(args, FlowWorkerParams())
        if (printHelpOrVersion(params.defaultParams, FlowWorker::class.java, shutDownService)) return
        setupMonitor(workerMonitor, params.defaultParams, this.javaClass.simpleName)

        val config = getBootstrapConfig(
            smartConfigFactoryFactory,
            params.defaultParams,
            configurationValidatorFactory.createConfigValidator())

        running = true
        val publisher = publisherFactory.createPublisher(
            PublisherConfig("", false),
            config
        )

        thread {
            while (running) {
                publisher.publish(
                    listOf(
                        Record(
                            CLUSTER_WORKER_VERSION_TOPIC,
                            this::class.java.simpleName,
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

        flowProcessor.start(config)
    }

    override fun shutdown() {
        logger.info("Flow worker stopping.")
        running = false
        flowProcessor.stop()
        workerMonitor.stop()
    }
}

/** Additional parameters for the flow worker are added here. */
private class FlowWorkerParams {
    @Mixin
    var defaultParams = DefaultWorkerParams()
}