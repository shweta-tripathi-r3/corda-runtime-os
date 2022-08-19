package net.corda.applications.workers.workercommon.internal

import io.javalin.Javalin
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import net.corda.applications.workers.workercommon.MetricsServer
import net.corda.utilities.classload.OsgiClassLoader
import net.corda.utilities.classload.executeWithThreadContextClassLoader
import net.corda.utilities.executeWithStdErrSuppressed
import net.corda.v5.base.util.contextLogger
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.osgi.framework.FrameworkUtil
import org.osgi.service.component.annotations.Component


@Component(service = [MetricsServer::class])
class PrometheusMetricsServer : MetricsServer {
    private val prometheusRegistry: PrometheusMeterRegistry

    private companion object {
        private val logger = contextLogger()
    }

    init {
        logger.info("Creating metric registry")
        val metricsRegistry = Metrics.globalRegistry
        prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        metricsRegistry.add(prometheusRegistry)

        ClassLoaderMetrics().bindTo(metricsRegistry)
        JvmMemoryMetrics().bindTo(metricsRegistry)
        JvmGcMetrics().bindTo(metricsRegistry)
        ProcessorMetrics().bindTo(metricsRegistry)
        JvmThreadMetrics().bindTo(metricsRegistry)
        UptimeMetrics().bindTo(metricsRegistry)
    }
    private var server: Javalin? = null

    override fun listen(port: Int) {
        logger.info("Starting metrics server on port $port")
        server = Javalin
            .create()
            .apply { startServer(this, port) }
            .get(HTTP_METRICS_ROUTE) { context ->
                context.result(prometheusRegistry.scrape())
            }
    }

    override val port get() = server?.port()

    override fun stop() {
        server?.stop()
    }

    /** Starts a Javalin server on [port]. */
    private fun startServer(server: Javalin, port: Int) {
        val bundle = FrameworkUtil.getBundle(WebSocketServletFactory::class.java)

        if (bundle == null) {
            server.start(port)
        } else {
            // We temporarily switch the context class loader to allow Javalin to find `WebSocketServletFactory`.
            executeWithThreadContextClassLoader(OsgiClassLoader(listOf(bundle))) {
                // Required because Javalin prints an error directly to stderr if it cannot find a logging
                // implementation via standard class loading mechanism. This mechanism is not appropriate for OSGi.
                // The logging implementation is found correctly in practice.
                executeWithStdErrSuppressed {
                    server.start(port)
                }
            }
        }
    }
}