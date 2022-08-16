package net.corda.components.test.impl

import net.corda.components.test.TestService
import net.corda.configuration.read.ConfigChangedEvent
import net.corda.configuration.read.ConfigurationReadService
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleEvent
import net.corda.lifecycle.StartEvent
import net.corda.lifecycle.createCoordinator
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.v5.base.util.contextLogger
import net.corda.v5.base.util.debug
import net.corda.data.*
import net.corda.libs.configuration.SmartConfig
import net.corda.libs.configuration.SmartConfigFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.lifecycle.StopEvent
import net.corda.messaging.api.subscription.config.RPCConfig
import net.corda.schema.configuration.BootConfig
import net.corda.schema.configuration.ConfigKeys
import net.corda.v5.base.concurrent.getOrThrow
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Component(service = [TestService::class])
class TestServiceImpl @Activate constructor(
    @Reference(service = LifecycleCoordinatorFactory::class)
    private val coordinatorFactory: LifecycleCoordinatorFactory,
    @Reference(service = ConfigurationReadService::class)
    private val configurationReadService: ConfigurationReadService,
    @Reference(service = PublisherFactory::class)
    private val publisherFactory: PublisherFactory
//    @Reference(service = JavalinServerFactory::class)
//    private val javalinFactory: JavalinServerFactory

) : TestService {
    private var executorService: ExecutorService = Executors.newCachedThreadPool()
    private var currentConfig: Map<String, SmartConfig> = emptyMap()

    companion object {
        private val logger = contextLogger()

        // TODO: Should also use constant
        private val requiredKeys = setOf(ConfigKeys.MESSAGING_CONFIG, "corda.test")
        private const val REGISTRATION = "REGISTRATION"
        private const val CONFIG_HANDLE = "CONFIG_HANDLE"
    }

    private val coordinator = coordinatorFactory.createCoordinator<TestService> { event: LifecycleEvent, coordinator: LifecycleCoordinator ->
        logger.debug { "FlowService received: $event" }
        logger.debug { "FlowService received: ${coordinator.status}" }
        when (event) {
            is StartEvent -> {
                configurationReadService.start()
                coordinator.createManagedResource(REGISTRATION) {
                    coordinator.followStatusChangesByName(
                        setOf(
                            LifecycleCoordinatorName.forComponent<ConfigurationReadService>()
                        )
                    )
                }
                coordinator.updateStatus(LifecycleStatus.UP)
                logger.info("${this::javaClass.name} is now Up")
            }
            is RegistrationStatusChangeEvent -> {
                when (event.status) {
                    LifecycleStatus.ERROR -> {
                        coordinator.closeManagedResources(setOf(CONFIG_HANDLE))
                        coordinator.postEvent(StopEvent(errored = true))
                    }
                    LifecycleStatus.UP -> {
                        // Receive updates to the RPC and Messaging config
                        coordinator.createManagedResource(CONFIG_HANDLE) {
                            configurationReadService.registerComponentForUpdates(
                                coordinator,
                                requiredKeys
                            )
                        }
                    }
                    else -> logger.debug { "Unexpected status: ${event.status}" }
                }
                coordinator.updateStatus(event.status)
                logger.info("${this::javaClass.name} is now ${event.status}")
            }
            // TODO: Do something more clever here
            is ConfigChangedEvent -> currentConfig = event.config
        }
    }

    private fun onMessage(message: String, future: CompletableFuture<String>) {
        logger.info("Received message: $message")
        when (message) {
            "break" -> future.complete("bye")
            else -> {
                val (request, response, topic, data) = message.split('|')
                logger.debug { "request type: $request" }
                logger.debug { "response type: $response" }
                logger.debug { "topic: $topic" }
                logger.debug { "data: $data" }
//                val reqType = Class.forName(request)!!
                val reqType = String::class.java
                val responseType = Class.forName(response)!!
                // Open some connection
                val sender = publisherFactory.createRPCSender(
                    RPCConfig(
                        "test-worker",
                        "test-worker-${UUID.randomUUID()}",
                        topic,
                        reqType,
                        responseType
                    ),
                    currentConfig[ConfigKeys.MESSAGING_CONFIG]!!
                )

                val resolvedResponse = sender.sendRequest(data).getOrThrow(Duration.ofMillis(1000))

                // Get response
                // Reply
                future.complete(resolvedResponse.toString())
                sender.close()
            }
        }
    }

    override val isRunning: Boolean
        get() = coordinator.isRunning

    private fun startServer() {
        logger.info("Starting socket server")
        val socketServer = ServerSocket(4132)
        val clientSocket: Socket = socketServer.accept()
        val writer = PrintWriter(clientSocket.getOutputStream(), true)
        val reader = BufferedReader(
            InputStreamReader(clientSocket.getInputStream())
        )

        var inputLine = reader.readLine()
        while (isRunning && inputLine != null) {
            val future = CompletableFuture<String>()
            executorService.submit {
                onMessage(inputLine, future)
            }
            val resp = future.get()
            if (resp == "bye") {
                break
            } else {
                writer.println(resp)
                inputLine = reader.readLine()
            }
        }
    }

    override fun start() {
        logger.info("Trying to start")
        coordinator.start()

        logger.info("Hello")

        executorService.submit {
            startServer()
        }
    }

    override fun stop() {
        logger.info("Trying to stop")
        coordinator.stop()
        executorService.shutdown()
    }
}
